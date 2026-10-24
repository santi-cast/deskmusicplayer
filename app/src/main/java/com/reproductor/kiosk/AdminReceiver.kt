package com.reproductor.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Device-owner entry point. Activated from the PC with:
 * adb shell dpm set-device-owner com.reproductor.kiosk/.AdminReceiver
 */
class AdminReceiver : DeviceAdminReceiver()
