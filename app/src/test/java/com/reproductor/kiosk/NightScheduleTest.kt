package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightScheduleTest {
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    @Test
    fun scheduleAcrossMidnight() {
        val s = NightSchedule(startMinute = m(23), endMinute = m(7))
        assertTrue(s.isNight(m(23)))
        assertTrue(s.isNight(m(2, 30)))
        assertTrue(s.isNight(m(6, 59)))
        assertFalse(s.isNight(m(7)))
        assertFalse(s.isNight(m(22, 59)))
        assertFalse(s.isNight(m(12)))
    }

    @Test
    fun scheduleWithinOneDay() {
        val s = NightSchedule(startMinute = m(1), endMinute = m(6))
        assertTrue(s.isNight(m(1)))
        assertTrue(s.isNight(m(5, 59)))
        assertFalse(s.isNight(m(6)))
        assertFalse(s.isNight(m(0, 59)))
    }

    @Test
    fun equalStartAndEndMeansNeverNight() {
        val s = NightSchedule(startMinute = m(7), endMinute = m(7))
        assertFalse(s.isNight(m(7)))
        assertFalse(s.isNight(m(0)))
    }

    @Test
    fun parsesAndFormatsTimeOfDay() {
        assertEquals(m(23), NightSchedule.parseMinutes("23:00"))
        assertEquals(m(7, 5), NightSchedule.parseMinutes("7:05"))
        assertEquals(0, NightSchedule.parseMinutes(" 00:00 "))
        assertEquals("07:05", NightSchedule.formatMinutes(m(7, 5)))
        assertEquals("23:00", NightSchedule.formatMinutes(m(23)))
    }

    @Test
    fun rejectsInvalidTimeOfDay() {
        listOf("24:00", "12:60", "7", "aa:bb", "", "-1:00", "12:5").forEach {
            assertNull(it, NightSchedule.parseMinutes(it))
        }
    }
}
