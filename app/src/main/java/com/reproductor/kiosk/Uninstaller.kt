package com.reproductor.kiosk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/**
 * Uninstalls an app through PackageInstaller. As device owner Android does it without
 * asking; otherwise it hands back its own confirmation, which is shown here.
 */
object Uninstaller {
    private const val ACTION_RESULT = "com.reproductor.kiosk.UNINSTALL_RESULT"
    private const val EXTRA_LABEL = "label"

    fun uninstall(context: Context, packageName: String, label: String) {
        val result = Intent(context, ResultReceiver::class.java)
            .setAction(ACTION_RESULT)
            .putExtra(EXTRA_LABEL, label)
        val pending = PendingIntent.getBroadcast(context, packageName.hashCode(), result, PendingIntent.FLAG_UPDATE_CURRENT)
        context.packageManager.packageInstaller.uninstall(packageName, pending.intentSender)
    }

    class ResultReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION ->
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                PackageInstaller.STATUS_SUCCESS ->
                    Toast.makeText(context, context.getString(R.string.uninstalled, label), Toast.LENGTH_SHORT).show()
                else ->
                    Toast.makeText(context, context.getString(R.string.uninstall_failed, label), Toast.LENGTH_LONG).show()
            }
        }
    }
}
