package com.reproductor.kiosk

import java.time.DayOfWeek
import java.time.LocalDate

/** Text shown by the clock screensaver, matching the old reloj.html page. */
object ClockFormat {
    private val days = mapOf(
        DayOfWeek.MONDAY to "lunes",
        DayOfWeek.TUESDAY to "martes",
        DayOfWeek.WEDNESDAY to "miércoles",
        DayOfWeek.THURSDAY to "jueves",
        DayOfWeek.FRIDAY to "viernes",
        DayOfWeek.SATURDAY to "sábado",
        DayOfWeek.SUNDAY to "domingo",
    )
    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun time(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    fun date(date: LocalDate): String =
        "${days.getValue(date.dayOfWeek)}, ${date.dayOfMonth} de ${months[date.monthValue - 1]}"

    fun battery(percent: Int, charging: Boolean): String =
        (if (charging) "⚡ Cargando · " else "Batería · ") + "$percent %"
}
