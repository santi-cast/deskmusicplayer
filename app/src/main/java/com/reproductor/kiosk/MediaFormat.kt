package com.reproductor.kiosk

/** Formatting of the playback position shown under the now-playing track. */
object MediaFormat {
    fun progress(positionMs: Long, durationMs: Long): String =
        if (durationMs > 0) "${duration(positionMs)} / ${duration(durationMs)}" else duration(positionMs)

    fun fraction(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Live broadcast (the FM radio reports position -1 and no duration): no progress to show. */
    fun isLive(positionMs: Long, durationMs: Long): Boolean = positionMs < 0 || durationMs <= 0

    private val frequency = Regex("""\d{2,3}(\.\d{1,2})?""")

    /** The FM radio publishes the frequency as title ("93.9"); null for anything else. */
    fun radioFrequency(packageName: String, title: String): String? =
        title.takeIf { packageName == Apps.RADIO && frequency.matches(it) }

    /** "93.9" from the FM radio → "93.9 MHz". */
    fun title(packageName: String, title: String): String =
        radioFrequency(packageName, title)?.let { "$it MHz" } ?: title

    /** The FM radio publishes no artist; name the source instead. */
    fun artist(packageName: String, artist: String): String =
        if (packageName == Apps.RADIO && artist.isEmpty()) "Radio FM" else artist

    private fun duration(ms: Long): String {
        val total = ms.coerceAtLeast(0) / 1000
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
