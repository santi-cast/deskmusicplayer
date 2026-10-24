package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeLevelTest {
    @Test
    fun swipingUpRaisesAndDownLowers() {
        // A swipe over 60 % of the height covers the whole range.
        assertEquals(0.5f + 0.5f, SwipeLevel.adjust(0.5f, dy = -300f, height = 1000f, min = 0f, max = 1f), 0.001f)
        assertEquals(0.25f, SwipeLevel.adjust(0.5f, dy = 150f, height = 1000f, min = 0f, max = 1f), 0.001f)
    }

    @Test
    fun levelIsClampedToItsRange() {
        assertEquals(1f, SwipeLevel.adjust(0.9f, dy = -1000f, height = 1000f, min = 0.01f, max = 1f), 0f)
        assertEquals(0.01f, SwipeLevel.adjust(0.1f, dy = 1000f, height = 1000f, min = 0.01f, max = 1f), 0f)
    }

    @Test
    fun leftHalfIsBrightnessRightHalfIsVolume() {
        assertTrue(SwipeLevel.isBrightnessSide(x = 100f, width = 1000f))
        assertFalse(SwipeLevel.isBrightnessSide(x = 600f, width = 1000f))
    }

    @Test
    fun percentIsRounded() {
        assertEquals(35, SwipeLevel.percent(0.349f))
        assertEquals(53, SwipeLevel.percent(8 / 15f))
    }
}
