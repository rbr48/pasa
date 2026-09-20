package com.izhaanintellect.pasa

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PASA Application class.
 * Initializes Hilt dependency injection, notification channels,
 * and WorkManager.
 */
@HiltAndroidApp
class PasaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesManager: com.izhaanintellect.pasa.data.PreferencesManager

    companion object {
        private const val TAG = "PASA_App"
        const val GUARDIAN_CHANNEL_ID = "pasa_guardian_channel"
        const val ALERT_CHANNEL_ID = "pasa_alerts_channel"
        /** Silent, badge-less channel used for camera/mic foreground service — minimizes notification footprint */
        const val STEALTH_CHANNEL_ID = "pasa_stealth_ops_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PASA (Private Android Security Agent) starting up...")
        createNotificationChannels()

        if (preferencesManager.isSetupComplete) {
            try {
                com.izhaanintellect.pasa.service.PasaService.start(this)
                Log.i(TAG, "Guardian service started from Application.onCreate")
            } catch (e: Exception) {
                Log.w(TAG, "Could not start service from Application.onCreate: ${e.message}")
            }
        }
    }


    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        val guardianChannel = NotificationChannel(
            GUARDIAN_CHANNEL_ID,
            getString(R.string.notification_channel_guardian),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_guardian_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_alerts_desc)
            enableVibration(true)
        }

        nm.createNotificationChannel(guardianChannel)
        nm.createNotificationChannel(alertChannel)

        val stealthChannel = NotificationChannel(
            STEALTH_CHANNEL_ID,
            "Security Operations",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Background hardware verification operations"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
        }
        nm.createNotificationChannel(stealthChannel)
        Log.d(TAG, "Notification channels registered")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
