package com.reproductor.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device-owner entry point. Activated from the PC with
 * adb shell dpm set-device-owner com.reproductor.kiosk/.AdminReceiver
 * or by scanning the provisioning QR code in Android's setup wizard after a factory reset.
 */
class AdminReceiver : DeviceAdminReceiver() {
    /** End of QR provisioning: apply the kiosk policies and open the home screen, which locks. */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Kiosk.applyPolicies(context)
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
