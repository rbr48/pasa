package com.izhaanintellect.pasa.commands

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.izhaanintellect.pasa.util.OemProtectionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OEM Autostart & Background Protection Command.
 * Detects aggressive OEM battery killers (Xiaomi HyperOS/MIUI, Samsung OneUI, Huawei EMUI,
 * Oppo ColorOS, Vivo OriginOS, Transsion) and provides 1-tap navigation to whitelist PASA.
 *
 * Command: /autostart
 */
@Singleton
class AutostartCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/autostart"
    override val description = "OEM background autostart & battery protection dispatcher"
    override val usage = "/autostart"

    companion object {
        private const val NOTIFICATION_ID = 4004
        private const val CHANNEL_ID = "pasa_oem_autostart"
        const val ACTION_LAUNCH_OEM_AUTOSTART = "com.izhaanintellect.pasa.ACTION_LAUNCH_OEM_AUTOSTART"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val manufacturer = Build.MANUFACTURER.uppercase()
        val model = Build.MODEL
        val oemMenuName = OemProtectionHelper.getOemProtectionTitle()
        val isAggressive = OemProtectionHelper.isAggressiveOem()

        // Dispatch a 1-tap high-priority notification to the phone display
        val notificationDispatched = dispatchAutostartNotification()

        val guidance = when {
            manufacturer.contains("XIAOMI") || manufacturer.contains("REDMI") || manufacturer.contains("POCO") -> """
                <b>Xiaomi / HyperOS / MIUI Instructions:</b>
                1. Tap the notification on your phone or open <b>Security</b> app.
                2. Tap <b>Manage Apps</b> ➔ <b>PASA Sentinel</b>.
                3. Enable <b>Autostart</b> (allow background start).
                4. Set <b>Battery Saver</b> to <b>No Restrictions</b>.
            """.trimIndent()

            manufacturer.contains("SAMSUNG") -> """
                <b>Samsung OneUI Instructions:</b>
                1. Open <b>Settings</b> ➔ <b>Battery and device care</b> ➔ <b>Battery</b>.
                2. Tap <b>Background usage limits</b>.
                3. Add <b>PASA Sentinel</b> to <b>Never sleeping apps</b>.
            """.trimIndent()

            manufacturer.contains("HUAWEI") || manufacturer.contains("HONOR") -> """
                <b>Huawei / Honor Instructions:</b>
                1. Open <b>Settings</b> ➔ <b>Battery</b> ➔ <b>App launch</b>.
                2. Find <b>PASA Sentinel</b> and switch to <b>Manage manually</b>.
                3. Enable all 3: <i>Auto-launch</i>, <i>Secondary launch</i>, and <i>Run in background</i>.
            """.trimIndent()

            manufacturer.contains("OPPO") || manufacturer.contains("REALME") -> """
                <b>Oppo / Realme / ColorOS Instructions:</b>
                1. Open <b>Settings</b> ➔ <b>App Management</b> ➔ <b>Auto-launch apps</b>.
                2. Toggle <b>PASA Sentinel</b> to <b>Allowed</b>.
                3. Under Battery ➔ Allow background activity.
            """.trimIndent()

            manufacturer.contains("VIVO") || manufacturer.contains("IQOO") -> """
                <b>Vivo / iQOO / FuntouchOS Instructions:</b>
                1. Open <b>Settings</b> ➔ <b>Battery</b> ➔ <b>Background power consumption</b>.
                2. Find <b>PASA Sentinel</b> and select <b>High background power usage</b>.
                3. In iManager ➔ App manager ➔ Autostart manager ➔ Enable PASA.
            """.trimIndent()

            else -> """
                <b>Standard Android Instructions:</b>
                1. Open <b>Settings</b> ➔ <b>Apps</b> ➔ <b>PASA Sentinel</b>.
                2. Tap <b>Battery</b> ➔ Select <b>Unrestricted</b>.
                3. Ensure background data and autostart permissions are granted.
            """.trimIndent()
        }

        val notifyStatus = if (notificationDispatched) {
            "📲 <b>1-Tap Setup Notification:</b> Dispatched to phone screen.\n<i>(Tap the notification on your phone to open the OEM menu instantly)</i>\n\n"
        } else ""

        val responseText = """
            ⚙️ <b>OEM AUTOSTART & BACKGROUND SURVIVAL</b>
            ━━━━━━━━━━━━━━━━━━━━
            📱 <b>Device:</b> $manufacturer $model
            🏷️ <b>OEM Protection Profile:</b> $oemMenuName
            ⚠️ <b>Aggressive Killer Detected:</b> ${if (isAggressive) "YES (Action Required)" else "Standard"}

            $notifyStatus$guidance
            ━━━━━━━━━━━━━━━━━━━━
            🛡️ <i>Ensures PASA daemon survives background memory purges & deep sleep.</i>
        """.trimIndent()

        return CommandResult(success = true, message = responseText)
    }

    private fun dispatchAutostartNotification(): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "PASA OEM Protection Dispatcher",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "1-Tap navigation to OEM autostart and battery whitelist settings"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Launch helper intent
            val launchIntent = Intent(ACTION_LAUNCH_OEM_AUTOSTART).apply {
                setPackage(context.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🛡️ PASA: Configure Background Protection")
                .setContentText("Tap here to open ${OemProtectionHelper.getOemProtectionTitle()}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Your device requires explicit background permission. Tap here to configure ${OemProtectionHelper.getOemProtectionTitle()}."
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: Exception) {
            false
        }
    }
}
