package com.reproductor.kiosk

/** Night window in minutes of the day; it may cross midnight. Equal start and end disables it. */
data class NightSchedule(val startMinute: Int, val endMinute: Int) {
    fun isNight(minuteOfDay: Int): Boolean = when {
        startMinute == endMinute -> false
        startMinute < endMinute -> minuteOfDay in startMinute until endMinute
        else -> minuteOfDay >= startMinute || minuteOfDay < endMinute
    }

    companion object {
        private val pattern = Regex("""(\d{1,2}):(\d{2})""")

        /** "HH:MM" to minutes of the day, or null when it is not a valid time. */
        fun parseMinutes(text: String): Int? {
            val match = pattern.matchEntire(text.trim()) ?: return null
            val (h, m) = match.destructured.toList().map { it.toInt() }
            return if (h < 24 && m < 60) h * 60 + m else null
        }

        fun formatMinutes(minutes: Int): String = ClockFormat.time(minutes / 60, minutes % 60)
    }
}
