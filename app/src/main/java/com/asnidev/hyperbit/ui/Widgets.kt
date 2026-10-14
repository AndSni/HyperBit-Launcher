package com.asnidev.hyperbit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ---------------------------------------------------------------- text

@Composable
fun RetroText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalRetro.current.ink,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    style: TextStyle = LocalRetro.current.text,
) {
    // Text on a lit (pressed) background drops its glow: a halo there just smudges.
    val glowless = color == LocalRetro.current.solidPaper
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = align, shadow = if (glowless) null else style.shadow),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
    )
}

/** A row that lights up (neon fill, dark text) while pressed, as menus and lists do. */
@Composable
fun InvertingRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.(fg: Color) -> Unit,
) {
    val r = LocalRetro.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .then(if (pressed) Modifier.background(r.ink.copy(alpha = 0.9f)) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) { content(if (pressed) r.solidPaper else r.ink) }
}

// ---------------------------------------------------------------- frames

/**
 * Glass with a neon edge in this style's shape. [shadow] adds the corner
 * brackets (the old drop shadow's job: lift the box off the desk); [thick]
 * a brighter, wider edge; [solid] nearly opaque glass, for anything drawn
 * over another window.
 */
@Composable
fun Modifier.frame(shadow: Boolean = false, thick: Boolean = false, solid: Boolean = false): Modifier {
    val r = LocalRetro.current
    val shape = r.shape()
    val base = if (shadow && r.bracket) cornerBrackets() else this
    return base
        .neon(shape, width = if (thick) r.u * 2 else r.u)
        .clip(shape)
        .background(if (solid) r.paper.copy(alpha = 0.97f) else r.paper)
        .padding(if (thick) r.u * 2 else r.u)
}

/** A window: title bar, an optional info line, content and optional scroll bars. */
@Composable
fun RetroWindow(
    title: String,
    modifier: Modifier = Modifier,
    info: (@Composable RowScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onFull: (() -> Unit)? = null,
    vScroll: ScrollInfo? = null,
    hScroll: ScrollInfo? = null,
    titleModifier: Modifier = Modifier,
    /** Over another window (drawer, options): glass you can't see through. */
    solid: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val r = LocalRetro.current
    Column(modifier.frame(shadow = true, solid = solid)) {
        TitleBar(title, onClose, onFull, titleModifier)
        if (info != null) {
            Row(
                Modifier.fillMaxWidth().height(r.line + r.u * 6).padding(horizontal = r.u * 10),
                verticalAlignment = Alignment.CenterVertically,
                content = info,
            )
            HLine(alpha = 0.35f)
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.weight(1f).fillMaxHeight(), content = content)
            if (vScroll != null) {
                Scrollbar(vScroll, vertical = true, Modifier.width(r.bar).fillMaxHeight())
            }
        }
        if (hScroll != null) {
            Row(Modifier.fillMaxWidth().height(r.bar)) {
                Scrollbar(hScroll, vertical = false, Modifier.weight(1f).fillMaxHeight())
                if (vScroll != null) Box(Modifier.width(r.bar).fillMaxHeight())
            }
        }
    }
}

/** A hairline across, in neon at [alpha]. */
@Composable
fun HLine(modifier: Modifier = Modifier, alpha: Float = 0.6f) {
    val r = LocalRetro.current
    Box(modifier.fillMaxWidth().height(r.u).background(r.ink.copy(alpha = alpha)))
}

@Composable
fun VLine(modifier: Modifier = Modifier, alpha: Float = 0.6f) {
    val r = LocalRetro.current
    Box(modifier.width(r.u).fillMaxHeight().background(r.ink.copy(alpha = alpha)))
}

@Composable
private fun TitleBar(title: String, onClose: (() -> Unit)?, onFull: (() -> Unit)?, modifier: Modifier) {
    val r = LocalRetro.current
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(r.bar)) {
            // The old patterned title bar, at full resolution: fine diagonal hatching
            // (bracket) or scan lines (scanline), fading toward the title.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val c = r.ink.copy(alpha = 0.32f)
                    if (r.bracket) {
                        val step = 6.dp.toPx()
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(c, Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
                            x += step
                        }
                    } else {
                        var y = 3.dp.toPx()
                        while (y < size.height - 2.dp.toPx()) {
                            drawRect(c, Offset(0f, y), Size(size.width, 1f))
                            y += 3.dp.toPx() / 2
                        }
                    }
                    // Fade the texture out toward the middle, where the title sits.
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent, 0.3f to r.solidPaper, 0.7f to r.solidPaper, 1f to Color.Transparent,
                        ),
                    )
                },
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                GlyphBox(onClose) { closeGlyph(it) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    RetroText(title, style = r.title)
                }
                GlyphBox(onFull) { fullGlyph(it) }
            }
        }
        HLine()
    }
}

