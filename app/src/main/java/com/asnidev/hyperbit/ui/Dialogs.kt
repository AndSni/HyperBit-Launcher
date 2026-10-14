package com.asnidev.hyperbit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asnidev.hyperbit.BuildConfig
import com.asnidev.hyperbit.ColorSlot
import com.asnidev.hyperbit.Dialog
import com.asnidev.hyperbit.LauncherViewModel
import com.asnidev.hyperbit.data.AppKey
import com.asnidev.hyperbit.data.HPos
import com.asnidev.hyperbit.data.LabelMode
import com.asnidev.hyperbit.data.VPos
import com.asnidev.hyperbit.data.IconStyle
import com.asnidev.hyperbit.data.Prefs

@Composable
fun Dialogs(vm: LauncherViewModel, prefs: Prefs, dialog: Dialog) {
    val dismiss = { vm.dialog = null }
    when (dialog) {
        is Dialog.AppMenu -> AppMenu(vm, prefs, dialog, dismiss)
        is Dialog.TaskbarMenu -> TaskbarMenu(vm, prefs, dialog.index, dismiss)
        is Dialog.IconStyleFor -> IconStyleBox(vm, prefs, dialog.key, dismiss)
        is Dialog.Rename -> RenameBox(vm, dialog.key, dismiss)
        is Dialog.PickColor -> ColorBox(vm, prefs, dialog.slot, dismiss)
        Dialog.About -> AboutBox(dismiss)
        Dialog.Layout -> LayoutBox(vm, prefs, dismiss)
    }
}

/** Full-width choice in a box. */
@Composable
private fun Choice(text: String, onClick: () -> Unit, default: Boolean = false) {
    val r = LocalRetro.current
    RetroButton(text, onClick, Modifier.fillMaxWidth().padding(vertical = r.u * 2), default = default)
}

@Composable
private fun BoxHeader(key: AppKey, vm: LauncherViewModel) {
    val r = LocalRetro.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = r.u * 6)) {
        AppIcon(key, r.line * 2.5f)
        Spacer(Modifier.width(r.u * 8))
        RetroText(vm.label(key), Modifier.weight(1f), maxLines = 2)
    }
}

@Composable
private fun AppMenu(vm: LauncherViewModel, prefs: Prefs, d: Dialog.AppMenu, dismiss: () -> Unit) {
    val r = LocalRetro.current
    val key = d.key
    RetroDialog(dismiss) {
        Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
            BoxHeader(key, vm)
            Choice("Open", { dismiss(); vm.launch(key, null) }, default = true)
            if (key in prefs.home.values) Choice("Remove from home", { dismiss(); vm.removeFromHome(key) })
            else Choice("Add to home", { dismiss(); vm.addToHome(key) })
            Row(
                Modifier.fillMaxWidth().padding(vertical = r.u * 2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(r.u * 4),
            ) {
                RetroText("Taskbar", Modifier.weight(1f))
                for (i in 0 until Prefs.MAX_TASKBAR) {
                    val here = prefs.taskbar.getOrNull(i) == key
                    RetroButton("${i + 1}", {
                        vm.setTaskbar(i, if (here) null else key)
                        if (!here && prefs.taskbarCount <= i) vm.update { it.copy(taskbarCount = i + 1) }
                    }, default = here)
                }
            }
            Choice("Icon style…", { vm.dialog = Dialog.IconStyleFor(key) })
            Choice("Rename…", { vm.dialog = Dialog.Rename(key) })
            Choice("Hide from apps", { dismiss(); vm.hide(key) })
            Choice("App info", { dismiss(); vm.repo.openInfo(key) })
            Choice("Uninstall", { dismiss(); vm.repo.uninstall(key) })
        }
    }
}

@Composable
private fun TaskbarMenu(vm: LauncherViewModel, prefs: Prefs, index: Int, dismiss: () -> Unit) {
    val key = prefs.taskbar.getOrNull(index)
    RetroDialog(dismiss) {
        if (key != null) BoxHeader(key, vm) else RetroText("Button ${index + 1} is empty.", Modifier.padding(bottom = LocalRetro.current.u * 6))
        Choice("Choose app…", { dismiss(); vm.pickingFor = index; vm.drawer = 1f }, default = true)
        if (key != null) {
            Choice("Clear button", { dismiss(); vm.setTaskbar(index, null) })
            Choice("App options…", { vm.dialog = Dialog.AppMenu(key, taskbar = index) })
        }
        Choice("Cancel", dismiss)
    }
}

