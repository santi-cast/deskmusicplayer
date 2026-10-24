package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsValidationTest {
    private fun parse(
        idle: String = "60",
        brightness: String = "1",
        start: String = "23:00",
        end: String = "07:00",
    ) = SettingsValidation.parse(idle, brightness, start, end)

    @Test
    fun validFormIsParsed() {
        assertEquals(SettingsValues(60, 0.01f, 23 * 60, 7 * 60), parse())
        assertEquals(
            SettingsValues(5, 1f, 0, 23 * 60 + 59),
            parse(idle = " 5 ", brightness = "100", start = "0:00", end = "23:59"),
        )
    }

    @Test
    fun idleBelowFiveSecondsIsRejected() {
        assertNull(parse(idle = "4"))
        assertNull(parse(idle = ""))
        assertNull(parse(idle = "abc"))
    }

    @Test
    fun brightnessOutsideOneToHundredIsRejected() {
        assertNull(parse(brightness = "0"))
        assertNull(parse(brightness = "101"))
        assertNull(parse(brightness = ""))
    }

    @Test
    fun invalidTimesAreRejected() {
        assertNull(parse(start = "24:00"))
        assertNull(parse(end = "7"))
        assertNull(parse(end = ""))
    }

    @Test
    fun brightnessPercentRoundsAndClamps() {
        assertEquals(1, SettingsValidation.brightnessPercent(0.01f))
        assertEquals(35, SettingsValidation.brightnessPercent(0.349f))
        assertEquals(100, SettingsValidation.brightnessPercent(1f))
        assertEquals(1, SettingsValidation.brightnessPercent(0f))
    }
}
