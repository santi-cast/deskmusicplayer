package com.reproductor.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PixelFormat
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Full-screen clock drawn as an overlay window on top of whatever app is open,
 * so closing it simply reveals that app again. Media controls and the power icon
 * work; vertical swipes change brightness (left half) or volume (right half), like
 * a video player; any other tap calls [onDismiss].
 */
class ClockOverlay(context: Context, private val prefs: Prefs, private val onDismiss: () -> Unit) {
    private val ctx = ContextThemeWrapper(context, R.style.KioskTheme)
    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val root: View = LayoutInflater.from(ctx).inflate(R.layout.screensaver, null)
    private val content: View = root.findViewById(R.id.content)
    private val time: TextView = root.findViewById(R.id.time)
    private val date: TextView = root.findViewById(R.id.date)
    private val battery: TextView = root.findViewById(R.id.battery)
    private val art: ImageView = root.findViewById(R.id.art)
    private val nowPlayingBox: LinearLayout = root.findViewById(R.id.now_playing)
    private val title: TextView = root.findViewById(R.id.title)
    private val artist: TextView = root.findViewById(R.id.artist)
    private val progress: ProgressBar = root.findViewById(R.id.progress)
    private val progressText: TextView = root.findViewById(R.id.progress_text)
    private val controls: List<ImageView> =
        listOf(R.id.previous, R.id.play_pause, R.id.next).map { root.findViewById(it) }
    private val nowPlaying = NowPlaying(context) { handler.post { updateTrack() } }
    private val levelBox: View = root.findViewById(R.id.level_box)
    private val levelText: TextView = root.findViewById(R.id.level_text)
    private val levelBar: ProgressBar = root.findViewById(R.id.level_bar)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val hideLevel = Runnable { levelBox.visibility = View.GONE }
    private var params: WindowManager.LayoutParams? = null

    // Current swipe, if any.
    private var downX = 0f
    private var downY = 0f
    private var swiping = false
    private var brightnessSwipe = false
    private var startLevel = 0f
    private var wantedVolume = 0

