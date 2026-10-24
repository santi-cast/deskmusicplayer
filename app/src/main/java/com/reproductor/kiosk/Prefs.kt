package com.reproductor.kiosk

import android.content.Context

/** User settings, editable from the hidden settings screen. */
class Prefs(private val context: Context) {
    private val sp = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    /** Lock task on (only effective as device owner). */
    var kioskEnabled: Boolean
        get() = sp.getBoolean("kiosk_enabled", true)
        set(v) = sp.edit().putBoolean("kiosk_enabled", v).apply()

    /** Apps on the home screen, in order; until the user changes them, the default apps that are installed. */
    var selection: AppSelection
        get() = AppSelection.decode(sp.getString("home_apps", null)) { Apps.launchIntent(context, it) != null }
        set(v) = sp.edit().putString("home_apps", v.encode()).apply()

    /** Set once the swipe lock screen was removed as device owner (Android cannot report it). */
    var lockScreenOff: Boolean
        get() = sp.getBoolean("lock_screen_off", false)
        set(v) = sp.edit().putBoolean("lock_screen_off", v).apply()

    /** Package of the store chosen in Settings → Apps; null means the first installed one. */
    var store: String?
        get() = sp.getString("store", null)
        set(v) = sp.edit().putString("store", v).apply()

    /** The store to open, if any is installed. */
    val resolvedStore: Store? get() = Stores.resolve(store) { Apps.launchIntent(context, it) != null }

    var idleSeconds: Int
        get() = sp.getInt("idle_seconds", 60)
        set(v) = sp.edit().putInt("idle_seconds", v).apply()

    /** Window brightness while the clock shows, 0.01..1. */
    var clockBrightness: Float
        get() = sp.getFloat("clock_brightness", 0.01f)
        set(v) = sp.edit().putFloat("clock_brightness", v).apply()

    var nightStartMinute: Int
        get() = sp.getInt("night_start", 23 * 60)
        set(v) = sp.edit().putInt("night_start", v).apply()

    var nightEndMinute: Int
        get() = sp.getInt("night_end", 7 * 60)
        set(v) = sp.edit().putInt("night_end", v).apply()

    val nightSchedule: NightSchedule get() = NightSchedule(nightStartMinute, nightEndMinute)
}
