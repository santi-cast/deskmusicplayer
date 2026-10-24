package com.reproductor.kiosk

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Follows the media session of whichever app is playing (Spotify, Tempus, Radio). */
class NowPlaying(context: Context, private val onChange: () -> Unit) {
    data class Track(
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val durationMs: Long,
        val playing: Boolean,
        val live: Boolean,
    )

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val stationLogos = StationLogos(context)
    private val listener = ComponentName(context, MediaListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = onChange()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            pickController()
            onChange()
        }
        override fun onSessionDestroyed() {
            pickController()
            onChange()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener {
        pickController()
        onChange()
    }

    /** False when the notification listener permission has not been granted. */
    fun start(): Boolean = try {
        manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
        pickController()
        true
    } catch (e: SecurityException) {
        false
    }

    fun stop() {
        manager.removeOnActiveSessionsChangedListener(sessionsListener)
        controller?.unregisterCallback(callback)
        controller = null
    }

    val track: Track?
        get() {
            val c = controller ?: return null
            val m = c.metadata ?: return null
            val title = m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
            val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
            val durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION)
            return Track(
                title = MediaFormat.title(c.packageName, title),
                artist = MediaFormat.artist(c.packageName, artist),
                art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: MediaFormat.radioFrequency(c.packageName, title)?.let(stationLogos::logo),
                durationMs = durationMs,
                playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                live = MediaFormat.isLive(c.playbackState?.position ?: -1, durationMs),
            )
        }

    /** Frequency the FM radio is tuned to, if it is the active session ("93.9"). */
    val radioFrequency: String?
        get() {
            val c = controller ?: return null
            val title = c.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
            return MediaFormat.radioFrequency(c.packageName, title)
        }

    /** Current position, extrapolated from the last update while playing. */
    val positionMs: Long
        get() {
            val s = controller?.playbackState ?: return 0
            if (s.state != PlaybackState.STATE_PLAYING) return s.position
            val elapsed = SystemClock.elapsedRealtime() - s.lastPositionUpdateTime
            return s.position + (elapsed * s.playbackSpeed).toLong()
        }

    fun previous() = controller?.transportControls?.skipToPrevious()
    fun next() = controller?.transportControls?.skipToNext()
    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    private fun pickController() {
        val sessions = try {
            manager.getActiveSessions(listener)
        } catch (e: SecurityException) {
            emptyList()
        }
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        if (best?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = best
        best?.registerCallback(callback, handler)
    }
}
