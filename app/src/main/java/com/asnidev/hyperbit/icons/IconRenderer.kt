package com.asnidev.hyperbit.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.asnidev.hyperbit.data.IconStyle

/** Everything that changes how one icon looks. */
data class IconSpec(
    val style: IconStyle,
    /** Pixels per side for the PIXEL style; the others are always [IconRenderer.SIDE]. */
    val res: Int,
    /** Neon: lights, lines, glow. */
    val ink: Int,
    /** Accent: darks and mid tones of the duotone, the hologram's fringe. */
    val accent: Int,
    /** Halo strength 0..3 (0: none). */
    val glow: Int,
    val stripBackground: Boolean,
    /** Lit up, as while pressed. */
    val selected: Boolean = false,
)

/** Turns app icons into neon ones and keeps the results in memory. */
class IconRenderer {

    private val cache = LruCache<String, ImageBitmap>(400)

    fun cached(id: String, spec: IconSpec): ImageBitmap? = cache.get("$id|$spec")

    /** Call off the main thread. */
    fun render(id: String, drawable: Drawable, spec: IconSpec): ImageBitmap {
        cache.get("$id|$spec")?.let { return it }
        val px = convert(drawable, spec)
        val bitmap = Bitmap.createBitmap(px, SIDE, SIDE, Bitmap.Config.ARGB_8888).asImageBitmap()
        cache.put("$id|$spec", bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()

    /**
     * The icon at [SIDE] pixels before any style: the right layer, drawn in the
     * middle [INNER] pixels so the halo has room around it.
     */
    fun prepare(drawable: Drawable, spec: IconSpec): IntArray {
        val layer = when {
            spec.style == IconStyle.GLYPH -> LayerChoice.MONOCHROME
            spec.style == IconStyle.ORIGINAL || !spec.stripBackground -> LayerChoice.WHOLE
            else -> LayerChoice.FOREGROUND
        }
        val content = if (spec.style == IconStyle.PIXEL) pixelated(drawable, layer, spec.res) else raster(drawable, INNER, layer)
        val px = IntArray(SIDE * SIDE)
        val off = (SIDE - INNER) / 2
        for (y in 0 until INNER) content.copyInto(px, (y + off) * SIDE + off, y * INNER, (y + 1) * INNER)
        return px
    }

    private fun convert(drawable: Drawable, spec: IconSpec): IntArray {
        val n = SIDE
        val prepared = prepare(drawable, spec)
        val base = if (spec.style == IconStyle.ORIGINAL || spec.style == IconStyle.PIXEL) prepared else NeonPixels.dropFaint(prepared)
        var px = when (spec.style) {
            IconStyle.ORIGINAL, IconStyle.PIXEL -> base
            IconStyle.NEON -> NeonPixels.duotone(base, spec.accent, spec.ink)
            IconStyle.HOLOGRAM -> NeonPixels.hologram(base, n, spec.ink, spec.accent)
            IconStyle.WIREFRAME -> NeonPixels.wireframe(base, n, spec.ink)
            IconStyle.GLYPH -> NeonPixels.glyph(base, n, spec.ink)
        }
        if (spec.selected) px = NeonPixels.brighten(px, 0.35f)
        val glow = if (spec.selected) (spec.glow + 1).coerceAtMost(3) else spec.glow
        if (glow > 0 && spec.style != IconStyle.ORIGINAL) {
            px = NeonPixels.glow(px, n, spec.ink, radius = n / 28, strength = 0.35f * glow)
        }
        return px
    }

    /** The icon at [res] pixels (supersampled), blown up to [INNER] without smoothing. */
    private fun pixelated(drawable: Drawable, layer: LayerChoice, res: Int): IntArray {
        val small = RetroPixels.hardAlpha(RetroPixels.downsample(raster(drawable, res * 4, layer), res * 4, 4))
        return IntArray(INNER * INNER) { i ->
            val sx = (i % INNER) * res / INNER
            val sy = (i / INNER) * res / INNER
            small[sy * res + sx]
        }
    }

    private enum class LayerChoice { WHOLE, FOREGROUND, MONOCHROME }

    /** The icon drawn into a [side]×[side] ARGB array. */
    private fun raster(drawable: Drawable, side: Int, choice: LayerChoice): IntArray {
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = drawable as? AdaptiveIconDrawable
        val layer: Drawable? = when {
            adaptive == null -> null
            choice == LayerChoice.MONOCHROME && Build.VERSION.SDK_INT >= 33 -> adaptive.monochrome ?: adaptive.foreground
            choice == LayerChoice.MONOCHROME || choice == LayerChoice.FOREGROUND -> adaptive.foreground
            else -> null
        }
        if (layer != null) {
            // A layer is 108 units wide and only the middle 72 show through the mask,
            // so it's drawn half as large again to fill the square with that middle.
            val pad = side / 4
            val old = layer.copyBounds()
            layer.setBounds(-pad, -pad, side + pad, side + pad)
            layer.draw(canvas)
            layer.bounds = old
        } else {
            val old = drawable.copyBounds()
            drawable.setBounds(0, 0, side, side)
            drawable.draw(canvas)
            drawable.bounds = old
        }
        val px = IntArray(side * side)
        bitmap.getPixels(px, 0, side, 0, 0, side, side)
        bitmap.recycle()
        return px
    }

    companion object {
        /** Every icon bitmap is this many pixels square, halo included. */
        const val SIDE = 192
        /** The picture itself; the rest is room for the halo. */
        const val INNER = 144
    }
}
