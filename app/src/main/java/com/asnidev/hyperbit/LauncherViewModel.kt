package com.asnidev.hyperbit

import android.app.Application
import android.graphics.Rect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.viewModelScope
import com.asnidev.hyperbit.apps.AppEntry
import com.asnidev.hyperbit.apps.AppRepository
import com.asnidev.hyperbit.data.AppKey
import com.asnidev.hyperbit.data.IconStyle
import com.asnidev.hyperbit.data.Prefs
import com.asnidev.hyperbit.data.SettingsStore
import com.asnidev.hyperbit.icons.IconRenderer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/** The boxes that can sit over the desktop; one at a time. */
sealed interface Dialog {
    /** An app's long-press box. [slot] is its home slot, [taskbar] its button, when opened from there. */
    data class AppMenu(val key: AppKey, val slot: Int? = null, val taskbar: Int? = null) : Dialog
    data class TaskbarMenu(val index: Int) : Dialog
    data class IconStyleFor(val key: AppKey) : Dialog
    data class Rename(val key: AppKey) : Dialog
    data class PickColor(val slot: ColorSlot) : Dialog
    data object About : Dialog
    data object Layout : Dialog
}

/** The four theme colours, for the colour picker. */
enum class ColorSlot(val label: String) {
    INK("Neon"), PAPER("Glass"), DESK("Backdrop"), DESK_INK("Accent");

    fun of(p: com.asnidev.hyperbit.data.Palette): Int = when (this) {
        INK -> p.ink; PAPER -> p.paper; DESK -> p.desk; DESK_INK -> p.deskInk
    }

    fun set(p: com.asnidev.hyperbit.data.Palette, c: Int) = when (this) {
        INK -> p.copy(ink = c); PAPER -> p.copy(paper = c); DESK -> p.copy(desk = c); DESK_INK -> p.copy(deskInk = c)
    }
}

/** Where a launch started, in window pixels, for the zoom rectangles. */
data class Zoom(val from: androidx.compose.ui.geometry.Rect, val id: Long = System.nanoTime())

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    // Background work that fails is logged, never allowed to crash the home screen.
    private val scope: CoroutineScope =
        viewModelScope + CoroutineExceptionHandler { _, e -> Log.w(TAG, "background task failed", e) }

    val repo = AppRepository(app, scope)
    private val store = SettingsStore(app)
    val icons = IconRenderer()

    val apps: StateFlow<List<AppEntry>> = repo.apps
    val prefs: StateFlow<Prefs> = store.prefs.stateIn(scope, SharingStarted.Eagerly, Prefs())

    /** False until the saved settings are read, so the default look never flashes. */
    val loaded: StateFlow<Boolean> = store.prefs.map { true }.stateIn(scope, SharingStarted.Eagerly, false)

    /** 0 = closed, 1 = open; follows the finger while swiping. */
    var drawer by mutableFloatStateOf(0f)

    /** Set by "Find…": the drawer opens with the keyboard up. */
    var focusSearch by mutableStateOf(false)

    var settingsOpen by mutableStateOf(false)
    var dialog by mutableStateOf<Dialog?>(null)

    /** Set while the drawer is open to choose an app for this taskbar button. */
    var pickingFor by mutableStateOf<Int?>(null)

    var zoom by mutableStateOf<Zoom?>(null)

    /** Columns and rows the home window really shows (fewer than set when icons need the room); set by its layout. */
    var shownColumns by mutableIntStateOf(0)
    var shownRows by mutableIntStateOf(0)

    init {
        scope.launch {
            val saved = store.prefs.first()
            if (saved.seeded) return@launch
            // Wait for the first app list, then fill taskbar and home once.
            val list = repo.apps.first { it.isNotEmpty() }
            val defaults = repo.defaults().ifEmpty { list.take(8).map { it.key } }
            store.update { p ->
                if (p.seeded) p else p.copy(
                    taskbar = List(Prefs.MAX_TASKBAR) { defaults.getOrNull(it) },
                    home = defaults.drop(Prefs.MAX_TASKBAR).withIndex().associate { (i, k) -> i to k },
                    seeded = true,
                )
            }
        }
    }

    fun label(entry: AppEntry): String = prefs.value.renames[entry.key] ?: entry.label

    fun label(key: AppKey): String =
        prefs.value.renames[key] ?: apps.value.firstOrNull { it.key == key }?.label ?: key.pkg.substringAfterLast('.')

    fun launch(key: AppKey, from: androidx.compose.ui.geometry.Rect?) {
        if (from != null && prefs.value.zoomRects) zoom = Zoom(from)
        val bounds = from?.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
        repo.launch(key, bounds)
    }

    /** Back to the bare desktop: Home pressed, or the launcher left. */
    fun closeAll() {
        dialog = null
        settingsOpen = false
        pickingFor = null
        focusSearch = false
        drawer = 0f
    }

    fun update(transform: (Prefs) -> Prefs) {
        scope.launch { store.update(transform) }
    }

    // --- home window ---

    fun addToHome(key: AppKey) = update { p ->
        if (key in p.home.values) p else p.copy(home = p.home + (p.firstFreeSlot() to key))
    }

    fun removeFromHome(key: AppKey) = update { p -> p.copy(home = p.home.filterValues { it != key }) }

    fun moveSlot(from: Int, to: Int) = update { it.swap(from, to) }

    fun cleanUp() = update { it.cleanUp() }

    // --- taskbar ---

    fun setTaskbar(index: Int, key: AppKey?) = update { p ->
        p.copy(taskbar = p.taskbar.mapIndexed { i, k -> if (i == index) key else k })
    }

    // --- per app ---

    fun rename(key: AppKey, label: String) = update {
        it.copy(renames = if (label.isBlank()) it.renames - key else it.renames + (key to label.trim()))
    }

    fun setIconStyle(key: AppKey, style: IconStyle?) = update {
        it.copy(iconOverrides = if (style == null) it.iconOverrides - key else it.iconOverrides + (key to style))
    }

    fun hide(key: AppKey) = update { it.withoutApp(key).copy(hidden = it.hidden + key) }
    fun unhide(key: AppKey) = update { it.copy(hidden = it.hidden - key) }

    /**
     * Debug builds: every app's icon in each style side by side, written to
     * cache/iconsheet.png (`--ez iconsheet true`), in the current colours.
     */
    fun iconSheet() {
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val list = apps.first { it.isNotEmpty() }.take(24)
            val p = prefs.value
            val side = com.asnidev.hyperbit.icons.IconRenderer.SIDE
            val styles = IconStyle.entries
            val sheet = android.graphics.Bitmap.createBitmap(side * styles.size, side * list.size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(sheet)
            canvas.drawColor(p.theme.palette.paper)
            for ((row, app) in list.withIndex()) for ((col, style) in styles.withIndex()) {
                val d = repo.icon(app.key) ?: continue
                val spec = com.asnidev.hyperbit.icons.IconSpec(
                    style, p.iconRes, p.theme.palette.ink, p.theme.palette.deskInk,
                    if (p.iconOutline) p.theme.glow else 0, p.iconStripBackground,
                )
                val img = icons.render("sheet:${app.key.encode()}", d, spec)
                canvas.drawBitmap(img.asAndroidBitmap(), (col * side).toFloat(), (row * side).toFloat(), null)
            }
            java.io.File(getApplication<Application>().cacheDir, "iconsheet.png").outputStream().use {
                sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            Log.i(TAG, "icon sheet written")
        }
    }

    override fun onCleared() {
        repo.close()
    }

    private companion object {
        const val TAG = "HyperBit"
    }
}