/** A square touch target for a drawn glyph; it lights up while pressed. */
@Composable
private fun GlyphBox(onClick: (() -> Unit)?, size: Dp = LocalRetro.current.bar, glyph: DrawScope.(Color) -> Unit) {
    val r = LocalRetro.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Canvas(
        Modifier.size(size)
            .then(if (onClick != null) Modifier.clickable(source, null, onClick = onClick) else Modifier),
    ) {
        if (pressed) drawCircle(r.ink.copy(alpha = 0.25f), radius = this.size.minDimension * 0.45f)
        glyph(if (pressed) Color.White else r.ink)
    }
}

/** Draws [path] in [c] with a soft halo, line width [w]. */
private fun DrawScope.neonPath(path: Path, c: Color, w: Float, glow: Int) {
    for (step in glow downTo 1) drawPath(path, c.copy(alpha = 0.15f), style = Stroke(w + step * 2.5f * density, cap = StrokeCap.Round))
    drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round))
}

private fun DrawScope.closeGlyph(c: Color) {
    val s = size.minDimension
    val k = s * 0.3f
    val p = Path().apply {
        moveTo(center.x - k / 2, center.y - k / 2); lineTo(center.x + k / 2, center.y + k / 2)
        moveTo(center.x + k / 2, center.y - k / 2); lineTo(center.x - k / 2, center.y + k / 2)
    }
    neonPath(p, c, 1.5f * density, 2)
    drawRect(c.copy(alpha = 0.7f), Offset(center.x - k, center.y - k), Size(k * 2, k * 2), style = Stroke(density))
}

private fun DrawScope.fullGlyph(c: Color) {
    val s = size.minDimension
    val k = s * 0.3f
    val p = Path().apply {
        moveTo(center.x, center.y - k); lineTo(center.x + k, center.y); lineTo(center.x, center.y + k); lineTo(center.x - k, center.y); close()
        moveTo(center.x, center.y - k / 2.5f); lineTo(center.x + k / 2.5f, center.y); lineTo(center.x, center.y + k / 2.5f); lineTo(center.x - k / 2.5f, center.y); close()
    }
    neonPath(p, c, 1.5f * density, 2)
}

// ---------------------------------------------------------------- scroll bars

/**
 * [position] 0..1 of the scrollable range, [visible] the share on screen (thumb
 * size). [step] moves one notch (-1/+1); [jump] scrolls to a 0..1 position.
 */
class ScrollInfo(
    val position: Float,
    val visible: Float,
    val step: (Int) -> Unit,
    val jump: (Float) -> Unit,
)

@Composable
fun Scrollbar(info: ScrollInfo, vertical: Boolean, modifier: Modifier) {
    if (vertical) {
        Column(modifier) {
            GlyphBox({ info.step(-1) }) { chevron(it, 0) }
            Track(info, vertical = true, Modifier.weight(1f).fillMaxWidth())
            GlyphBox({ info.step(1) }) { chevron(it, 2) }
        }
    } else {
        Row(modifier) {
            GlyphBox({ info.step(-1) }) { chevron(it, 3) }
            Track(info, vertical = false, Modifier.weight(1f).fillMaxHeight())
            GlyphBox({ info.step(1) }) { chevron(it, 1) }
        }
    }
}

