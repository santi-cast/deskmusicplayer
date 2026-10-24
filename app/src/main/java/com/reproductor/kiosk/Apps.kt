package com.reproductor.kiosk

import android.content.Context
import android.content.Intent

/** Known player apps: the default home screen and the FM radio, whose metadata is special. */
object Apps {
    const val SPOTIFY = "com.spotify.music"
    const val TEMPUS = "com.eddyizm.degoogled.tempus"
    const val RADIO = "com.motorola.fmplayer"
    const val MUSIC = "org.oxycblt.auxio"
    const val PODCASTS = "de.danoeh.antennapod"

    /** Aurora Store, the preferred store (see [Stores]). */
    const val STORE = "com.aurora.store"

    fun launchIntent(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
