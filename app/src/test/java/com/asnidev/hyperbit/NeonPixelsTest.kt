package com.asnidev.hyperbit

import com.asnidev.hyperbit.icons.NeonPixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonPixelsTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val cyan = 0xFF2DE2E6.toInt()
    private val pink = 0xFFFF3864.toInt()

    @Test
    fun duotoneMapsDarkToDeepAccentAndLightToNeon() {
        val out = NeonPixels.duotone(intArrayOf(black, white, 0), pink, cyan)
        assertEquals(NeonPixels.mix(black, pink, 0.45f), out[0])
        assertEquals(cyan, out[1])
        assertEquals(0, out[2])
    }

    @Test
    fun hueBlendStaysSaturated() {
        // Halfway from pink to cyan by hue is a vivid violet, not gray.
        val mid = NeonPixels.mixHue(pink, cyan, 0.5f)
        assertTrue(NeonPixels.hsv(mid)[1] > 0.6f)
        assertEquals(pink, NeonPixels.mixHue(pink, cyan, 0f))
    }

    @Test
    fun faintLayersAreDropped() {
        val shadow = 0x33000000
        assertEquals(0, NeonPixels.dropFaint(intArrayOf(shadow))[0])
        assertEquals(cyan, NeonPixels.dropFaint(intArrayOf(cyan))[0])
    }

    @Test
    fun glowSpreadsBeyondTheShapeAndKeepsItOnTop() {
        val n = 15
        val px = IntArray(n * n).also { it[7 * n + 7] = white }
        val out = NeonPixels.glow(px, n, cyan, radius = 2, strength = 1f)
        assertEquals(white, out[7 * n + 7])
        assertTrue(NeonPixels.alpha(out[7 * n + 9]) > 0f)
        assertEquals(0f, NeonPixels.alpha(out[0]))
    }

    @Test
    fun wireframeLightsEdgesNotTheMiddle() {
        val n = 12
        val px = IntArray(n * n) { i -> if (i % n in 3..8 && i / n in 3..8) white else 0 }
        val out = NeonPixels.wireframe(px, n, cyan)
        assertTrue(NeonPixels.alpha(out[3 * n + 5]) > 0.9f)   // on the edge
        assertTrue(NeonPixels.alpha(out[6 * n + 6]) < 0.2f)   // inside
        assertEquals(0f, NeonPixels.alpha(out[0]))             // outside
    }

    @Test
    fun hologramDimsEveryThirdRow() {
        val n = 6
        val px = IntArray(n * n) { white }
        val out = NeonPixels.hologram(px, n, cyan, pink)
        assertTrue(NeonPixels.alpha(out[2 * n + 5]) < NeonPixels.alpha(out[1 * n + 5]))
    }

    @Test
    fun blurKeepsTheTotal() {
        val n = 9
        val v = FloatArray(n * n).also { it[4 * n + 4] = 1f }
        val b = NeonPixels.blur(v, n, 1)
        assertEquals(1f, b.sum(), 0.001f)
    }
}
