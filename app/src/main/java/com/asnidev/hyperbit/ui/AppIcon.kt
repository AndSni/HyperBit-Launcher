package com.asnidev.hyperbit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.asnidev.hyperbit.icons.IconRenderer
import com.asnidev.hyperbit.LauncherViewModel
import com.asnidev.hyperbit.data.AppKey
import com.asnidev.hyperbit.data.IconStyle
import com.asnidev.hyperbit.icons.IconSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

val LocalVm = staticCompositionLocalOf<LauncherViewModel> { error("no view model") }

/** Converted icons this many pixels per side at [small] size: list rows and taskbar. */
fun smallRes(res: Int): Int = max(16, res / 2)


/**
 * An app's icon in the chosen retro style, at most [size] square and snapped
 * so every icon pixel is a whole number of screen pixels. [selected] draws it
 * inverted, as while pressed. [style] overrides the saved style (previews).
 */
@Composable
fun AppIcon(
    key: AppKey,
    size: Dp,
    selected: Boolean = false,
    small: Boolean = false,
    style: IconStyle? = null,
    /** Room available; the icon may round up toward it, never past it. */
    limit: Dp = size,
    modifier: Modifier = Modifier,
) {
    val vm = LocalVm.current
    val prefs by vm.prefs.collectAsState()
    val generation by vm.repo.generation.collectAsState()
    val s = style ?: prefs.styleFor(key)
    val res = if (small) smallRes(prefs.iconRes) else prefs.iconRes
    val pal = prefs.theme.palette
    val glow = if (prefs.iconOutline) prefs.theme.glow else 0
    val spec = IconSpec(s, res, pal.ink, pal.deskInk, glow, prefs.iconStripBackground, selected)
    val id = "${key.encode()}@$generation"

    val image: ImageBitmap? by produceState(
        // While a new look is drawn, show the other state of the same icon rather than nothing.
        initialValue = vm.icons.cached(id, spec) ?: vm.icons.cached(id, spec.copy(selected = !selected)),
        id, spec,
    ) {
        value = withContext(Dispatchers.Default) {
            val drawable = vm.repo.icon(key) ?: return@withContext null
            val img = vm.icons.render(id, drawable, spec)
            // The pressed look too, so a touch shows it at once.
            vm.repo.icon(key)?.let { vm.icons.render(id, it, spec.copy(selected = !selected)) }
            img
        }
    }

    val density = LocalDensity.current
    val side = with(density) { minOf(size, maxOf(size, limit)).roundToPx() }
    Canvas(modifier.size(with(density) { side.toDp() })) {
        val img = image ?: return@Canvas
        // The bitmap has room for the halo around the picture; the picture fills
        // the layout box and the halo spills past it.
        val drawn = side * IconRenderer.SIDE / IconRenderer.INNER
        val off = (side - drawn) / 2
        drawImage(
            img,
            dstOffset = IntOffset(off, off),
            dstSize = IntSize(drawn, drawn),
            filterQuality = if (s == IconStyle.PIXEL) FilterQuality.None else FilterQuality.High,
        )
    }
}