/** A thin rail with tick marks and a glowing thumb that can be dragged. */
@Composable
private fun Track(info: ScrollInfo, vertical: Boolean, modifier: Modifier) {
    val r = LocalRetro.current
    val density = LocalDensity.current
    var length by remember { mutableFloatStateOf(0f) }
    val thumbMin = with(density) { r.bar.toPx() }
    val thumb = (length * info.visible.coerceIn(0f, 1f)).coerceAtLeast(thumbMin).coerceAtMost(length)
    val room = (length - thumb).coerceAtLeast(0f)
    val at = room * info.position.coerceIn(0f, 1f)
    // Read through a lambda so a drag that started stays on the latest position.
    val latest by rememberUpdatedState(info)
    Box(
        modifier
            .onSizeChanged { length = if (vertical) it.height.toFloat() else it.width.toFloat() }
            .drawBehind {
                val c = r.ink.copy(alpha = 0.35f)
                val tick = 8.dp.toPx()
                if (vertical) {
                    drawLine(c, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), density.density)
                    var y = 0f
                    while (y < size.height) { drawLine(c, Offset(size.width * 0.38f, y), Offset(size.width * 0.62f, y), 1f); y += tick }
                } else {
                    drawLine(c, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), density.density)
                    var x = 0f
                    while (x < size.width) { drawLine(c, Offset(x, size.height * 0.38f), Offset(x, size.height * 0.62f), 1f); x += tick }
                }
            }
            .pointerInput(vertical) {
                detectTapGestures { o ->
                    val p = if (vertical) o.y else o.x
                    val l = if (vertical) size.height else size.width
                    // Page toward the tap, like clicking the gray area.
                    val cur = latest.position
                    val dir = if (p / l > cur) 1 else -1
                    latest.jump((cur + dir * latest.visible).coerceIn(0f, 1f))
                }
            },
    ) {
        if (length > 0f) {
            var dragAt by remember { mutableFloatStateOf(0f) }
            val shape = r.shape(r.u * 4)
            val thumbDp = with(density) { thumb.toDp() }
            Box(
                Modifier
                    .offset { if (vertical) IntOffset(0, at.roundToInt()) else IntOffset(at.roundToInt(), 0) }
                    .then(if (vertical) Modifier.fillMaxWidth().height(thumbDp) else Modifier.fillMaxHeight().width(thumbDp))
                    .padding(r.u * 5)
                    .neon(shape)
                    .background(r.ink.copy(alpha = 0.22f), shape)
                    .pointerInput(vertical) {
                        detectDragGestures(onDragStart = { dragAt = latest.position * room }) { change, d ->
                            change.consume()
                            dragAt = (dragAt + if (vertical) d.y else d.x).coerceIn(0f, room)
                            if (room > 0f) latest.jump(dragAt / room)
                        }
                    },
            )
        }
    }
}

/** A chevron pointing up (0), right (1), down (2) or left (3). */
private fun DrawScope.chevron(c: Color, dir: Int) {
    val k = size.minDimension * 0.16f
    val p = Path().apply {
        moveTo(-k * 1.4f, k * 0.7f); lineTo(0f, -k * 0.7f); lineTo(k * 1.4f, k * 0.7f)
    }
    drawContext.transform.translate(center.x, center.y)
    drawContext.transform.rotate(dir * 90f, Offset.Zero)
    neonPath(p, c, 1.6f * density, 2)
    drawContext.transform.rotate(-dir * 90f, Offset.Zero)
    drawContext.transform.translate(-center.x, -center.y)
}

// ---------------------------------------------------------------- controls