@Composable
private fun IconStyleBox(vm: LauncherViewModel, prefs: Prefs, key: AppKey, dismiss: () -> Unit) {
    val r = LocalRetro.current
    val override = prefs.iconOverrides[key]
    RetroDialog(dismiss) {
        Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
            BoxHeader(key, vm)
            StyleRow(key, "Same as all (${prefs.iconStyle.label})", prefs.iconStyle, override == null) { vm.setIconStyle(key, null) }
            IconStyle.entries.forEach { s ->
                StyleRow(key, s.label, s, override == s) { vm.setIconStyle(key, s) }
            }
            Spacer(Modifier.height(r.u * 6))
            Choice("Done", dismiss, default = true)
        }
    }
}

/** A choice with the icon drawn in that style beside it. */
@Composable
private fun StyleRow(key: AppKey, label: String, style: IconStyle, selected: Boolean, onSelect: () -> Unit) {
    val r = LocalRetro.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { RetroRadio(label, selected, onSelect) }
        AppIcon(key, r.line * 2f, style = style)
    }
}

@Composable
private fun RenameBox(vm: LauncherViewModel, key: AppKey, dismiss: () -> Unit) {
    val r = LocalRetro.current
    var text by remember { mutableStateOf(vm.label(key)) }
    val save = { vm.rename(key, text); dismiss() }
    RetroDialog(dismiss) {
        BoxHeader(key, vm)
        RetroField(text, { text = it.take(40) }, Modifier.fillMaxWidth(), onDone = save)
        RetroText("Empty brings back the app's own name.", Modifier.padding(vertical = r.u * 4), color = r.ink.copy(alpha = 0.6f), maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(r.u * 6)) {
            RetroButton("Cancel", dismiss, Modifier.weight(1f))
            RetroButton("OK", save, Modifier.weight(1f), default = true)
        }
    }
}

/** Neons, accents and the dark glass and base tones of the presets. */
private val NEON_COLORS = longArrayOf(
    0x2DE2E6, 0xFF3864, 0xFCEE0A, 0x00F0FF, 0xFF71CE, 0x01CDFE, 0x39FF14, 0xFFB000,
    0xFF5F00, 0xFF2A2A, 0xA0E9FF, 0x7B61FF, 0xB967FF, 0xE6F7FF, 0x4DD0E1, 0xFFFFFF,
    0x0D0221, 0x0A0A12, 0x050508, 0x1A0B2E, 0x120458, 0x061018, 0x02060A, 0x001505,
    0x000800, 0x120A00, 0x080400, 0x120404, 0x0B1A2A, 0x050B14, 0x1FA82F, 0x000000,
).map { (it or 0xFF000000).toInt() }.toIntArray()

@Composable
private fun ColorBox(vm: LauncherViewModel, prefs: Prefs, slot: ColorSlot, dismiss: () -> Unit) {
    val r = LocalRetro.current
    val current = slot.of(prefs.theme.palette)
    var hex by remember(current) { mutableStateOf("%06X".format(current and 0xFFFFFF)) }
    fun set(c: Int) = vm.update { it.copy(theme = it.theme.copy(palette = slot.set(it.theme.palette, c))) }
    RetroDialog(dismiss) {
        RetroText(slot.label, Modifier.padding(bottom = r.u * 6))
        NEON_COLORS.toList().chunked(8).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(r.u * 3)) {
                row.forEach { c ->
                    val chosen = c == current
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).padding(vertical = r.u * 1.5f)
                            .border(if (chosen) r.u * 3 else r.u, r.ink)
                            .clickable { set(c) }
                            .padding(if (chosen) r.u * 3 else r.u)
                            .background(Color(c)),
                    )
                }
            }
        }
        Row(Modifier.padding(top = r.u * 6), verticalAlignment = Alignment.CenterVertically) {
            RetroText("Hex #")
            RetroField(hex, { t ->
                hex = t.filter { it.isLetterOrDigit() }.take(6).uppercase()
                hex.takeIf { it.length == 6 }?.toIntOrNull(16)?.let { set(it or 0xFF000000.toInt()) }
            }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(r.u * 6))
        Choice("Done", dismiss, default = true)
    }
}

