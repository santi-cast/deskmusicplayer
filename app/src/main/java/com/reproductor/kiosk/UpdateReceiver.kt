package com.reproductor.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Installing an update ends the lock task; reopen the home screen so it locks again at once. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
