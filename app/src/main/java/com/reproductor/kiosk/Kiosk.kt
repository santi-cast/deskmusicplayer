package com.reproductor.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri

/** Lock-task kiosk; only active when this app is the device owner. */
object Kiosk {
    private fun dpm(context: Context) = context.getSystemService(DevicePolicyManager::class.java)
    private fun admin(context: Context) = ComponentName(context, AdminReceiver::class.java)

    fun isOwner(context: Context) = dpm(context).isDeviceOwnerApp(context.packageName)

    fun isLocked(context: Context) =
        context.getSystemService(ActivityManager::class.java).lockTaskModeState !=
            ActivityManager.LOCK_TASK_MODE_NONE

    /**
     * Allows the selected apps in lock task, plus the app store and the system installer
     * so apps can be installed, and makes this app the permanent launcher.
     */
    fun applyPolicies(context: Context) {
        val dpm = dpm(context)
        val admin = admin(context)
        val tools = listOfNotNull(systemInstaller(context), Prefs(context).resolvedStore?.packageName)
        val allowed = Prefs(context).selection.lockTaskPackages(context.packageName, tools)
        dpm.setLockTaskPackages(admin, allowed.toTypedArray())
        dpm.addPersistentPreferredActivity(
            admin,
            IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            },
            ComponentName(context, MainActivity::class.java),
        )
    }

    /** The package that shows Android's install confirmation; its name varies between makers. */
    private fun systemInstaller(context: Context): String? =
        context.packageManager.resolveActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(Uri.parse("content://x/a.apk"), "application/vnd.android.package-archive"),
            0,
        )?.activityInfo?.packageName?.takeIf { it != "android" }

    /** Called on every resume of the home screen: locks again unless the lock was turned off. */
    fun lockIfEnabled(activity: Activity) {
        if (!isOwner(activity)) return
        applyPolicies(activity)
        if (Prefs(activity).kioskEnabled && !isLocked(activity)) activity.startLockTask()
    }

    /**
     * Opens an Android screen the lock would block (Android settings, the browser). The lock
     * stays on: the target app, plus [alsoAllow], is allowed until one of this app's screens is
     * shown again, which restores the normal allow list. (Stopping the lock instead is not
     * reliable on Android 8.1: the next screen was still blocked and the phone was left on a
     * black screen.)
     */
    fun openAllowingTarget(activity: Activity, intent: Intent, alsoAllow: List<String> = emptyList()) {
        allowTemporarily(activity, intent, alsoAllow)
        activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Adds the app that handles [intent] (and [alsoAllow]) to the lock until applyPolicies runs again. */
    fun allowTemporarily(activity: Activity, intent: Intent, alsoAllow: List<String> = emptyList()) {
        if (!isOwner(activity) || !isLocked(activity)) return
        val target = activity.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        val dpm = dpm(activity)
        val allowed = dpm.getLockTaskPackages(admin(activity)).toList() + listOfNotNull(target) + alsoAllow
        dpm.setLockTaskPackages(admin(activity), allowed.distinct().toTypedArray())
    }

    /** As device owner, removes the swipe lock screen (fails if the phone has a PIN or pattern). */
    fun disableLockScreen(context: Context): Boolean =
        isOwner(context) && dpm(context).setKeyguardDisabled(admin(context), true)

    /** Stops the lock and gives up device owner, leaving a normal phone. */
    @Suppress("DEPRECATION")
    fun removeOwner(activity: Activity) {
        if (isLocked(activity)) activity.stopLockTask()
        val dpm = dpm(activity)
        dpm.clearPackagePersistentPreferredActivities(admin(activity), activity.packageName)
        dpm.setLockTaskPackages(admin(activity), emptyArray())
        dpm.clearDeviceOwnerApp(activity.packageName)
    }
}
