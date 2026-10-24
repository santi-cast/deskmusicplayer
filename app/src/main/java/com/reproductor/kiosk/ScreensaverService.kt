package com.reproductor.kiosk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Shows [ClockOverlay] after a period without touches anywhere on screen.
 *
 * Touches in other apps are detected with a 1x1 overlay that ignores touches itself
 * but sets FLAG_WATCH_OUTSIDE_TOUCH: Android 8.1 tells it about every touch-down on
 * the windows below it (without coordinates). Runs in the foreground so Android does
 * not stop it while Spotify or another app is open.
 */
class ScreensaverService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager
    private lateinit var power: PowerManager
    private lateinit var clock: ClockOverlay
    private var detector: View? = null

    private val showClock = Runnable {
        if (power.isInteractive) {
            Log.d(TAG, "idle: showing clock")
            clock.show()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacks(showClock)
                    clock.hide()
                }
                Intent.ACTION_SCREEN_ON -> resetIdle()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        wm = getSystemService(WindowManager::class.java)
        power = getSystemService(PowerManager::class.java)
        clock = ClockOverlay(this, prefs) {
            Log.d(TAG, "clock dismissed")
            clock.hide()
            resetIdle()
        }
        startForeground(NOTIFICATION_ID, notification())
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (detector == null && Settings.canDrawOverlays(this)) addDetector()
        if (intent?.action == ACTION_SHOW_NOW) handler.post(showClock) else resetIdle()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(screenReceiver)
        clock.hide()
        detector?.let { wm.removeView(it) }
        super.onDestroy()
    }

    private fun addDetector() {
        val view = View(this)
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                Log.d(TAG, "touch detected")
                resetIdle()
            }
            false
        }
        wm.addView(view, WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSPARENT,
        ).apply { gravity = Gravity.TOP or Gravity.START })
        detector = view
    }

    private fun resetIdle() {
        handler.removeCallbacks(showClock)
        if (!clock.isShowing) handler.postDelayed(showClock, prefs.idleSeconds * 1000L)
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.screensaver), NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.screensaver))
            .build()
    }

    companion object {
        private const val TAG = "KioskScreensaver"
        private const val CHANNEL = "screensaver"
        private const val NOTIFICATION_ID = 1

        /** Shows the clock at once; used for testing over ADB. */
        const val ACTION_SHOW_NOW = "com.reproductor.kiosk.SHOW_CLOCK"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, ScreensaverService::class.java))
    }
}
