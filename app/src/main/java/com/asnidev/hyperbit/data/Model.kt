package com.asnidev.hyperbit.data

/**
 * One launchable activity in one user profile. The profile is stored as its
 * serial number because UserHandle itself isn't stable across reboots.
 */
data class AppKey(val pkg: String, val cls: String, val user: Long) {
    fun encode(): String = "$pkg/$cls#$user"

    companion object {
        fun decode(s: String): AppKey? {
            val slash = s.indexOf('/')
            val hash = s.lastIndexOf('#')
            if (slash <= 0 || hash <= slash) return null
            val user = s.substring(hash + 1).toLongOrNull() ?: return null
            return AppKey(s.substring(0, slash), s.substring(slash + 1, hash), user)
        }
    }
}

/** How an app icon is drawn. PIXEL alone is low resolution, at [Prefs.iconRes] pixels. */
enum class IconStyle(val label: String) {
    ORIGINAL("Original"),
    NEON("Neon duotone"),
    HOLOGRAM("Hologram"),
    WIREFRAME("Wireframe"),
    GLYPH("Glyph"),
    PIXEL("Neon pixel"),
}

/**
 * Window chrome; both keep the old layout (title bar, close and full boxes, info
 * line, scroll bars). BRACKET: chamfered corners with corner brackets. SCANLINE:
 * rounded, with a title bar of fine scan lines (the old pinstripes, refined).
 */
enum class WindowStyle(val label: String) { BRACKET("Bracket"), SCANLINE("Scanline") }

/** What the line under a window's title shows. */
enum class InfoBar(val label: String) {
    STORAGE("Storage"),
    CLOCK("Date and time"),
    BATTERY("Battery"),
    ITEMS("Item count"),
    NONE("Hidden"),
}

/** What the desktop behind the windows shows; drawn in [Palette.deskInk] over [Palette.desk]. */
enum class Backdrop(val label: String) {
    GRID("Horizon grid"),
    HEX("Hex lattice"),
    CIRCUIT("Circuit"),
    RAIN("Data rain"),
    DOTS("Dot matrix"),
    PLAIN("Plain glow"),
}

/**
 * The four colours, as 0xAARRGGBB: [ink] the neon of lines and text, [paper]
 * the dark glass windows are made of, [desk] the backdrop's base and [deskInk]
 * the accent (backdrop lines, highlights, the second icon tone).
 */
data class Palette(val ink: Int, val paper: Int, val desk: Int, val deskInk: Int)

/** Where app names go in the home window. BESIDE leaves icons the cell's full height. */
enum class LabelMode(val label: String) { UNDER("Under icons"), BESIDE("Beside icons"), HIDDEN("Hidden") }

/** Where the home window sits when it is smaller than the desktop. */
enum class HPos(val label: String) { LEFT("Left"), CENTER("Center"), RIGHT("Right") }
enum class VPos(val label: String) { TOP("Top"), MIDDLE("Middle"), BOTTOM("Bottom") }

/** Fonts bundled in res/font (plus the two system ones). */
enum class FontChoice(val label: String) {
    CHAKRA("Chakra Petch"),
    ORBITRON("Orbitron"),
    SHARE_TECH("Share Tech Mono"),
    VT323("Terminal"),
    SYSTEM_MONO("System mono"),
    SYSTEM("System sans"),
}

data class Theme(
    val palette: Palette,
    val backdrop: Backdrop,
    val window: WindowStyle,
    val font: FontChoice = FontChoice.CHAKRA,
    /** Text size in sp. */
    val textSize: Int = 15,
    /** Neon glow around lines, text and icons: 0 off to 3 strong. */
    val glow: Int = 2,
    /** Fine scan lines and a vignette over the whole screen. */
    val scanlines: Boolean = true,
)

data class Preset(val name: String, val theme: Theme)

private fun c(rgb: Long): Int = (0xFF000000 or rgb).toInt()

