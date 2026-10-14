package com.asnidev.hyperbit.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.asnidev.hyperbit.BuildConfig
import com.asnidev.hyperbit.ColorSlot
import com.asnidev.hyperbit.Dialog
import com.asnidev.hyperbit.LauncherViewModel
import com.asnidev.hyperbit.data.FontChoice
import com.asnidev.hyperbit.data.IconStyle
import com.asnidev.hyperbit.data.InfoBar
import com.asnidev.hyperbit.data.LabelMode
import com.asnidev.hyperbit.data.Backdrop
import androidx.compose.ui.draw.clip
import com.asnidev.hyperbit.data.Prefs
import com.asnidev.hyperbit.data.Presets
import com.asnidev.hyperbit.data.Theme
import com.asnidev.hyperbit.data.WindowStyle
import com.asnidev.hyperbit.system.SystemActions
import kotlinx.coroutines.launch

/** Everything adjustable, in one scrolling window over the desktop. */
@Composable
fun SettingsWindow(vm: LauncherViewModel, prefs: Prefs) {
    val r = LocalRetro.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val apps by vm.apps.collectAsState()
    fun theme(t: (Theme) -> Theme) = vm.update { it.copy(theme = t(it.theme)) }

    val vScroll = ScrollInfo(
        position = if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue,
        visible = if (scroll.maxValue == 0) 1f else scroll.viewportSize.toFloat() / (scroll.maxValue + scroll.viewportSize),
        step = { d -> scope.launch { scroll.animateScrollBy(d * scroll.viewportSize * 0.25f) } },
        jump = { f -> scope.launch { scroll.scrollTo((f * scroll.maxValue).toInt()) } },
    )

    RetroWindow(
        title = if (prefs.theme.window == WindowStyle.BRACKET) "SYS://OPTIONS" else "Options",
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(r.u * 8),
        onClose = { vm.settingsOpen = false },
        vScroll = vScroll,
        solid = true,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = r.u * 8, vertical = r.u * 4)) {

            Heading("Presets")
            Presets.all.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = r.u * 2), horizontalArrangement = Arrangement.spacedBy(r.u * 6)) {
                    pair.forEach { p ->
                        RetroButton(
                            p.name, { vm.update { it.copy(theme = p.theme) } }, Modifier.weight(1f),
                            default = prefs.theme == p.theme,
                            leading = { _ -> Swatch(Color(p.theme.palette.desk), Color(p.theme.palette.ink), Color(p.theme.palette.paper)) },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Heading("Colors")
            ColorSlot.entries.forEach { slot ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.dialog = Dialog.PickColor(slot) }.padding(vertical = r.u * 3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RetroText(slot.label, Modifier.weight(1f))
                    RetroText("#%06X".format(slot.of(prefs.theme.palette) and 0xFFFFFF), Modifier.padding(end = r.u * 6))
                    Box(Modifier.size(r.line * 1.6f, r.line).neon(r.shape(r.u * 3), Color(slot.of(prefs.theme.palette))).background(Color(slot.of(prefs.theme.palette)), r.shape(r.u * 3)))
                }
            }

            Heading("Backdrop")
            Backdrop.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = r.u * 4), horizontalArrangement = Arrangement.spacedBy(r.u * 8)) {
                    row.forEach { b ->
                        val chosen = prefs.theme.backdrop == b
                        val shape = r.shape(r.u * 6)
                        Column(Modifier.weight(1f).clickable { theme { it.copy(backdrop = b) } }) {
                            // A still preview of each backdrop in the current colours.
                            BackdropView(
                                b, animate = false,
                                Modifier.fillMaxWidth().aspectRatio(0.75f)
                                    .neon(shape, if (chosen) r.deskInk else r.ink.copy(alpha = 0.4f), if (chosen) r.u * 2 else r.u, if (chosen) r.glow else 0)
                                    .clip(shape),
                            )
                            RetroText(b.label, Modifier.padding(top = r.u * 3), style = r.small, color = if (chosen) r.deskInk else r.ink)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            RetroCheckbox("Animate the backdrop", prefs.animate, { v -> vm.update { it.copy(animate = v) } })

            Heading("Glow")
            Stepper("Neon glow", listOf("off", "low", "medium", "high")[prefs.theme.glow], { d -> theme { it.copy(glow = (it.glow + d).coerceIn(0, 3)) } })
            RetroCheckbox("Scan lines and vignette", prefs.theme.scanlines, { v -> theme { it.copy(scanlines = v) } })

            Heading("Windows")
            WindowStyle.entries.forEach { w ->
                RetroRadio("${w.label} title bars", prefs.theme.window == w, { theme { it.copy(window = w) } })
            }
            Spacer(Modifier.height(r.u * 4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RetroText("Home title ")
                RetroField(prefs.homeTitle, { t -> vm.update { it.copy(homeTitle = t.take(40)) } }, Modifier.weight(1f))
            }
            RetroText("Empty uses the default.", Modifier.padding(top = r.u * 2), color = r.ink.copy(alpha = 0.6f))

            Heading("Font")
            FontChoice.entries.forEach { f ->
                RetroRadio(f.label, prefs.theme.font == f, { theme { it.copy(font = f) } })
            }
            Stepper("Text size", "${prefs.theme.textSize}", { d ->
                theme { it.copy(textSize = (it.textSize + d).coerceIn(Prefs.TEXT_SIZES)) }
            })

            Heading("Home window")
            RetroText(
                "${prefs.windowWidth}% wide, ${prefs.windowHeight}% high, ${prefs.windowV.label.lowercase()} ${prefs.windowH.label.lowercase()}",
                Modifier.padding(vertical = r.u * 2),
            )
            RetroButton(
                "Layout…",
                { vm.settingsOpen = false; vm.dialog = Dialog.Layout },
                Modifier.fillMaxWidth().padding(bottom = r.u * 4),
            )
            Stepper("Columns", "${prefs.columns}", { d -> vm.update { it.copy(columns = (it.columns + d).coerceIn(Prefs.COLUMNS)) } })
            Stepper("Rows", "${prefs.rows}", { d -> vm.update { it.copy(rows = (it.rows + d).coerceIn(Prefs.ROWS)) } })
            RetroText("App names", Modifier.padding(top = r.u * 4))
            LabelMode.entries.forEach { m ->
                RetroRadio(m.label, prefs.labels == m, { vm.update { it.copy(labels = m) } })
            }
            RetroText("Info line", Modifier.padding(top = r.u * 4))
            InfoBar.entries.forEach { b ->
                RetroRadio(b.label, prefs.infoBar == b, { vm.update { it.copy(infoBar = b) } })
            }

            Heading("Icons")
            IconStyle.entries.forEach { s ->
                RetroRadio(s.label, prefs.iconStyle == s, { vm.update { it.copy(iconStyle = s) } })
            }
            Stepper("Size", "${prefs.iconScale}%", { d ->
                vm.update { it.copy(iconScale = (it.iconScale + d * Prefs.ICON_SCALE_STEP).coerceIn(Prefs.ICON_SCALE)) }
            })
            if (prefs.iconStyle == IconStyle.PIXEL) Stepper("Pixels", "${prefs.iconRes}×${prefs.iconRes}", { d ->
                vm.update {
                    val i = (Prefs.ICON_RES.indexOf(it.iconRes) + d).coerceIn(0, Prefs.ICON_RES.lastIndex)
                    it.copy(iconRes = Prefs.ICON_RES[i])
                }
            })
            RetroCheckbox("Glow around icons", prefs.iconOutline, { v -> vm.update { it.copy(iconOutline = v) } })
            RetroCheckbox("Drop icon backgrounds", prefs.iconStripBackground, { v -> vm.update { it.copy(iconStripBackground = v) } })
            if (prefs.iconOverrides.isNotEmpty()) {
                RetroButton(
                    "Reset ${prefs.iconOverrides.size} app styles",
                    { vm.update { it.copy(iconOverrides = emptyMap()) } },
                    Modifier.padding(top = r.u * 4),
                )
            }

            Heading("Taskbar")
            Stepper("Buttons", if (prefs.taskbarCount == 0) "off" else "${prefs.taskbarCount}", { d ->
                vm.update { it.copy(taskbarCount = (it.taskbarCount + d).coerceIn(0, Prefs.MAX_TASKBAR)) }
            })
            RetroCheckbox("App names on buttons", prefs.taskbarLabels, { v -> vm.update { it.copy(taskbarLabels = v) } })
            RetroText("Hold a button to change its app.", Modifier.padding(top = r.u * 2), color = r.ink.copy(alpha = 0.6f))

            Heading("Desktop")
            RetroCheckbox("Menu bar", prefs.showMenuBar, { v -> vm.update { it.copy(showMenuBar = v) } })
            RetroCheckbox("Clock in the menu bar", prefs.menuClock, { v -> vm.update { it.copy(menuClock = v) } })
            RetroCheckbox("Hide the status bar", prefs.hideStatusBar, { v -> vm.update { it.copy(hideStatusBar = v) } })
            RetroCheckbox("Swipe down for notifications", prefs.swipeDownNotifications, { v -> vm.update { it.copy(swipeDownNotifications = v) } })
            RetroCheckbox("Zoom frames when opening apps", prefs.zoomRects, { v -> vm.update { it.copy(zoomRects = v) } })
            if (!prefs.showMenuBar) {
                RetroText("Without the menu bar, hold an empty spot in the home window for options.", Modifier.padding(top = r.u * 2), color = r.ink.copy(alpha = 0.6f), maxLines = 3)
            }

            if (prefs.hidden.isNotEmpty()) {
                Heading("Hidden apps")
                prefs.hidden.forEach { key ->
                    Row(Modifier.fillMaxWidth().padding(vertical = r.u * 2), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(key, r.line * 1.5f, small = true)
                        Spacer(Modifier.width(r.u * 6))
                        RetroText(apps.firstOrNull { it.key == key }?.label ?: key.pkg, Modifier.weight(1f))
                        RetroButton("Show", { vm.unhide(key) })
                    }
                }
            }

            Heading("System")
            if (!SystemActions.isDefaultLauncher(context)) {
                RetroButton("Make HyperBit the home app", { (context as? Activity)?.let(SystemActions::requestDefaultLauncher) }, Modifier.fillMaxWidth(), default = true)
            } else {
                RetroText("HyperBit is the home app.")
            }
            Spacer(Modifier.height(r.u * 4))
            RetroButton("About HyperBit", { vm.dialog = Dialog.About }, Modifier.fillMaxWidth())
            RetroText("Version ${BuildConfig.VERSION_NAME}", Modifier.padding(vertical = r.u * 6), color = r.ink.copy(alpha = 0.6f))
        }
    }
}

/** Three colour chips: a theme at a glance. */
@Composable
private fun Swatch(desk: Color, ink: Color, paper: Color) {
    val r = LocalRetro.current
    Row(Modifier.border(r.u, r.ink)) {
        Box(Modifier.size(r.line * 0.6f, r.line).background(desk))
        Box(Modifier.size(r.line * 0.6f, r.line).background(paper))
        Box(Modifier.size(r.line * 0.6f, r.line).background(ink))
    }
}
