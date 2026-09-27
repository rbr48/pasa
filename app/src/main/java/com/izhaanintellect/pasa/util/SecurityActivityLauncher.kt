package com.izhaanintellect.pasa.util

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.izhaanintellect.pasa.PasaApp

/**
 * Universal launcher for security and lockscreen activities across Android 10 through 16.
 * Bypasses Background Activity Launch (BAL) restrictions via:
 * 1. Screen wake-lock acquisition (lighting the display when locked)
 * 2. ActivityOptions background activity allowance (Android 14+)
 * 3. High-priority full-screen intent notifications (CATEGORY_ALARM)
 * 4. Resilient fallback to direct startActivity
 */
object SecurityActivityLauncher {

    private const val TAG = "PASA_ActivityLauncher"

    fun launch(
        context: Context,
        intent: Intent,
        notificationId: Int,
        notificationTitle: String,
        notificationText: String,
        wakeScreen: Boolean = true,
        ongoing: Boolean = true,
        silentNotification: Boolean = false
    ) {
        // 1. Wake screen if requested
        if (wakeScreen) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = pm?.newWakeLock(
                    @Suppress("DEPRECATION") PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "pasa:security_launch_wake"
                )
                wakeLock?.acquire(15000L) // 15 seconds wake window
                Log.d(TAG, "Screen wake-lock acquired for $notificationTitle")
            } catch (e: Exception) {
                Log.w(TAG, "WakeLock acquisition warning: ${e.message}")
            }
        }

        // 2. Ensure resilient window flags
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
            Intent.FLAG_ACTIVITY_NO_USER_ACTION
        )

        // 3. Direct startActivity attempt (succeeds if SYSTEM_ALERT_WINDOW is granted or app in foreground)
        try {
            context.startActivity(intent)
            Log.d(TAG, "Direct startActivity dispatched for $notificationTitle")
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity restricted by OS: ${e.message}")
        }

        // 4. Create PendingIntent with background activity start allowance for Android 14+
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                options.pendingIntentBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
            Log.d(TAG, "PendingIntent sent with background start allowance")
        } catch (e: Exception) {
            Log.w(TAG, "PendingIntent send error: ${e.message}")
        }

        // 5. Post high-priority full-screen intent notification to guarantee lockscreen takeover
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val builder = NotificationCompat.Builder(context, PasaApp.ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)

            if (silentNotification) {
                builder.setSilent(true)
            } else {
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }

            nm?.notify(notificationId, builder.build())
            Log.i(TAG, "High-priority fullScreenIntent posted for $notificationTitle (id: $notificationId)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not post full-screen intent notification: ${e.message}")
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId)
            Log.d(TAG, "Notification $notificationId dismissed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dismiss notification $notificationId: ${e.message}")
        }
    }

    /**
     * Forcibly turns on and brightens the display hardware for a designated duration.
     */
    fun wakeScreen(context: Context, durationMs: Long = 10000L) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "pasa:security_display_wake"
            )
            wakeLock?.acquire(durationMs)
            Log.d(TAG, "Screen hardware wake-lock acquired for ${durationMs}ms")
        } catch (e: Exception) {
            Log.w(TAG, "Failed acquiring screen wake lock: ${e.message}")
        }
    }
}
