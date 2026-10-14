package com.asnidev.hyperbit.icons

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The high-definition icon looks, on plain ARGB arrays (not premultiplied,
 * row-major, [n]×[n]) so they run in unit tests. Alpha stays soft: these are
 * smooth, glowing pictures, not one-bit ones.
 */
object NeonPixels {

    fun alpha(c: Int): Float = ((c ushr 24) and 0xFF) / 255f
    fun red(c: Int): Int = (c shr 16) and 0xFF
    fun green(c: Int): Int = (c shr 8) and 0xFF
    fun blue(c: Int): Int = c and 0xFF

    fun luminance(c: Int): Float = (0.299f * red(c) + 0.587f * green(c) + 0.114f * blue(c)) / 255f

    fun argb(a: Float, r: Int, g: Int, b: Int): Int =
        ((a.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** [c]'s colour with alpha [a]. */
    fun withAlpha(c: Int, a: Float): Int = argb(a, red(c), green(c), blue(c))

    fun mix(a: Int, b: Int, t: Float): Int = argb(
        1f,
        (red(a) + (red(b) - red(a)) * t).roundToInt(),
        (green(a) + (green(b) - green(a)) * t).roundToInt(),
        (blue(a) + (blue(b) - blue(a)) * t).roundToInt(),
    )

    /** Brightness of the visible pixels stretched over their own range (5th..95th percentile). */
    private fun levels(px: IntArray): FloatArray {
        val seen = px.filter { alpha(it) > 0.1f }.map(::luminance).sorted()
        val lo = if (seen.isEmpty()) 0f else seen[(seen.size * 0.05f).toInt().coerceAtMost(seen.lastIndex)]
        val hi = if (seen.isEmpty()) 1f else seen[(seen.size * 0.95f).toInt().coerceAtMost(seen.lastIndex)]
        val range = (hi - lo).takeIf { it > 0.15f }
        return FloatArray(px.size) { i ->
            val l = luminance(px[i])
            if (range == null) l else ((l - lo) / range).coerceIn(0f, 1f)
        }
    }

    /**
     * Shadows in a deep [dark], mid tones in [dark] itself, lights in [light].
     * The way from one neon to the other turns around the colour wheel, so mid
     * tones stay saturated (pink to cyan through violet) rather than going gray.
     */
    fun duotone(px: IntArray, dark: Int, light: Int): IntArray {
        val t = levels(px)
        val deep = mix(0xFF000000.toInt(), dark, 0.45f)
        return IntArray(px.size) { i ->
            val a = alpha(px[i])
            if (a == 0f) 0 else {
                val c = if (t[i] < 0.4f) mix(deep, dark, t[i] / 0.4f) else mixHue(dark, light, (t[i] - 0.4f) / 0.6f)
                withAlpha(c, a)
            }
        }
    }

    /** Blend through hue, saturation and value: neons stay vivid on the way. */
    fun mixHue(a: Int, b: Int, t: Float): Int {
        val ha = hsv(a)
        val hb = hsv(b)
        var dh = hb[0] - ha[0]
        if (dh > 180f) dh -= 360f
        if (dh < -180f) dh += 360f
        val h = (ha[0] + dh * t + 360f) % 360f
        return fromHsv(h, ha[1] + (hb[1] - ha[1]) * t, ha[2] + (hb[2] - ha[2]) * t)
    }

    fun hsv(c: Int): FloatArray {
        val r = red(c) / 255f; val g = green(c) / 255f; val b = blue(c) / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return floatArrayOf((h + 360f) % 360f, if (max == 0f) 0f else d / max, max)
    }

    fun fromHsv(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt() % 6) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        return argb(1f, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /**
     * Without the faint layers some icons carry (a long shadow at a fifth
     * opacity): in neon they would turn into dark squares or frame lines.
     */
    fun dropFaint(px: IntArray, below: Float = 0.35f): IntArray = IntArray(px.size) { i ->
        if (alpha(px[i]) < below) 0 else px[i]
    }

    /**
     * A projection in [tint]: brightness becomes opacity, every third row is
     * dimmed like scan lines, and an [ghost]-coloured copy sits a few pixels to
     * the left, added on top (the colour fringe of a misaligned projector).
     */
    fun hologram(px: IntArray, n: Int, tint: Int, ghost: Int): IntArray {
        val t = levels(px)
        val base = IntArray(px.size) { i ->
            val a = alpha(px[i]) * (0.3f + 0.7f * t[i]) * if ((i / n) % 3 == 2) 0.4f else 1f
            withAlpha(tint, a)
        }
        val shift = (n / 48).coerceAtLeast(1)
        val fringe = IntArray(px.size) { i ->
            val x = i % n
            val src = if (x + shift < n) base[i + shift] else 0
            withAlpha(ghost, alpha(src) * 0.55f)
        }
        return add(fringe, base)
    }

    /** Glowing lines where the picture has edges (Sobel), over a faint fill of the shape. */
    fun wireframe(px: IntArray, n: Int, line: Int): IntArray {
        // Edges of brightness and of the outline alike: brightness times alpha does both.
        val v = FloatArray(px.size) { luminance(px[it]) * 0.6f + alpha(px[it]) * 0.4f }
        fun at(x: Int, y: Int) = v[y.coerceIn(0, n - 1) * n + x.coerceIn(0, n - 1)]
        return IntArray(px.size) { i ->
            val x = i % n
            val y = i / n
            val gx = at(x + 1, y - 1) + 2 * at(x + 1, y) + at(x + 1, y + 1) - at(x - 1, y - 1) - 2 * at(x - 1, y) - at(x - 1, y + 1)
            val gy = at(x - 1, y + 1) + 2 * at(x, y + 1) + at(x + 1, y + 1) - at(x - 1, y - 1) - 2 * at(x, y - 1) - at(x + 1, y - 1)
            val edge = (sqrt(gx * gx + gy * gy) * 1.6f).coerceIn(0f, 1f)
            val fill = alpha(px[i]) * 0.12f
            withAlpha(line, maxOf(edge, fill))
        }
    }

    /** The shape alone in [color], lit toward white at the top like a lamp seen from above. */
    fun glyph(px: IntArray, n: Int, color: Int): IntArray {
        val top = mix(color, 0xFFFFFFFF.toInt(), 0.45f)
        return IntArray(px.size) { i ->
            val a = alpha(px[i])
            if (a == 0f) 0 else withAlpha(mix(top, color, ((i / n) / (n - 1f) * 1.6f).coerceAtMost(1f)), a)
        }
    }

    /**
     * A soft halo in [color] around whatever is opaque in [px] (three box blurs
     * of radius [radius] ≈ a Gaussian), at [strength] opacity, under the picture.
     */
    fun glow(px: IntArray, n: Int, color: Int, radius: Int, strength: Float): IntArray {
        if (radius <= 0 || strength <= 0f) return px
        var mask = FloatArray(px.size) { alpha(px[it]) }
        repeat(3) { mask = blur(mask, n, radius) }
        val halo = IntArray(px.size) { withAlpha(color, (mask[it] * strength).coerceAtMost(1f)) }
        return over(px, halo)
    }

    /** Separable box blur of radius [r] on an [n]×[n] map. */
    fun blur(v: FloatArray, n: Int, r: Int): FloatArray {
        val tmp = FloatArray(v.size)
        val out = FloatArray(v.size)
        val w = 2 * r + 1
        for (y in 0 until n) {
            var sum = 0f
            for (x in -r..r) sum += v[y * n + x.coerceIn(0, n - 1)]
            for (x in 0 until n) {
                tmp[y * n + x] = sum / w
                sum += v[y * n + (x + r + 1).coerceAtMost(n - 1)] - v[y * n + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until n) {
            var sum = 0f
            for (y in -r..r) sum += tmp[y.coerceIn(0, n - 1) * n + x]
            for (y in 0 until n) {
                out[y * n + x] = sum / w
                sum += tmp[(y + r + 1).coerceAtMost(n - 1) * n + x] - tmp[(y - r).coerceAtLeast(0) * n + x]
            }
        }
        return out
    }

    /** [top] drawn over [bottom] (normal alpha blending). */
    fun over(top: IntArray, bottom: IntArray): IntArray = IntArray(top.size) { i ->
        val t = top[i]
        val b = bottom[i]
        val ta = alpha(t)
        val ba = alpha(b)
        val a = ta + ba * (1 - ta)
        if (a <= 0f) 0 else argb(
            a,
            ((red(t) * ta + red(b) * ba * (1 - ta)) / a).roundToInt(),
            ((green(t) * ta + green(b) * ba * (1 - ta)) / a).roundToInt(),
            ((blue(t) * ta + blue(b) * ba * (1 - ta)) / a).roundToInt(),
        )
    }

    /** Light added to light: colours sum, as overlapping beams do. */
    fun add(a: IntArray, b: IntArray): IntArray = IntArray(a.size) { i ->
        val x = a[i]
        val y = b[i]
        val xa = alpha(x)
        val ya = alpha(y)
        val alpha = (xa + ya).coerceAtMost(1f)
        if (alpha <= 0f) 0 else argb(
            alpha,
            ((red(x) * xa + red(y) * ya) / alpha).roundToInt(),
            ((green(x) * xa + green(y) * ya) / alpha).roundToInt(),
            ((blue(x) * xa + blue(y) * ya) / alpha).roundToInt(),
        )
    }

    /** Toward white by [amount]: how a pressed icon lights up. */
    fun brighten(px: IntArray, amount: Float): IntArray = IntArray(px.size) { i ->
        val c = px[i]
        if (alpha(c) == 0f) 0 else withAlpha(mix(c, 0xFFFFFFFF.toInt(), amount), alpha(c))
    }
}
