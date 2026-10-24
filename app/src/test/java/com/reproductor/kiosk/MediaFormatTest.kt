package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFormatTest {
    @Test
    fun progressShowsPositionAndDuration() {
        assertEquals("1:02 / 3:45", MediaFormat.progress(62_751, 225_000))
        assertEquals("0:00 / 0:09", MediaFormat.progress(0, 9_999))
    }

    @Test
    fun longTracksShowHours() {
        assertEquals("1:00:05 / 2:03:00", MediaFormat.progress(3_605_000, 7_380_000))
    }

    @Test
    fun unknownDurationShowsOnlyPosition() {
        assertEquals("4:10", MediaFormat.progress(250_000, 0))
        assertEquals("4:10", MediaFormat.progress(250_000, -1))
    }

    @Test
    fun fractionIsClampedBetweenZeroAndOne() {
        assertEquals(0.5f, MediaFormat.fraction(50, 100), 0f)
        assertEquals(1f, MediaFormat.fraction(150, 100), 0f)
        assertEquals(0f, MediaFormat.fraction(-5, 100), 0f)
        assertEquals(0f, MediaFormat.fraction(50, 0), 0f)
    }

    @Test
    fun liveWhenPositionOrDurationIsUnknown() {
        assertEquals(true, MediaFormat.isLive(-1, 0))
        assertEquals(true, MediaFormat.isLive(5_000, 0))
        assertEquals(false, MediaFormat.isLive(62_751, 225_000))
    }

    @Test
    fun radioFrequencyGetsUnitAndSourceLabel() {
        assertEquals("93.9 MHz", MediaFormat.title(Apps.RADIO, "93.9"))
        assertEquals("105 MHz", MediaFormat.title(Apps.RADIO, "105"))
        assertEquals("Radio FM", MediaFormat.artist(Apps.RADIO, ""))
    }

    @Test
    fun otherTitlesAndArtistsAreKept() {
        assertEquals("Los 40", MediaFormat.title(Apps.RADIO, "Los 40"))
        assertEquals("93.9", MediaFormat.title(Apps.SPOTIFY, "93.9"))
        assertEquals("deadmau5", MediaFormat.artist(Apps.RADIO, "deadmau5"))
        assertEquals("", MediaFormat.artist(Apps.SPOTIFY, ""))
    }
}