/**
 * Size and place of the home window, its grid and its icons. Docked at the
 * bottom so the window stays in sight while it changes.
 */
@Composable
private fun LayoutBox(vm: LauncherViewModel, prefs: Prefs, dismiss: () -> Unit) {
    val r = LocalRetro.current
    val step = Prefs.WINDOW_STEP
    RetroDialog(dismiss, alignment = Alignment.BottomCenter) {
        RetroText("Layout", Modifier.padding(bottom = r.u * 4))
        Stepper("Width", "${prefs.windowWidth}%", { d ->
            vm.update { it.copy(windowWidth = (it.windowWidth + d * step).coerceIn(Prefs.WINDOW_WIDTH)) }
        })
        Stepper("Height", "${prefs.windowHeight}%", { d ->
            vm.update { it.copy(windowHeight = (it.windowHeight + d * step).coerceIn(Prefs.WINDOW_HEIGHT)) }
        })
        Stepper("Across", prefs.windowH.label, { d ->
            vm.update { it.copy(windowH = HPos.entries[(it.windowH.ordinal + d).coerceIn(0, HPos.entries.lastIndex)]) }
        })
        Stepper("Up/down", prefs.windowV.label, { d ->
            vm.update { it.copy(windowV = VPos.entries[(it.windowV.ordinal + d).coerceIn(0, VPos.entries.lastIndex)]) }
        })
        HLine(Modifier.padding(vertical = r.u * 3))
        Stepper("Columns", "${prefs.columns}", { d -> vm.update { it.copy(columns = (it.columns + d).coerceIn(Prefs.COLUMNS)) } })
        Stepper("Rows", "${prefs.rows}", { d -> vm.update { it.copy(rows = (it.rows + d).coerceIn(Prefs.ROWS)) } })
        Stepper("Icon size", "${prefs.iconScale}%", { d ->
            vm.update { it.copy(iconScale = (it.iconScale + d * Prefs.ICON_SCALE_STEP).coerceIn(Prefs.ICON_SCALE)) }
        })
        Stepper("Names", prefs.labels.label.substringBefore(' '), { d ->
            vm.update { it.copy(labels = LabelMode.entries[(it.labels.ordinal + d).coerceIn(0, LabelMode.entries.lastIndex)]) }
        })
        if (vm.shownRows in 1 until prefs.rows || vm.shownColumns in 1 until prefs.columns) {
            RetroText(
                "Showing ${vm.shownColumns}×${vm.shownRows}: icons this size need the room. Other apps move to the next page.",
                Modifier.padding(top = r.u * 2), maxLines = 4,
                style = r.text.copy(fontSize = r.text.fontSize * 0.85f),
            )
        }
        Spacer(Modifier.height(r.u * 6))
        Row(horizontalArrangement = Arrangement.spacedBy(r.u * 6)) {
            RetroButton("Defaults", {
                val d = Prefs()
                vm.update {
                    it.copy(
                        windowWidth = d.windowWidth, windowHeight = d.windowHeight, windowH = d.windowH, windowV = d.windowV,
                        columns = d.columns, rows = d.rows, iconScale = d.iconScale, labels = d.labels,
                    )
                }
            }, Modifier.weight(1f))
            RetroButton("Done", dismiss, Modifier.weight(1f), default = true)
        }
    }
}

@Composable
private fun AboutBox(dismiss: () -> Unit) {
    val r = LocalRetro.current
    RetroDialog(dismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            RetroText("HyperBit", align = TextAlign.Center)
            RetroText("-".repeat(15), align = TextAlign.Center)
            Spacer(Modifier.height(r.u * 4))
            RetroText(
                "A home screen in the style of the first graphical desktops.\n\nVersion ${BuildConfig.VERSION_NAME}",
                Modifier.fillMaxWidth(), align = TextAlign.Center, maxLines = 6,
            )
            Spacer(Modifier.height(r.u * 6))
            RetroText(
                "Fonts: VGA 8×16 (int10h.org, CC BY-SA 4.0), Pixelify Sans, VT323, Silkscreen, Press Start 2P (SIL OFL).",
                Modifier.fillMaxWidth(), align = TextAlign.Center, maxLines = 6,
                style = r.text.copy(fontSize = r.text.fontSize * 0.8f),
            )
            Spacer(Modifier.height(r.u * 8))
            RetroButton("OK", dismiss, Modifier.width(r.line * 5), default = true)
        }
    }
}
