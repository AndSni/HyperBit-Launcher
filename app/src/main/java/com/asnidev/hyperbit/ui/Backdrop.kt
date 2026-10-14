package com.asnidev.hyperbit.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.asnidev.hyperbit.data.Backdrop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Seconds since the backdrop started, ticking about 30 times a second while [animate]. */
@Composable
private fun rememberClock(animate: Boolean): Float {
    val time by produceState(0f, animate) {
        if (!animate) return@produceState
        val start = withFrameNanos { it }
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                // 30 frames a second is smooth enough for a background and halves the work.
                if (now - last >= 32_000_000L) {
                    last = now
                    value = (now - start) / 1e9f
                }
            }
        }
    }
    return time
}

/** The desktop behind the windows, in the theme's accent over its base colour. */
@Composable
fun BackdropView(backdrop: Backdrop, animate: Boolean, modifier: Modifier = Modifier) {
    val r = LocalRetro.current
    val t = rememberClock(animate)
    // Its own layer: redrawing every frame mustn't redraw the windows over it.
    Canvas(modifier.graphicsLayer()) {
        when (backdrop) {
            Backdrop.GRID -> grid(r, t)
            Backdrop.HEX -> hex(r, t)
            Backdrop.CIRCUIT -> circuit(r, t)
            Backdrop.RAIN -> rain(r, t)
            Backdrop.DOTS -> dots(r, t)
            Backdrop.PLAIN -> plain(r, t)
        }
    }
}

private fun DrawScope.glowLine(c: Color, a: Offset, b: Offset, w: Float) {
    drawLine(c.copy(alpha = c.alpha * 0.25f), a, b, w * 4, StrokeCap.Round)
    drawLine(c, a, b, w, StrokeCap.Round)
}

/** Synthwave: a sky fading into the horizon, a sliced sun, a floor grid rushing closer. */
private fun DrawScope.grid(r: Retro, t: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.46f
    val skyLow = lerp(r.desk, r.deskInk, 0.45f)
    val sky = Brush.verticalGradient(listOf(r.desk, r.desk, skyLow), 0f, horizon)
    drawRect(sky, size = Size(w, horizon))

    // The sun: neon to accent, cut by bands of sky that thicken toward the horizon.
    val sunR = w * 0.24f
    val sunC = Offset(w / 2, horizon - sunR * 0.15f)
    clipRect(bottom = horizon) {
        drawCircle(Brush.verticalGradient(listOf(r.ink, r.deskInk), sunC.y - sunR, sunC.y + sunR * 0.4f), sunR, sunC)
        drawCircle(r.ink.copy(alpha = 0.10f), sunR * 1.25f, sunC)
        var y = sunC.y
        var band = 2f * density
        while (y < horizon) {
            drawRect(sky, Offset(0f, y), Size(w, band))
            y += band * 3f
            band *= 1.35f
        }
    }

    // Floor.
    drawRect(Brush.verticalGradient(listOf(lerp(r.desk, r.deskInk, 0.12f), r.desk), horizon, h), Offset(0f, horizon), Size(w, h - horizon))
    val line = r.deskInk
    val lw = 1.2f * density
    // Lines toward the vanishing point.
    val vp = Offset(w / 2, horizon)
    for (i in -14..14) {
        val x = w / 2 + i * w * 0.16f
        glowLine(line.copy(alpha = 0.55f), vp, Offset(x + (x - w / 2) * 1.5f, h * 1.3f), lw)
    }
    // Cross lines, closer together near the horizon, moving toward the viewer.
    val phase = (t * 0.45f) % 1f
    for (k in 0 until 16) {
        val d = (k + phase) / 16f
        val y = horizon + (h - horizon) * d.pow(2.3f)
        glowLine(line.copy(alpha = (0.15f + d).coerceAtMost(0.9f)), Offset(0f, y), Offset(w, y), lw)
    }
    // The horizon itself.
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, line.copy(alpha = 0.5f), Color.Transparent), horizon - 18 * density, horizon + 18 * density), Offset(0f, horizon - 18 * density), Size(w, 36 * density))
    drawLine(line, Offset(0f, horizon), Offset(w, horizon), 1.5f * density)
}

