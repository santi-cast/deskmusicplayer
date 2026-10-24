package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationLogosTest {
    @Test
    fun frequencyComesFromTheRadioTitle() {
        assertEquals("93.9", MediaFormat.radioFrequency(Apps.RADIO, "93.9"))
        assertEquals("102.7", MediaFormat.radioFrequency(Apps.RADIO, "102.7"))
        assertNull(MediaFormat.radioFrequency(Apps.RADIO, "Los 40"))
        assertNull(MediaFormat.radioFrequency(Apps.SPOTIFY, "93.9"))
    }

    @Test
    fun logoFilesAreNamedAfterTheFrequency() {
        assertEquals(
            listOf("93.9.png", "93.9.jpg", "93.9.jpeg", "93.9.webp"),
            StationLogos.fileNames("93.9"),
        )
    }

    @Test
    fun typedFrequenciesAreStoredAsTheRadioPublishesThem() {
        assertEquals("93.9", StationLogos.normalize("93,9"))
        assertEquals("98.0", StationLogos.normalize(" 98 "))
        assertEquals("100.7", StationLogos.normalize("100.70"))
        assertEquals("87.5", StationLogos.normalize("87.5"))
        assertEquals("108.0", StationLogos.normalize("108"))
    }

    @Test
    fun frequenciesOutsideTheFmBandOrNotNumbersAreRejected() {
        listOf("87.4", "108.1", "", "abc", "93.9.1", "-95").forEach { assertNull(it, StationLogos.normalize(it)) }
    }
}
