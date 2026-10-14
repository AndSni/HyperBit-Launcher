package com.asnidev.hyperbit.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asnidev.hyperbit.R
import com.asnidev.hyperbit.data.FontChoice
import com.asnidev.hyperbit.data.Theme
import com.asnidev.hyperbit.data.WindowStyle
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The look everything is drawn with: the old desktop's layout, drawn at full
 * resolution in neon on dark glass. [u] is the unit for lines and spacing
 * (about 1 dp, a whole number of real pixels so hairlines stay sharp).
 */
@Immutable
class Retro(
    val theme: Theme,
    /** Neon: lines and text. */
    val ink: Color,
    /** The glass windows are made of (already translucent). */
    val paper: Color,
    /** The backdrop's base colour. */
    val desk: Color,
    /** The accent: backdrop lines, highlights, second tones. */
    val deskInk: Color,
    val u: Dp,
    /** [u] in real pixels. */
    val upx: Int,
    val text: TextStyle,
    /** Titles: wider letter spacing, a stronger glow. */
    val title: TextStyle,
    /** Info lines and small print, in the accent colour. */
    val small: TextStyle,
    /** Height of one line of text, the unit for bars, boxes and rows. */
    val line: Dp,
) {
    val window: WindowStyle get() = theme.window
    val bracket: Boolean get() = theme.window == WindowStyle.BRACKET

    /** Bars (title, menu, scroll) are a text line plus a little air. */
    val bar: Dp get() = line + u * 8

    /** 0 (off) to 3 (strong). */
    val glow: Int get() = theme.glow

    /** The shape of windows, boxes and buttons in this style, [cut] deep at the corners. */
    fun shape(cut: Dp = u * 10): Shape = if (bracket) CutCornerShape(cut) else RoundedCornerShape(cut)

    /** Paper as a solid colour, for text drawn on ink. */
    val solidPaper: Color get() = paper.copy(alpha = 1f)
}

val LocalRetro = staticCompositionLocalOf<Retro> { error("no Retro theme") }

fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.CHAKRA -> FontFamily(Font(R.font.chakrapetch_regular), Font(R.font.chakrapetch_semibold, FontWeight.SemiBold))
    FontChoice.ORBITRON -> FontFamily(Font(R.font.orbitron))
    FontChoice.SHARE_TECH -> FontFamily(Font(R.font.sharetechmono_regular))
    FontChoice.VT323 -> FontFamily(Font(R.font.vt323_regular))
    FontChoice.SYSTEM_MONO -> FontFamily.Monospace
    FontChoice.SYSTEM -> FontFamily.SansSerif
}

/** Some fonts are drawn much larger or smaller than others at the same size. */
private fun FontChoice.scale(): Float = when (this) {
    FontChoice.VT323 -> 1.15f
    FontChoice.ORBITRON -> 0.92f
    else -> 1f
}

@Composable
fun rememberRetro(theme: Theme): Retro {
    val density = LocalDensity.current
    return remember(theme, density) {
        val upx = max(1, density.density.roundToInt())
        val u = (upx / density.density).dp
        val size = (theme.textSize * theme.font.scale()).sp
        val line = with(density) { (theme.textSize * 1.35f).sp.toDp() }
        val p = theme.palette
        val ink = Color(p.ink)
        val glowPx = with(density) { (2.dp * theme.glow).toPx() }
        val shadow = if (theme.glow == 0) null else Shadow(ink.copy(alpha = 0.85f), Offset.Zero, glowPx)
        val base = TextStyle(
            fontFamily = theme.font.family(),
            fontSize = size,
            lineHeight = (theme.textSize * 1.35f).sp,
            color = ink,
            shadow = shadow,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
        Retro(
            theme = theme,
            ink = ink,
            paper = Color(p.paper).copy(alpha = 0.84f),
            desk = Color(p.desk),
            deskInk = Color(p.deskInk),
            u = u,
            upx = upx,
            text = base,
            title = base.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.18.em,
                shadow = shadow?.copy(blurRadius = glowPx * 1.6f),
            ),
            small = base.copy(fontSize = size * 0.8f, letterSpacing = 0.06.em, color = Color(p.deskInk), shadow = null),
            line = line,
        )
    }
}

/**
 * A neon outline of [shape]: a bright core [width] wide, and for each glow
 * step a wider, fainter stroke behind it. Drawn behind the content and not
 * clipped, so the halo spills outside.
 */
@Composable
fun Modifier.neon(
    shape: Shape,
    color: Color = LocalRetro.current.ink,
    width: Dp = LocalRetro.current.u,
    glow: Int = LocalRetro.current.glow,
): Modifier {
    val density = LocalDensity.current
    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, density)
        val w = width.toPx()
        for (step in glow downTo 1) {
            drawOutline(outline, color.copy(alpha = 0.08f + 0.05f * (3 - step)), style = Stroke(w + step * 3.dp.toPx()))
        }
        drawOutline(outline, color, style = Stroke(w))
    }
}

/** L-shaped brackets just outside each corner, in [color]: the targeting-frame look. */
@Composable
fun Modifier.cornerBrackets(color: Color = LocalRetro.current.deskInk, arm: Dp = 14.dp): Modifier {
    val r = LocalRetro.current
    return drawWithContent {
        drawContent()
        val w = (r.upx * 2).toFloat()
        val a = arm.toPx()
        val o = r.upx * 4f
        val x0 = -o; val y0 = -o; val x1 = size.width + o; val y1 = size.height + o
        for ((c, sw) in listOf(color.copy(alpha = 0.25f) to w * 3, color to w)) {
            drawLine(c, Offset(x0, y0), Offset(x0 + a, y0), sw); drawLine(c, Offset(x0, y0), Offset(x0, y0 + a), sw)
            drawLine(c, Offset(x1, y0), Offset(x1 - a, y0), sw); drawLine(c, Offset(x1, y0), Offset(x1, y0 + a), sw)
            drawLine(c, Offset(x0, y1), Offset(x0 + a, y1), sw); drawLine(c, Offset(x0, y1), Offset(x0, y1 - a), sw)
            drawLine(c, Offset(x1, y1), Offset(x1 - a, y1), sw); drawLine(c, Offset(x1, y1), Offset(x1, y1 - a), sw)
        }
    }
}

/** Fine horizontal lines every [every] real pixels in [color]: the scan-line texture. */
fun Modifier.scanTexture(color: Color, every: Int = 3): Modifier = drawBehind {
    var y = 0f
    while (y < size.height) {
        drawRect(color, Offset(0f, y), Size(size.width, 1f))
        y += every
    }
}
