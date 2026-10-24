package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupCheckTest {
    @Test
    fun screenCountsAsAlwaysOnFromOneDayOfTimeout() {
        assertTrue(SetupCheck.isScreenAlwaysOn(Int.MAX_VALUE))
        assertTrue(SetupCheck.isScreenAlwaysOn(24 * 60 * 60 * 1000))
        assertFalse(SetupCheck.isScreenAlwaysOn(30_000))
    }

    @Test
    fun pendingCountsOnlyTheStepsTheHomeScreenNeeds() {
        assertEquals(0, SetupCheck.pendingEssentials(overlay = true, notifications = true))
        assertEquals(2, SetupCheck.pendingEssentials(overlay = false, notifications = false))
        assertEquals(1, SetupCheck.pendingEssentials(overlay = false, notifications = true))
    }
}
