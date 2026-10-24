package com.reproductor.kiosk

/** An app store the player can open from Settings → Apps. */
data class Store(val packageName: String, val label: String)

/**
 * App stores that work without a Google account, in order of preference. Google Play is left
 * out on purpose: the player is meant for an old phone with no Google account signed in.
 */
object Stores {
    const val GOOGLE_PLAY = "com.android.vending"

    /** Official page to install Aurora Store from the phone when no store is installed. */
    const val AURORA_DOWNLOAD_PAGE = "https://f-droid.org/packages/com.aurora.store/"

    /** Android Settings: hosts the "install unknown apps" switch the browser needs once. */
    const val ANDROID_SETTINGS = "com.android.settings"

    val KNOWN = listOf(
        Store(Apps.STORE, "Aurora Store"),
        Store("org.fdroid.fdroid", "F-Droid"),
        Store("com.looker.droidify", "Droid-ify"),
    )

    fun isStore(packageName: String) = KNOWN.any { it.packageName == packageName }

    fun installed(isInstalled: (String) -> Boolean) = KNOWN.filter { isInstalled(it.packageName) }

    /** The chosen store while it is installed; otherwise the first installed one, if any. */
    fun resolve(chosen: String?, isInstalled: (String) -> Boolean): Store? =
        installed(isInstalled).let { list -> list.firstOrNull { it.packageName == chosen } ?: list.firstOrNull() }
}
