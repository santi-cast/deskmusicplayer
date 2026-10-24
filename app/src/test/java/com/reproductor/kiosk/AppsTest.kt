package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsTest {
    @Test
    fun withTheDefaultSelectionLockTaskStillAllowsTodaysApps() {
        assertEquals(
            listOf(
                "com.reproductor.kiosk",
                "com.spotify.music",
                "com.eddyizm.degoogled.tempus",
                "com.motorola.fmplayer",
                "org.oxycblt.auxio",
                "de.danoeh.antennapod",
            ),
            AppSelection.DEFAULT.lockTaskPackages("com.reproductor.kiosk"),
        )
    }
}
