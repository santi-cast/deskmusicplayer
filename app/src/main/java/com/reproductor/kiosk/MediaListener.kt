package com.reproductor.kiosk

import android.service.notification.NotificationListenerService

/**
 * Exists only so the system lets the app read media sessions.
 * Enabled with: adb shell cmd notification allow_listener com.reproductor.kiosk/.MediaListener
 */
class MediaListener : NotificationListenerService()
