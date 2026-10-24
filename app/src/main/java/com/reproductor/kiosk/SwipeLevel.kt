package com.reproductor.kiosk

import kotlin.math.roundToInt

/** Vertical swipes on the clock, like a video player: left half brightness, right half volume. */
object SwipeLevel {
    /** Share of the screen height a swipe needs to go through the whole range. */
    private const val FULL_RANGE_HEIGHT = 0.6f

    fun adjust(start: Float, dy: Float, height: Float, min: Float, max: Float): Float =
        (start - dy / (height * FULL_RANGE_HEIGHT) * (max - min)).coerceIn(min, max)

    fun isBrightnessSide(x: Float, width: Float): Boolean = x < width / 2

    fun percent(level: Float): Int = (level * 100).roundToInt()
}
