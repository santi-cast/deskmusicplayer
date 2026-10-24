package com.reproductor.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StoresTest {
    private fun installed(vararg packages: String): (String) -> Boolean = { it in packages }

    @Test
    fun knownStoresAreAuroraFDroidAndDroidifyButNotGooglePlay() {
        assertEquals(listOf("com.aurora.store", "org.fdroid.fdroid", "com.looker.droidify"), Stores.KNOWN.map { it.packageName })
        assertFalse(Stores.KNOWN.any { it.packageName == "com.android.vending" })
    }

    @Test
    fun theChosenStoreIsUsedWhileItIsInstalled() {
        assertEquals("org.fdroid.fdroid", Stores.resolve("org.fdroid.fdroid", installed("com.aurora.store", "org.fdroid.fdroid"))?.packageName)
    }

    @Test
    fun withoutAChoiceOrIfTheChosenOneIsGoneTheFirstInstalledIsUsed() {
        assertEquals("com.aurora.store", Stores.resolve(null, installed("org.fdroid.fdroid", "com.aurora.store"))?.packageName)
        assertEquals("com.looker.droidify", Stores.resolve("org.fdroid.fdroid", installed("com.looker.droidify"))?.packageName)
    }

    @Test
    fun noStoreInstalledMeansNone() {
        assertNull(Stores.resolve(null, installed("com.spotify.music")))
        assertEquals(emptyList<Store>(), Stores.installed(installed()))
    }

    @Test
    fun storesAndGooglePlayAreNeitherOfferedNorUninstallable() {
        listOf("org.fdroid.fdroid", "com.looker.droidify", "com.android.vending").forEach {
            assertFalse(it, AppSelection.isOfferable(it, ownPackage = "com.kiosk"))
        }
        assertFalse(AppSelection.canUninstall("org.fdroid.fdroid", ownPackage = "com.kiosk", isSystemApp = false))
    }

    @Test
    fun auroraIsDownloadedFromItsOfficialFDroidPage() {
        assertEquals("https://f-droid.org/packages/com.aurora.store/", Stores.AURORA_DOWNLOAD_PAGE)
    }
}