/** A honeycomb in faint accent, lit where a slow pulse wanders across it. */
private fun DrawScope.hex(r: Retro, t: Float) {
    drawRect(Brush.verticalGradient(listOf(r.desk, lerp(r.desk, r.deskInk, 0.10f))))
    val s = 22 * density
    val dx = s * 1.5f
    val dy = s * sqrt(3f)
    val pulse = Offset(size.width * (0.5f + 0.35f * sin(t * 0.23f)), size.height * (0.5f + 0.38f * cos(t * 0.17f)))
    val reach = size.minDimension * 0.35f
    var col = 0
    var x = 0f
    while (x < size.width + s) {
        var y = if (col % 2 == 0) 0f else dy / 2
        while (y < size.height + s) {
            val d = (Offset(x, y) - pulse).getDistance()
            val lit = exp(-(d * d) / (reach * reach))
            val p = Path()
            for (k in 0..5) {
                val a = PI.toFloat() / 3 * k
                val px = x + s * 0.94f * cos(a)
                val py = y + s * 0.94f * sin(a)
                if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            if (lit > 0.05f) drawPath(p, r.deskInk.copy(alpha = 0.10f * lit))
            drawPath(p, r.deskInk.copy(alpha = 0.10f + 0.6f * lit), style = Stroke(1f * density))
            y += dy
        }
        x += dx
        col++
    }
}

/** Fixed traces of a circuit board with pulses of light running along them. */
private class Trace(val points: List<Offset>, val lengths: FloatArray, val speed: Float, val offset: Float) {
    val total: Float get() = lengths.last()

    fun at(dist: Float): Offset {
        val d = ((dist % total) + total) % total
        var i = 1
        while (i < lengths.size - 1 && lengths[i] < d) i++
        val seg = lengths[i] - lengths[i - 1]
        val f = if (seg == 0f) 0f else (d - lengths[i - 1]) / seg
        return points[i - 1] + (points[i] - points[i - 1]) * f
    }
}

private val traceCache = HashMap<Size, List<Trace>>()

private fun traces(size: Size, cell: Float): List<Trace> = traceCache.getOrPut(size) {
    val rnd = Random(1984)
    val dirs = listOf(Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f), Offset(-1f, 1f), Offset(-1f, 0f), Offset(-1f, -1f), Offset(0f, -1f), Offset(1f, -1f))
    List(34) {
        var p = Offset((rnd.nextInt((size.width / cell).toInt().coerceAtLeast(1))) * cell, (rnd.nextInt((size.height / cell).toInt().coerceAtLeast(1))) * cell)
        var dir = rnd.nextInt(8)
        val pts = mutableListOf(p)
        repeat(4 + rnd.nextInt(6)) {
            // Turn by at most 45 degrees, as board traces do.
            dir = (dir + rnd.nextInt(3) - 1 + 8) % 8
            p += dirs[dir] * (cell * (1 + rnd.nextInt(4)))
            pts += p
        }
        val lens = FloatArray(pts.size)
        for (i in 1 until pts.size) lens[i] = lens[i - 1] + (pts[i] - pts[i - 1]).getDistance()
        Trace(pts, lens, cell * (1.5f + rnd.nextFloat() * 3f), rnd.nextFloat() * 1000f)
    }
}