object Presets {
    val all = listOf(
        Preset("Synthwave", Theme(Palette(c(0x2DE2E6), c(0x0D0221), c(0x0D0221), c(0xFF3864)), Backdrop.GRID, WindowStyle.BRACKET)),
        Preset("Night city", Theme(Palette(c(0xFCEE0A), c(0x0A0A12), c(0x050508), c(0x00F0FF)), Backdrop.CIRCUIT, WindowStyle.BRACKET, FontChoice.CHAKRA)),
        Preset("Vapor", Theme(Palette(c(0xFF71CE), c(0x1A0B2E), c(0x120458), c(0x01CDFE)), Backdrop.GRID, WindowStyle.SCANLINE, FontChoice.ORBITRON, 13)),
        Preset("Ghost", Theme(Palette(c(0xE6F7FF), c(0x061018), c(0x02060A), c(0x4DD0E1)), Backdrop.HEX, WindowStyle.SCANLINE, FontChoice.SHARE_TECH)),
        Preset("Phosphor", Theme(Palette(c(0x39FF14), c(0x001505), c(0x000800), c(0x1FA82F)), Backdrop.RAIN, WindowStyle.SCANLINE, FontChoice.VT323, 19)),
        Preset("Amber", Theme(Palette(c(0xFFB000), c(0x120A00), c(0x080400), c(0xFF5F00)), Backdrop.DOTS, WindowStyle.BRACKET, FontChoice.SHARE_TECH)),
        Preset("Redline", Theme(Palette(c(0xFF2A2A), c(0x120404), c(0x060000), c(0xFFFFFF)), Backdrop.HEX, WindowStyle.BRACKET, FontChoice.ORBITRON, 13)),
        Preset("Ice", Theme(Palette(c(0xA0E9FF), c(0x0B1A2A), c(0x050B14), c(0x7B61FF)), Backdrop.PLAIN, WindowStyle.SCANLINE)),
    )
    val default = all.first()
}

/** A slot on the home window: [index] counts across pages, page = index / (columns × rows). */
data class Prefs(
    val home: Map<Int, AppKey> = emptyMap(),
    /** Taskbar buttons; null is an empty button. Always [MAX_TASKBAR] long. */
    val taskbar: List<AppKey?> = List(MAX_TASKBAR) { null },
    val taskbarCount: Int = 3,
    val taskbarLabels: Boolean = true,
    val columns: Int = 2,
    val rows: Int = 8,
    val labels: LabelMode = LabelMode.UNDER,
    /** Home window size, percent of the desktop area. */
    val windowWidth: Int = 100,
    val windowHeight: Int = 100,
    val windowH: HPos = HPos.CENTER,
    val windowV: VPos = VPos.MIDDLE,
    val iconStyle: IconStyle = IconStyle.NEON,
    /** Pixels per side for converted icons. */
    val iconRes: Int = 32,
    /**
     * Icon size, percent of 2.5 text lines. Icons never outgrow their grid
     * cell; past that they stay as big as fits.
     */
    val iconScale: Int = 100,
    /** A neon halo behind each icon. */
    val iconOutline: Boolean = true,
    /** Drop adaptive icons' background plate before converting, so only the glyph remains. */
    val iconStripBackground: Boolean = true,
    val iconOverrides: Map<AppKey, IconStyle> = emptyMap(),
    val renames: Map<AppKey, String> = emptyMap(),
    val hidden: Set<AppKey> = emptySet(),
    val theme: Theme = Presets.default.theme,
    /** Title of the home window; blank uses the style's default. */
    val homeTitle: String = "",
    val infoBar: InfoBar = InfoBar.STORAGE,
    val showMenuBar: Boolean = true,
    val menuClock: Boolean = true,
    val hideStatusBar: Boolean = false,
    val zoomRects: Boolean = true,
    /** Backdrops move (grid scrolls, rain falls); off saves battery. */
    val animate: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    /** First start fills the home window and taskbar with the phone's default apps once. */
    val seeded: Boolean = false,
) {
    val pageSize: Int get() = columns * rows

    /** Pages in use, at least one. */
    val pages: Int get() = ((home.keys.maxOrNull() ?: 0) / pageSize) + 1

    fun styleFor(key: AppKey): IconStyle = iconOverrides[key] ?: iconStyle

    fun firstFreeSlot(): Int {
        var i = 0
        while (i in home) i++
        return i
    }

    /** Moves the app at [from] to [to]; whatever was at [to] goes to [from]. */
    fun swap(from: Int, to: Int): Prefs {
        if (from == to) return this
        val a = home[from] ?: return this
        val b = home[to]
        val m = home.toMutableMap()
        m[to] = a
        if (b != null) m[from] = b else m.remove(from)
        return copy(home = m)
    }

    /** Closes the gaps: apps keep their order, packed from the first slot. */
    fun cleanUp(): Prefs = copy(home = home.toSortedMap().values.withIndex().associate { (i, k) -> i to k })

    fun withoutApp(key: AppKey): Prefs = copy(
        home = home.filterValues { it != key },
        taskbar = taskbar.map { if (it == key) null else it },
    )

    companion object {
        const val MAX_TASKBAR = 3
        val COLUMNS = 1..6
        val ROWS = 2..10
        val ICON_RES = listOf(16, 24, 32, 48)
        val TEXT_SIZES = 12..24
        val ICON_SCALE = 50..200
        const val ICON_SCALE_STEP = 25
        val WINDOW_WIDTH = 40..100
        val WINDOW_HEIGHT = 30..100
        const val WINDOW_STEP = 5
    }
}
