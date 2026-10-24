package com.reproductor.kiosk

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Only used to open Android's power menu (Apagar / Reiniciar), which apps cannot
 * do otherwise. Enabled once from the PC:
 * adb shell settings put secure enabled_accessibility_services com.reproductor.kiosk/com.reproductor.kiosk.PowerMenuService
 * adb shell settings put secure accessibility_enabled 1
 */
class PowerMenuService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private var instance: PowerMenuService? = null

        /** Opens the power menu, or explains how to enable the service. */
        fun open(context: Context) {
            val opened = instance?.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) ?: false
            if (!opened) Toast.makeText(context, R.string.power_menu_disabled, Toast.LENGTH_LONG).show()
        }
    }
}