private fun DrawScope.circuit(r: Retro, t: Float) {
    drawRect(Brush.radialGradient(listOf(lerp(r.desk, r.deskInk, 0.10f), r.desk), Offset(size.width / 2, size.height * 0.6f), size.maxDimension * 0.7f))
    val list = traces(size, 26 * density)
    for (tr in list) {
        val p = Path().apply { moveTo(tr.points[0].x, tr.points[0].y); tr.points.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(p, r.deskInk.copy(alpha = 0.22f), style = Stroke(1.5f * density))
        drawCircle(r.deskInk.copy(alpha = 0.5f), 3f * density, tr.points.first(), style = Stroke(1.2f * density))
        drawCircle(r.deskInk.copy(alpha = 0.5f), 2.5f * density, tr.points.last())
        val head = tr.at(t * tr.speed + tr.offset)
        drawCircle(r.ink.copy(alpha = 0.18f), 9f * density, head)
        drawCircle(r.ink, 2.2f * density, head)
    }
}

private class Drop(val x: Float, val speed: Float, val offset: Float, val length: Int)

private val dropCache = HashMap<Size, List<Drop>>()

/** Columns of light falling at their own pace, each with a bright head. */
private fun DrawScope.rain(r: Retro, t: Float) {
    drawRect(Brush.verticalGradient(listOf(r.desk, lerp(r.desk, r.deskInk, 0.08f))))
    val col = 16 * density
    val drops = dropCache.getOrPut(size) {
        val rnd = Random(2077)
        List((size.width / col).toInt() + 1) { i ->
            Drop(i * col + col / 2, size.height * (0.15f + rnd.nextFloat() * 0.35f), rnd.nextFloat() * 10f, 8 + rnd.nextInt(14))
        }
    }
    val dash = 9 * density
    val gap = 4 * density
    for (d in drops) {
        val span = size.height + d.length * (dash + gap)
        val head = ((t + d.offset) * d.speed) % span
        for (k in 0 until d.length) {
            val y = head - k * (dash + gap)
            if (y < -dash || y > size.height) continue
            val a = (1f - k / d.length.toFloat())
            val c = if (k == 0) r.ink else r.deskInk.copy(alpha = 0.55f * a)
            drawLine(c, Offset(d.x, y - dash), Offset(d.x, y), 2f * density, StrokeCap.Round)
        }
    }
}

/** A dot matrix with ripples spreading from below. */
private fun DrawScope.dots(r: Retro, t: Float) {
    drawRect(r.desk)
    val step = 16 * density
    val origin = Offset(size.width / 2, size.height * 1.05f)
    val buckets = Array(6) { ArrayList<Offset>() }
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            val d = (Offset(x, y) - origin).getDistance() / (80 * density)
            val wave = max(0f, sin(d * 2f - t * 2.2f)).pow(3)
            buckets[(wave * 5.99f).toInt()].add(Offset(x, y))
            x += step
        }
        y += step
    }
    for ((i, points) in buckets.withIndex()) {
        val c = if (i == 5) r.ink.copy(alpha = 0.9f) else r.deskInk.copy(alpha = 0.18f + 0.14f * i)
        drawPoints(points, PointMode.Points, c, strokeWidth = (2.2f + i * 0.5f) * density, cap = StrokeCap.Round)
    }
}

/** Two soft glows breathing slowly on the base colour. */
private fun DrawScope.plain(r: Retro, t: Float) {
    drawRect(r.desk)
    val breathe = 1f + 0.06f * sin(t * 0.6f)
    drawRect(Brush.radialGradient(listOf(r.deskInk.copy(alpha = 0.35f), Color.Transparent), Offset(size.width * 0.5f, size.height * 1.0f), size.maxDimension * 0.6f * breathe))
    drawRect(Brush.radialGradient(listOf(r.ink.copy(alpha = 0.14f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.1f), size.maxDimension * 0.5f / breathe))
}

/** Scan lines and a vignette over everything (one tiled shader, one gradient). */
@Composable
fun Modifier.screenEffects(on: Boolean): Modifier {
    if (!on) return this
    val brush = remember {
        val px = intArrayOf(0, 0, 0x2E000000)
        ShaderBrush(ImageShader(Bitmap.createBitmap(px, 1, 3, Bitmap.Config.ARGB_8888).asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    return drawWithContent {
        drawContent()
        drawRect(brush)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), center, size.maxDimension * 0.75f))
    }
}
