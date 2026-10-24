package com.reproductor.kiosk

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Settings, opened from the gear icon on the home screen. */
class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var idleSeconds: EditText
    private lateinit var clockBrightness: EditText
    private lateinit var nightStart: EditText
    private lateinit var nightEnd: EditText
    private lateinit var kioskEnabled: Switch
    private lateinit var removeOwner: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        status = findViewById(R.id.status)
        idleSeconds = findViewById(R.id.idle_seconds)
        clockBrightness = findViewById(R.id.clock_brightness)
        nightStart = findViewById(R.id.night_start)
        nightEnd = findViewById(R.id.night_end)
        kioskEnabled = findViewById(R.id.kiosk_enabled)
        removeOwner = findViewById(R.id.remove_owner)

        idleSeconds.setText(prefs.idleSeconds.toString())
        clockBrightness.setText(SettingsValidation.brightnessPercent(prefs.clockBrightness).toString())
        nightStart.setText(NightSchedule.formatMinutes(prefs.nightStartMinute))
        nightEnd.setText(NightSchedule.formatMinutes(prefs.nightEndMinute))
        kioskEnabled.isChecked = prefs.kioskEnabled

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.show_clock).setOnClickListener { showClock() }
        findViewById<Button>(R.id.apps).setOnClickListener { startActivity(Intent(this, AppsActivity::class.java)) }
        findViewById<Button>(R.id.setup).setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
        // Station logos only make sense with the Motorola FM radio (not every phone has one).
        findViewById<Button>(R.id.logos).apply {
            visibility = if (Apps.launchIntent(this@SettingsActivity, Apps.RADIO) != null) android.view.View.VISIBLE else android.view.View.GONE
            setOnClickListener { startActivity(Intent(this@SettingsActivity, LogosActivity::class.java)) }
        }
        findViewById<Button>(R.id.close).setOnClickListener { finish() }
        kioskEnabled.setOnCheckedChangeListener { _, checked -> setKioskEnabled(checked) }
        removeOwner.setOnClickListener { confirmRemoveOwner() }
    }

    override fun onResume() {
        super.onResume()
        // Back from an Android screen opened from here: restore the normal allow list and lock.
        Kiosk.lockIfEnabled(this)
        refreshStatus()
    }

    private fun refreshStatus() {
        val owner = Kiosk.isOwner(this)
        kioskEnabled.isEnabled = owner
        removeOwner.isEnabled = owner
        status.text = getString(
            R.string.status,
            yesNo(owner),
            yesNo(Kiosk.isLocked(this)),
            yesNo(Settings.canDrawOverlays(this)),
            yesNo(hasNotificationAccess()),
        )
    }

    private fun yesNo(value: Boolean) = getString(if (value) R.string.yes else R.string.no)

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(ComponentName(this, MediaListener::class.java).flattenToString())
    }

    /** Stores every field only when all of them are valid. */
    private fun save() {
        val values = SettingsValidation.parse(
            idleSeconds.text.toString(),
            clockBrightness.text.toString(),
            nightStart.text.toString(),
            nightEnd.text.toString(),
        )
        if (values == null) {
            Toast.makeText(this, R.string.invalid_settings, Toast.LENGTH_LONG).show()
            return
        }
        prefs.idleSeconds = values.idleSeconds
        prefs.clockBrightness = values.clockBrightness
        prefs.nightStartMinute = values.nightStartMinute
        prefs.nightEndMinute = values.nightEndMinute
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun showClock() {
        startForegroundService(
            Intent(this, ScreensaverService::class.java).setAction(ScreensaverService.ACTION_SHOW_NOW),
        )
        finish()
    }

    private fun setKioskEnabled(enabled: Boolean) {
        prefs.kioskEnabled = enabled
        if (enabled) Kiosk.lockIfEnabled(this) else if (Kiosk.isLocked(this)) stopLockTask()
        refreshStatus()
    }

    private fun confirmRemoveOwner() {
        AlertDialog.Builder(this)
            .setMessage(R.string.remove_owner_confirm)
            .setPositiveButton(R.string.ok) { _, _ ->
                Kiosk.removeOwner(this)
                refreshStatus()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
