package com.reproductor.kiosk

/** Rules of the setup assistant, kept free of Android types for unit tests. */
object SetupCheck {
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000

    /** The app decides when the clock shows, so the screen timeout must be (practically) never. */
    fun isScreenAlwaysOn(screenOffTimeoutMs: Int) = screenOffTimeoutMs >= ONE_DAY_MS

    /** Steps without which the home screen does not work as intended: the clock and now playing. */
    fun pendingEssentials(overlay: Boolean, notifications: Boolean) = listOf(overlay, notifications).count { !it }
}
