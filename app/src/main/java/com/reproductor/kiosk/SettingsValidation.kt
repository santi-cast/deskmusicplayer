package com.reproductor.kiosk

import kotlin.math.roundToInt

/** Validated settings form. */
data class SettingsValues(
    val idleSeconds: Int,
    val clockBrightness: Float,
    val nightStartMinute: Int,
    val nightEndMinute: Int,
)

/** Validation of the settings form, kept free of Android types for unit tests. */
object SettingsValidation {
    /** All fields valid, or null when any of them is invalid. */
    fun parse(idle: String, brightnessPercent: String, nightStart: String, nightEnd: String): SettingsValues? {
        val seconds = idle.trim().toIntOrNull()?.takeIf { it >= 5 } ?: return null
        val percent = brightnessPercent.trim().toIntOrNull()?.takeIf { it in 1..100 } ?: return null
        val start = NightSchedule.parseMinutes(nightStart) ?: return null
        val end = NightSchedule.parseMinutes(nightEnd) ?: return null
        return SettingsValues(seconds, percent / 100f, start, end)
    }

    /** Stored brightness 0.01..1 as the 1..100 percent shown in the form. */
    fun brightnessPercent(brightness: Float): Int = (brightness * 100).roundToInt().coerceIn(1, 100)
}
