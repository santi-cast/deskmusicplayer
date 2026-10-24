package com.reproductor.kiosk

import android.app.Activity
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Settings → Configuración inicial: everything that used to need `adb`, done from the phone.
 * Each step shows whether it is done and opens the Android screen that does it; that screen is
 * allowed through the lock until this screen shows again.
 */
class SetupActivity : Activity() {
    private lateinit var list: LinearLayout

    private class Step(val title: Int, val why: Int, val done: Boolean, val action: Int?, val run: (() -> Unit)?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        setContentView(ScrollView(this).apply { addView(list) })
    }

    override fun onResume() {
        super.onResume()
        Kiosk.lockIfEnabled(this)
        render()
    }

    private fun render() {
        list.removeAllViews()
        list.addView(text(getString(R.string.setup_title), 24f, 0xFFE0E0E0.toInt()))
        list.addView(text(getString(R.string.setup_intro), 15f, 0xFF9A9A9A.toInt()))
        steps().forEach { step ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            row.addView(text(if (step.done) "✓" else "•", 26f, if (step.done) 0xFF1DB954.toInt() else 0xFFE5A100.toInt()).apply {
                setPadding(0, 0, dp(16), 0)
            })
            val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(text(getString(step.title), 18f, 0xFFE0E0E0.toInt()))
            texts.addView(text(getString(step.why), 14f, 0xFF9A9A9A.toInt()))
            row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (!step.done && step.action != null && step.run != null) {
                row.addView(Button(this).apply {
                    setText(step.action)
                    isAllCaps = false
                    setOnClickListener { step.run.invoke() }
                })
            }
            list.addView(row)
        }
        list.addView(Button(this).apply {
            setText(R.string.close)
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }

    private fun steps(): List<Step> {
        val owner = Kiosk.isOwner(this)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val timeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 0)
        val prefs = Prefs(this)
        return listOfNotNull(
            Step(R.string.setup_overlay, R.string.setup_overlay_why, Settings.canDrawOverlays(this), R.string.setup_open) {
                openAndroid(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            },
            Step(R.string.setup_notifications, R.string.setup_notifications_why, hasNotificationAccess(this), R.string.setup_open) {
                openAndroid(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            Step(R.string.setup_power, R.string.setup_power_why, hasPowerMenu(), R.string.setup_open) {
                openAndroid(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            Step(R.string.setup_screen, R.string.setup_screen_why, SetupCheck.isScreenAlwaysOn(timeout), R.string.setup_apply) {
                keepScreenOn()
            },
            Step(R.string.setup_no_pin, R.string.setup_no_pin_why, !keyguard.isKeyguardSecure, R.string.setup_open) {
                openAndroid(Intent(Settings.ACTION_SECURITY_SETTINGS))
            },
            // Android cannot report whether the swipe screen is off, so the app remembers it applied it.
            Step(R.string.setup_lock_screen, R.string.setup_lock_screen_why, prefs.lockScreenOff, R.string.setup_apply) {
                val ok = Kiosk.disableLockScreen(this)
                prefs.lockScreenOff = ok
                Toast.makeText(this, if (ok) R.string.setup_done else R.string.setup_lock_screen_failed, Toast.LENGTH_LONG).show()
                render()
            }.takeIf { owner },
            Step(R.string.setup_owner, if (owner) R.string.setup_owner_done else R.string.setup_owner_why, owner, null, null),
        )
    }

    /** Writing the screen timeout needs the "modify system settings" permission, asked once. */
    private fun keepScreenOn() {
        if (!Settings.System.canWrite(this)) {
            openAndroid(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            return
        }
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE)
        render()
    }

    private fun openAndroid(intent: Intent) = Kiosk.openAllowingTarget(this, intent)

    private fun hasPowerMenu(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(ComponentName(this, PowerMenuService::class.java).flattenToString())
    }

    private fun text(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun dp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        fun hasNotificationAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return enabled.contains(ComponentName(context, MediaListener::class.java).flattenToString())
        }

        /** How many essential steps are missing, for the notice on the home screen. */
        fun pendingEssentials(context: Context) =
            SetupCheck.pendingEssentials(Settings.canDrawOverlays(context), hasNotificationAccess(context))
    }
}
