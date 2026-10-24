package com.reproductor.kiosk

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    @Test
    fun timeIsTwentyFourHourWithLeadingZeros() {
        assertEquals("07:05", ClockFormat.time(7, 5))
        assertEquals("23:59", ClockFormat.time(23, 59))
        assertEquals("00:00", ClockFormat.time(0, 0))
    }

    @Test
    fun dateUsesSpanishDayAndMonthNames() {
        assertEquals("sábado, 24 de octubre", ClockFormat.date(LocalDate.of(2026, 10, 24)))
        assertEquals("domingo, 1 de marzo", ClockFormat.date(LocalDate.of(2026, 3, 1)))
        assertEquals("miércoles, 31 de diciembre", ClockFormat.date(LocalDate.of(2025, 12, 31)))
    }

    @Test
    fun batteryShowsChargingOrLevel() {
        assertEquals("⚡ Cargando · 87 %", ClockFormat.battery(87, charging = true))
        assertEquals("Batería · 5 %", ClockFormat.battery(5, charging = false))
        assertEquals("Batería · 100 %", ClockFormat.battery(100, charging = false))
    }
}
