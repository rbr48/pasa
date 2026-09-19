package com.izhaanintellect.pasa.commands

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.izhaanintellect.pasa.PasaApp
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote display message / owner alert broadcast on the device.
 * Turns on screen, displays a prominent full-screen lockscreen alert,
 * and sounds the emergency notification chime.
 */
@Singleton
class MessageCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/message"
    override val description = "Display an urgent message from the owner on the device screen"
    override val usage = "/message <your message here>"

    companion object {
        private const val TAG = "PASA_Message"
        private const val NOTIFICATION_ID = 2001
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(
                success = false,
                message = "⚠️ Please specify the message to display.\n<b>Usage:</b> <code>/message &lt;text&gt;</code>\n" +
                        "<i>Example: /message Please call +123456789. This phone is lost.</i>"
            )
        }

        val messageText = args.joinToString(" ")

        return try {
            // 1. Wake up the screen immediately
            wakeScreen()

            // 2. Launch AlertMessageActivity over lockscreen
            val alertIntent = AlertMessageActivity.createIntent(context, messageText)
            try {
                context.startActivity(alertIntent)
            } catch (actErr: Exception) {
                Log.w(TAG, "Could not start activity directly (background restrictions): ${actErr.message}")
            }

            // 3. Post full-screen intent notification to guarantee lockscreen display on Android 14-16
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val notification = NotificationCompat.Builder(context, PasaApp.ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🛡️ URGENT MESSAGE FROM OWNER")
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 1000))
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Owner message broadcasted to screen and notification tray: $messageText")

            CommandResult(
                success = true,
                message = "📢 <b>Owner Message Broadcasted to Device Screen</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "💬 <b>Message:</b> <i>\"$messageText\"</i>\n\n" +
                        "📱 <i>Screen turned ON, emergency audio chime sounded, and full-screen alert overlay displayed over the lockscreen.</i>"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast message", e)
            CommandResult(
                success = false,
                message = "❌ Failed to display message on screen: ${e.message}"
            )
        }
    }

    private fun wakeScreen() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(
                @Suppress("DEPRECATION") PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "pasa:alert_screen_wake"
            )
            wakeLock?.acquire(15000L) // Keep bright for 15 seconds
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquisition warning: ${e.message}")
        }
    }
}
