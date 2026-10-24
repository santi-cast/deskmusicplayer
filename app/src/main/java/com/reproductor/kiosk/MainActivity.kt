package com.reproductor.kiosk

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalTime

/** Home screen: the user's apps (featured one on top, the rest in a grid), plus time and battery. */
class MainActivity : Activity() {
    private lateinit var headerTime: TextView
    private lateinit var headerBattery: TextView

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateHeader()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        headerTime = findViewById(R.id.header_time)
        headerBattery = findViewById(R.id.header_battery)
        findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.power).setOnClickListener { PowerMenuService.open(this) }
        findViewById<View>(R.id.setup_notice).setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
        ScreensaverService.start(this)
    }

    override fun onResume() {
        super.onResume()
        Kiosk.lockIfEnabled(this)
        buildHome()
        updateSetupNotice()
        registerReceiver(tickReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        })
        updateHeader()
    }

    override fun onPause() {
        unregisterReceiver(tickReceiver)
        super.onPause()
    }

    /** This activity is the launcher: Back has nowhere to go. */
    override fun onBackPressed() = Unit

    private fun updateSetupNotice() {
        val notice = findViewById<TextView>(R.id.setup_notice)
        val pending = SetupActivity.pendingEssentials(this)
        notice.visibility = if (pending > 0) View.VISIBLE else View.GONE
        notice.text = getString(R.string.setup_pending, pending)
    }

    /** Rebuilt on every resume, since the selection may have changed in the settings. */
    private fun buildHome() {
        val container = findViewById<LinearLayout>(R.id.home_apps)
        container.removeAllViews()
        val selection = Prefs(this).selection
        val featured = selection.featured
        if (featured == null) {
            container.addView(TextView(this).apply {
                setText(R.string.no_apps)
                setTextColor(0xFF8A8A8A.toInt())
                textSize = 20f
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            return
        }
        container.addView(appButton(featured, ICON_FEATURED_DP), buttonParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f))
        selection.grid.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { row.addView(appButton(it, ICON_GRID_DP), buttonParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)) }
            container.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    /** The style's layout margin does not apply to views built in code. */
    private fun buttonParams(width: Int, height: Int, weight: Float) =
        LinearLayout.LayoutParams(width, height, weight).apply { dp(8).let { setMargins(it, it, it, it) } }

    private fun appButton(app: HomeApp, iconDp: Int): Button {
        val button = Button(this, null, 0, R.style.AppButton)
        // A style given in code replaces the platform button style, centring included.
        button.gravity = Gravity.CENTER
        button.setPadding(dp(24), 0, dp(24), 0)
        val intent = Apps.launchIntent(this, app.packageName)
        if (intent == null) {
            button.isEnabled = false
            button.text = getString(R.string.not_installed, app.label)
            return button
        }
        button.text = app.label
        val icon = packageManager.getApplicationIcon(app.packageName)
        val size = dp(iconDp)
        icon.setBounds(0, 0, size, size)
        button.setCompoundDrawablesRelative(icon, null, null, null)
        button.compoundDrawablePadding = dp(16)
        button.setOnClickListener { startActivity(intent) }
        return button
    }

    private fun dp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private fun updateHeader() {
        val now = LocalTime.now()
        headerTime.text = ClockFormat.time(now.hour, now.minute)
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (level >= 0) headerBattery.text = ClockFormat.battery(level * 100 / scale, plugged)
    }

    private companion object {
        const val ICON_FEATURED_DP = 64
        const val ICON_GRID_DP = 40
    }
}
