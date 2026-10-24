package com.reproductor.kiosk

/** An app shown on the home screen, with the name the user gave it. */
data class HomeApp(val packageName: String, val label: String)

/**
 * The apps on the home screen, in the user's order: the first one is the large
 * featured button and the rest go to the grid. Only these apps (and this one)
 * are allowed while the kiosk lock is on. Kept free of Android types for tests.
 */
data class AppSelection(val apps: List<HomeApp>) {
    val featured: HomeApp? get() = apps.firstOrNull()
    val grid: List<HomeApp> get() = apps.drop(1)
    val canAdd: Boolean get() = apps.size < MAX

    fun contains(packageName: String) = apps.any { it.packageName == packageName }

    fun add(packageName: String, label: String): AppSelection =
        if (!canAdd || contains(packageName)) this
        else AppSelection(apps + HomeApp(packageName, cleanLabel(label).ifEmpty { packageName }))

    fun remove(packageName: String) = AppSelection(apps.filter { it.packageName != packageName })

    fun moveUp(packageName: String): AppSelection = swap(indexOf(packageName), indexOf(packageName) - 1)

    fun moveDown(packageName: String): AppSelection = swap(indexOf(packageName), indexOf(packageName) + 1)

    fun rename(packageName: String, label: String): AppSelection {
        val clean = cleanLabel(label)
        if (clean.isEmpty() || !contains(packageName)) return this
        return AppSelection(apps.map { if (it.packageName == packageName) it.copy(label = clean) else it })
    }

    /** [tools] are allowed without a home button: the system installer and the app store. */
    fun lockTaskPackages(ownPackage: String, tools: List<String> = emptyList()): List<String> =
        (listOf(ownPackage) + tools + apps.map { it.packageName }).distinct()

    /** One "package<TAB>label" line per app; labels never contain tabs or line breaks. */
    fun encode(): String = apps.joinToString("\n") { "${it.packageName}\t${it.label}" }

    private fun indexOf(packageName: String) = apps.indexOfFirst { it.packageName == packageName }

    private fun swap(from: Int, to: Int): AppSelection {
        if (from < 0 || to !in apps.indices) return this
        val list = apps.toMutableList()
        list[from] = apps[to]
        list[to] = apps[from]
        return AppSelection(list)
    }

    companion object {
        /** One featured app plus a 2x2 grid. */
        const val MAX = 5
        private const val MAX_LABEL = 24

        val DEFAULT = AppSelection(
            listOf(
                HomeApp(Apps.SPOTIFY, "Spotify"),
                HomeApp(Apps.TEMPUS, "Servidor"),
                HomeApp(Apps.RADIO, "Radio FM"),
                HomeApp(Apps.MUSIC, "Música"),
                HomeApp(Apps.PODCASTS, "Podcasts"),
            ),
        )

        /**
         * Null means nothing was ever saved: then the default apps that are installed
         * (not every phone has an FM radio). Broken or repeated lines are skipped.
         */
        fun decode(text: String?, isInstalled: (String) -> Boolean = { true }): AppSelection {
            if (text == null) return AppSelection(DEFAULT.apps.filter { isInstalled(it.packageName) })
            val apps = text.lines()
                .mapNotNull { line ->
                    val parts = line.split('\t', limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank()) HomeApp(parts[0], cleanLabel(parts[1])) else null
                }
                .distinctBy { it.packageName }
                .take(MAX)
            return AppSelection(apps)
        }

        /**
         * Apps the user may put on the home screen: not this app, not Android Settings (it would
         * break the lock), not the stores (they open from Settings → Apps) and not Google Play
         * (no Google account on this old phone).
         */
        fun isOfferable(packageName: String, ownPackage: String) =
            packageName != ownPackage && packageName != Stores.ANDROID_SETTINGS &&
                !Stores.isStore(packageName) && packageName != Stores.GOOGLE_PLAY

        /** Never this app, a store (it installs the others) or apps that came with the phone. */
        fun canUninstall(packageName: String, ownPackage: String, isSystemApp: Boolean) =
            !isSystemApp && packageName != ownPackage && !Stores.isStore(packageName)

        private fun cleanLabel(label: String) =
            label.replace(Regex("""\s+"""), " ").trim().take(MAX_LABEL).trim()
    }
}
