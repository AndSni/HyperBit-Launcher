package com.asnidev.hyperbit

import com.asnidev.hyperbit.data.AppKey
import com.asnidev.hyperbit.data.HPos
import com.asnidev.hyperbit.data.LabelMode
import com.asnidev.hyperbit.data.VPos
import com.asnidev.hyperbit.data.IconStyle
import com.asnidev.hyperbit.data.Prefs
import com.asnidev.hyperbit.data.Presets
import com.asnidev.hyperbit.data.PrefsJson
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsJsonTest {
    private val a = AppKey("com.a", "com.a.Main", 0)
    private val b = AppKey("com.b", "com.b.Main", 10)

    @Test
    fun roundTrip() {
        val p = Prefs(
            home = mapOf(0 to a, 17 to b),
            taskbar = listOf(b, null, a),
            columns = 4, rows = 6, iconScale = 150, labels = LabelMode.BESIDE,
            windowWidth = 70, windowHeight = 55, windowH = HPos.RIGHT, windowV = VPos.BOTTOM,
            iconStyle = IconStyle.HOLOGRAM,
            iconOverrides = mapOf(a to IconStyle.ORIGINAL),
            renames = mapOf(b to "Bee"),
            hidden = setOf(b),
            theme = Presets.all[4].theme,
            homeTitle = "C:\\",
            seeded = true,
        )
        assertEquals(p, PrefsJson.read(PrefsJson.write(p)))
    }

    @Test
    fun oldShowLabelsFalseMeansHidden() {
        assertEquals(LabelMode.HIDDEN, PrefsJson.read("""{"showLabels":false}""").labels)
        assertEquals(LabelMode.UNDER, PrefsJson.read("""{"showLabels":true}""").labels)
    }

    @Test
    fun garbageGivesDefaults() {
        assertEquals(Prefs(), PrefsJson.read("{not json"))
        assertEquals(Prefs(), PrefsJson.read(null))
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val p = PrefsJson.read("""{"columns":99,"rows":0,"iconRes":7,"taskbarCount":9,"windowWidth":5,"windowHeight":500,"iconStyle":"NOPE"}""")
        assertEquals(6, p.columns)
        assertEquals(2, p.rows)
        assertEquals(32, p.iconRes)
        assertEquals(3, p.taskbarCount)
        assertEquals(40, p.windowWidth)
        assertEquals(100, p.windowHeight)
        assertEquals(IconStyle.NEON, p.iconStyle)
    }

    @Test
    fun swapAndCleanUp() {
        val p = Prefs(home = mapOf(0 to a, 5 to b))
        assertEquals(mapOf(3 to a, 5 to b), p.swap(0, 3).home)
        assertEquals(mapOf(5 to a, 0 to b), p.swap(0, 5).home)
        assertEquals(mapOf(0 to a, 1 to b), p.cleanUp().home)
        assertEquals(1, p.firstFreeSlot())
        assertEquals(1, Prefs(home = mapOf(15 to a)).pages)
        assertEquals(2, Prefs(home = mapOf(15 to a)).copy(rows = 7).pages)
    }
}