/** A push button; lit (filled) while pressed. [default] draws the brighter edge of the default choice. */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    default: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable (fg: Color) -> Unit)? = null,
) {
    val r = LocalRetro.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = r.shape(r.u * 8)
    val fg = if (pressed) r.solidPaper else r.ink
    Row(
        modifier
            .neon(shape, color = if (default) r.deskInk else r.ink, width = if (default) r.u * 2 else r.u, glow = if (pressed) 3 else r.glow)
            .clip(shape)
            .background(if (pressed) r.ink else r.paper)
            .combinedClick(source, enabled, onClick, onLongClick)
            .padding(horizontal = r.u * 10, vertical = r.u * 6),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading(fg)
            if (text.isNotEmpty()) Spacer(Modifier.width(r.u * 6))
        }
        if (text.isNotEmpty()) RetroText(text, color = if (enabled) fg else fg.copy(alpha = 0.4f), align = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClick(
    source: MutableInteractionSource,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier = combinedClickable(
    interactionSource = source,
    indication = null,
    enabled = enabled,
    onLongClick = onLongClick,
    onClick = onClick,
)

/** A square that fills with light when on. */
@Composable
fun RetroCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val r = LocalRetro.current
    InvertingRow({ onChange(!checked) }, modifier.fillMaxWidth().padding(vertical = r.u * 2)) { fg ->
        val shape = r.shape(r.u * 3)
        Box(Modifier.padding(r.u * 3).size(r.line - r.u * 4).neon(shape, fg, glow = if (checked) r.glow else 0).padding(r.u * 3)) {
            if (checked) Box(Modifier.fillMaxSize().neon(shape, fg).background(fg, shape))
        }
        Spacer(Modifier.width(r.u * 6))
        RetroText(label, Modifier.weight(1f), color = fg, maxLines = 2)
    }
}

/** A diamond with a lit core when chosen. */
@Composable
fun RetroRadio(label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val r = LocalRetro.current
    InvertingRow(onSelect, modifier.fillMaxWidth().padding(vertical = r.u * 2)) { fg ->
        Canvas(Modifier.padding(r.u * 3).size(r.line - r.u * 4)) {
            val k = size.minDimension / 2
            fun diamond(s: Float) = Path().apply {
                moveTo(center.x, center.y - s); lineTo(center.x + s, center.y); lineTo(center.x, center.y + s); lineTo(center.x - s, center.y); close()
            }
            neonPath(diamond(k * 0.9f), fg, 1.3f * density, if (selected) r.glow else 0)
            if (selected) drawPath(diamond(k * 0.45f), fg)
        }
        Spacer(Modifier.width(r.u * 6))
        RetroText(label, Modifier.weight(1f), color = fg, maxLines = 2)
    }
}

/** "label   ‹ value ›" */
@Composable
fun Stepper(label: String, value: String, onStep: (Int) -> Unit, modifier: Modifier = Modifier) {
    val r = LocalRetro.current
    Row(modifier.fillMaxWidth().padding(vertical = r.u * 2), verticalAlignment = Alignment.CenterVertically) {
        RetroText(label, Modifier.weight(1f))
        GlyphBox({ onStep(-1) }) { chevron(it, 3) }
        RetroText(value, Modifier.widthIn(min = r.line * 2.5f).padding(horizontal = r.u * 4), color = r.deskInk, align = TextAlign.Center)
        GlyphBox({ onStep(1) }) { chevron(it, 1) }
    }
}

/** One-line text field on a glowing underline. */
@Composable
fun RetroField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusRequester? = null,
    onDone: (() -> Unit)? = null,
) {
    val r = LocalRetro.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier
            .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
            .drawBehind {
                val y = size.height - r.upx
                drawRect(r.ink.copy(alpha = 0.18f), Offset(0f, y - 3 * r.upx), Size(size.width, 4f * r.upx))
                drawRect(r.ink, Offset(0f, y), Size(size.width, r.upx.toFloat()))
            }
            .padding(vertical = r.u * 3),
        textStyle = r.text,
        singleLine = true,
        cursorBrush = SolidColor(r.deskInk),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
    )
}

/** A section heading in settings: "// TEXT" in the accent, trailing off in a rule. */
@Composable
fun Heading(text: String) {
    val r = LocalRetro.current
    Row(Modifier.fillMaxWidth().padding(top = r.u * 14, bottom = r.u * 4), verticalAlignment = Alignment.CenterVertically) {
        RetroText("// ${text.uppercase()}", style = r.title.copy(fontSize = r.text.fontSize * 0.85f), color = r.deskInk)
        Spacer(Modifier.width(r.u * 8))
        Box(
            Modifier.weight(1f).height(r.u)
                .background(Brush.horizontalGradient(listOf(r.deskInk.copy(alpha = 0.8f), Color.Transparent))),
        )
    }
}

/**
 * A modal box (glass, neon edge, corner brackets), centred unless [alignment]
 * says otherwise. Tapping outside it calls [onDismiss].
 */
@Composable
fun RetroDialog(
    onDismiss: () -> Unit,
    maxWidth: Dp = 380.dp,
    alignment: Alignment = Alignment.Center,
    content: @Composable ColumnScope.() -> Unit,
) {
    val r = LocalRetro.current
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .padding(r.u * 16),
        contentAlignment = alignment,
    ) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } } // taps inside don't dismiss
                .cornerBrackets()
                .frame(thick = true, solid = true)
                .padding(r.u * 12),
            content = content,
        )
    }
}
