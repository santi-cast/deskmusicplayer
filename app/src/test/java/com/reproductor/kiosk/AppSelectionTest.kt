package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSelectionTest {
    private val a = HomeApp("com.a", "A")
    private val b = HomeApp("com.b", "B")
    private val c = HomeApp("com.c", "C")

    @Test
    fun defaultIsTodaysFiveAppsWithTheirNames() {
        assertEquals(
            listOf(
                HomeApp("com.spotify.music", "Spotify"),
                HomeApp("com.eddyizm.degoogled.tempus", "Servidor"),
                HomeApp("com.motorola.fmplayer", "Radio FM"),
                HomeApp("org.oxycblt.auxio", "Música"),
                HomeApp("de.danoeh.antennapod", "Podcasts"),
            ),
            AppSelection.DEFAULT.apps,
        )
    }

    @Test
    fun withNothingSavedOnlyInstalledDefaultAppsAreShown() {
        val installed = setOf("com.spotify.music", "org.oxycblt.auxio")
        assertEquals(
            listOf("com.spotify.music", "org.oxycblt.auxio"),
            AppSelection.decode(null, isInstalled = { it in installed }).apps.map { it.packageName },
        )
    }

    @Test
    fun aSavedSelectionIsKeptEvenIfAnAppIsMissing() {
        val saved = "com.spotify.music\tSpotify\ncom.motorola.fmplayer\tRadio FM"
        assertEquals(2, AppSelection.decode(saved, isInstalled = { it == "com.spotify.music" }).apps.size)
    }

    @Test
    fun firstAppIsFeaturedAndTheRestGoToTheGrid() {
        val s = AppSelection(listOf(a, b, c))
        assertEquals(a, s.featured)
        assertEquals(listOf(b, c), s.grid)
        assertNull(AppSelection(emptyList()).featured)
    }

    @Test
    fun savedSelectionIsRestoredExactly() {
        val s = AppSelection(listOf(HomeApp("com.a", "Mi música"), b))
        assertEquals(s, AppSelection.decode(s.encode()))
    }

    @Test
    fun nothingSavedMeansTheDefaultButAnEmptySaveStaysEmpty() {
        assertEquals(AppSelection.DEFAULT, AppSelection.decode(null))
        assertEquals(emptyList<HomeApp>(), AppSelection.decode("").apps)
    }

    @Test
    fun brokenOrRepeatedSavedLinesAreSkippedAndTheLimitIsKept() {
        val text = "com.a\tA\nbroken line\ncom.a\tA again\n\tno package\ncom.b\tB\ncom.c\tC\ncom.d\tD\ncom.e\tE\ncom.f\tF"
        assertEquals(listOf("com.a", "com.b", "com.c", "com.d", "com.e"), AppSelection.decode(text).apps.map { it.packageName })
    }

    @Test
    fun addingStopsAtFiveApps() {
        var s = AppSelection(emptyList())
        listOf("1", "2", "3", "4", "5").forEach { s = s.add("com.$it", it) }
        assertEquals(5, s.apps.size)
        assertFalse(s.canAdd)
        assertSame(s, s.add("com.6", "6"))
    }

    @Test
    fun addingAnAppTwiceDoesNothing() {
        val s = AppSelection(listOf(a))
        assertSame(s, s.add("com.a", "Other"))
    }

    @Test
    fun removingAnAppTakesItOut() {
        assertEquals(listOf(a, c), AppSelection(listOf(a, b, c)).remove("com.b").apps)
    }

    @Test
    fun movingUpAndDownSwapsWithTheNeighbourAndStopsAtTheEnds() {
        val s = AppSelection(listOf(a, b, c))
        assertEquals(listOf(b, a, c), s.moveUp("com.b").apps)
        assertEquals(listOf(a, c, b), s.moveDown("com.b").apps)
        assertSame(s, s.moveUp("com.a"))
        assertSame(s, s.moveDown("com.c"))
    }

    @Test
    fun renamingTrimsCleansAndRejectsBlankNames() {
        val s = AppSelection(listOf(a))
        assertEquals("Mi música", s.rename("com.a", "  Mi\tmúsica\n ").apps[0].label)
        assertSame(s, s.rename("com.a", "   "))
        assertEquals(24, s.rename("com.a", "x".repeat(40)).apps[0].label.length)
    }

    @Test
    fun lockTaskAllowsThisAppAndTheSelectedOnes() {
        assertEquals(listOf("com.kiosk", "com.a", "com.b"), AppSelection(listOf(a, b)).lockTaskPackages("com.kiosk"))
    }

    @Test
    fun lockTaskAlsoAllowsTheStoreAndTheSystemInstallerSoAppsCanBeInstalled() {
        assertEquals(
            listOf("com.kiosk", "com.android.installer", "com.aurora.store", "com.a"),
            AppSelection(listOf(a)).lockTaskPackages("com.kiosk", tools = listOf("com.android.installer", "com.aurora.store")),
        )
    }

    @Test
    fun thisAppAndAndroidSettingsAreNotOffered() {
        assertFalse(AppSelection.isOfferable("com.kiosk", ownPackage = "com.kiosk"))
        assertFalse(AppSelection.isOfferable("com.android.settings", ownPackage = "com.kiosk"))
        assertTrue(AppSelection.isOfferable("de.danoeh.antennapod", ownPackage = "com.kiosk"))
    }

    @Test
    fun theStoreIsNotOfferedBecauseItOpensFromTheSettings() {
        assertFalse(AppSelection.isOfferable("com.aurora.store", ownPackage = "com.kiosk"))
    }

    @Test
    fun onlyUserAppsOtherThanThisAppAndTheStoreCanBeUninstalled() {
        assertTrue(AppSelection.canUninstall("org.oxycblt.auxio", ownPackage = "com.kiosk", isSystemApp = false))
        assertFalse(AppSelection.canUninstall("com.kiosk", ownPackage = "com.kiosk", isSystemApp = false))
        assertFalse(AppSelection.canUninstall("com.aurora.store", ownPackage = "com.kiosk", isSystemApp = false))
        assertFalse(AppSelection.canUninstall("com.motorola.fmplayer", ownPackage = "com.kiosk", isSystemApp = true))
    }
}