    var isShowing = false
        private set
    private var lastMinute = -1
    private var night: Boolean? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (level >= 0) battery.text = ClockFormat.battery(level * 100 / scale, plugged)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            val now = LocalTime.now()
            val minute = now.hour * 60 + now.minute
            if (minute != lastMinute) {
                lastMinute = minute
                time.text = ClockFormat.time(now.hour, now.minute)
                date.text = ClockFormat.date(LocalDate.now())
                applyNight(prefs.nightSchedule.isNight(minute))
                drift()
            }
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        root.setOnTouchListener { _, event -> onRootTouch(event) }
        root.findViewById<View>(R.id.power).setOnClickListener {
            onDismiss()
            PowerMenuService.open(ctx)
        }
        // The window takes focus so the system bars can hide; Back then closes the clock.
        root.isFocusableInTouchMode = true
        root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) onDismiss()
            keyCode == KeyEvent.KEYCODE_BACK
        }
        controls[0].setOnClickListener { nowPlaying.previous() }
        controls[1].setOnClickListener { nowPlaying.playPause() }
        controls[2].setOnClickListener { nowPlaying.next() }
        root.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or_, ob ->
            if (r - l != or_ - ol || b - t != ob - ot) {
                resize(r - l, b - t)
                drift()
            }
        }
    }

    fun show() {
        if (isShowing) return
        isShowing = true
        lastMinute = -1
        night = null
        @Suppress("DEPRECATION")
        root.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE,
        ).apply { screenBrightness = prefs.clockBrightness }
        params = layoutParams
        levelBox.visibility = View.GONE
        wm.addView(root, layoutParams)
        ctx.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        nowPlaying.start()
        updateTrack()
        tick.run()
    }

    fun hide() {
        if (!isShowing) return
        isShowing = false
        handler.removeCallbacks(tick)
        handler.removeCallbacks(hideLevel)
        nowPlaying.stop()
        ctx.unregisterReceiver(batteryReceiver)
        wm.removeView(root)
    }

    /** Text sizes follow the short side of the screen, like vw units in the old page. */
    private fun resize(width: Int, height: Int) {
        val base = minOf(width, height).toFloat()
        fun TextView.px(fraction: Float) = setTextSize(TypedValue.COMPLEX_UNIT_PX, base * fraction)
        time.px(0.26f)
        date.px(0.06f)
        battery.px(0.05f)
        title.px(0.055f)
        artist.px(0.045f)
        progressText.px(0.04f)
        val controlSize = (base * 0.16f).toInt()
        controls.forEach { it.layoutParams = it.layoutParams.apply { this.width = controlSize; this.height = controlSize } }
        nowPlayingBox.layoutParams = nowPlayingBox.layoutParams.apply { this.width = (base * 0.8f).toInt() }
    }

    /** Random offset, as far as the content still fits on screen (±8 % wide, ±15 % high at most). */
    private fun drift() {
        val w = root.width
        val h = root.height
        if (w == 0 || h == 0) return
        val maxX = minOf(w * 0.08f, (w - content.width) / 2f).coerceAtLeast(0f)
        val maxY = minOf(h * 0.15f, (h - content.height) / 2f).coerceAtLeast(0f)
        content.translationX = Random.nextFloat() * 2 * maxX - maxX
        content.translationY = Random.nextFloat() * 2 * maxY - maxY
    }

    private fun applyNight(isNight: Boolean) {
        if (night == isNight) return
        night = isNight
        val (primary, secondary, faint) =
            if (isNight) Triple(0xFF7A1A1A.toInt(), 0xFF5A1414.toInt(), 0xFF401010.toInt())
            else Triple(0xFF8A8A8A.toInt(), 0xFF6A6A6A.toInt(), 0xFF4A4A4A.toInt())
        time.setTextColor(primary)
        title.setTextColor(primary)
        date.setTextColor(secondary)
        artist.setTextColor(secondary)
        controls.forEach { it.imageTintList = ColorStateList.valueOf(secondary) }
        battery.setTextColor(faint)
        // The ⚡ is a colour emoji that ignores the text colour; tint the whole line instead.
        battery.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply {
            colorFilter = PorterDuffColorFilter(faint, PorterDuff.Mode.SRC_IN)
        })
        progressText.setTextColor(faint)
        progress.progressTintList = ColorStateList.valueOf(secondary)
        art.alpha = if (isNight) 0.10f else 0.25f
        if (isNight) art.setColorFilter(Color.argb(160, 120, 0, 0)) else art.clearColorFilter()
    }

    private fun updateTrack() {
        if (!isShowing) return
        val track = nowPlaying.track
        nowPlayingBox.visibility = if (track == null) View.GONE else View.VISIBLE
        art.visibility = if (track?.art == null) View.GONE else View.VISIBLE
        if (track == null) return
        title.text = track.title
        artist.text = track.artist
        art.setImageBitmap(track.art)
        controls[1].setImageResource(
            if (track.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        updateProgress()
    }

    private fun updateProgress() {
        val track = nowPlaying.track ?: return
        val liveVisibility = if (track.live) View.GONE else View.VISIBLE
        progress.visibility = liveVisibility
        progressText.visibility = liveVisibility
        if (track.live) return
        val position = nowPlaying.positionMs
        progress.progress = (MediaFormat.fraction(position, track.durationMs) * 1000).toInt()
        progressText.text = MediaFormat.progress(position, track.durationMs)
    }

    /** A tap closes the clock; a vertical swipe changes brightness or volume instead. */
    private fun onRootTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                swiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - downY
                if (!swiping && abs(dy) > touchSlop && abs(dy) > abs(event.x - downX)) {
                    swiping = true
                    brightnessSwipe = SwipeLevel.isBrightnessSide(downX, root.width.toFloat())
                    startLevel = if (brightnessSwipe) prefs.clockBrightness else volumeLevel()
                    handler.removeCallbacks(hideLevel)
                    levelBox.visibility = View.VISIBLE
                }
                if (swiping) {
                    val min = if (brightnessSwipe) MIN_BRIGHTNESS else 0f
                    setLevel(SwipeLevel.adjust(startLevel, dy, root.height.toFloat(), min, 1f))
                }
            }
            MotionEvent.ACTION_UP -> if (swiping) endSwipe() else onDismiss()
            MotionEvent.ACTION_CANCEL -> if (swiping) endSwipe()
        }
        return true
    }

    private fun endSwipe() {
        handler.postDelayed(hideLevel, 1200)
        // Above the safe headphone level Android only raises the volume after its own warning.
        if (!brightnessSwipe && wantedVolume > audio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, wantedVolume, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun volumeLevel(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    private fun setLevel(level: Float) {
        if (brightnessSwipe) {
            prefs.clockBrightness = level
            params?.let {
                it.screenBrightness = level
                wm.updateViewLayout(root, it)
            }
            showLevel(R.string.brightness_level, level)
        } else {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            wantedVolume = (level * max).roundToInt()
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, wantedVolume, 0)
            showLevel(R.string.volume_level, volumeLevel())
        }
    }

    private fun showLevel(label: Int, level: Float) {
        levelText.text = ctx.getString(label, SwipeLevel.percent(level))
        levelBar.progress = SwipeLevel.percent(level)
    }

    private companion object {
        const val MIN_BRIGHTNESS = 0.01f
    }
}
