package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.FakeShutdownActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.izhaanintellect.pasa.util.SecurityActivityLauncher

/**
 * Handles simulated power-off deception (/fakeshutdown) and restoration (/wake).
 */
@Singleton
class FakeShutdownCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/fakeshutdown"
    override val description = "Simulate power off with black screen and touch forensics"
    override val usage = "/fakeshutdown | /blackout | /wake"

    companion object {
        private const val TAG = "PASA_FakeShutdownCmd"
        const val NOTIFICATION_ID = 2003
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sub = args.firstOrNull()?.lowercase()

        return if (sub == "wake" || sub == "stop" || sub == "off") {
            wakeDevice()
        } else {
            startFakeShutdown()
        }
    }

    fun wakeDevice(): CommandResult {
        Log.i(TAG, "Waking device from Fake Shutdown")
        preferencesManager.isFakeShutdownActive = false

        try {
            val dismissIntent = Intent(FakeShutdownActivity.ACTION_DISMISS_FAKE_SHUTDOWN).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(dismissIntent)
            SecurityActivityLauncher.dismissNotification(context, NOTIFICATION_ID)

            // Re-enable status bar if not in Lost Mode
            if (!preferencesManager.isLostModeActive) {
                try {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val component = android.content.ComponentName(context, com.izhaanintellect.pasa.admin.PasaDeviceAdmin::class.java)
                    if (dpm?.isDeviceOwnerApp(context.packageName) == true) {
                        dpm.setStatusBarDisabled(component, false)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast wake intent: ${e.message}")
        }

        return CommandResult(
            success = true,
            message = "☀️ <b>Device Woken Up</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "Fake Shutdown mode deactivated.\n" +
                    "Screen brightness and audio ringer have been restored."
        )
    }

    private fun startFakeShutdown(): CommandResult {
        Log.i(TAG, "Starting Fake Shutdown deception")
        preferencesManager.isFakeShutdownActive = true

        try {
            // Ensure Lock Task mode is configured before blackout (critical for power button suppression)
            try {
                val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                val component = android.content.ComponentName(context, com.izhaanintellect.pasa.admin.PasaDeviceAdmin::class.java)
                if (dpm?.isDeviceOwnerApp(context.packageName) == true) {
                    dpm.setLockTaskPackages(component, arrayOf(context.packageName))
                    dpm.setLockTaskFeatures(component, android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                    dpm.setStatusBarDisabled(component, true)
                    Log.i(TAG, "Lock Task mode re-applied for fake shutdown")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure Lock Task mode: ${e.message}")
            }

            val intent = FakeShutdownActivity.createIntent(context)
            SecurityActivityLauncher.launch(
                context = context,
                intent = intent,
                notificationId = NOTIFICATION_ID,
                notificationTitle = "System Power Management",
                notificationText = "Display standby protocol active",
                wakeScreen = true,
                ongoing = true,
                silentNotification = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch FakeShutdownActivity", e)
            return CommandResult(
                success = false,
                message = "❌ Could not activate Fake Shutdown: ${e.message}"
            )
        }

        return CommandResult(
            success = true,
            message = "📴 <b>Fake Shutdown Activated!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "The phone is displaying an authentic power-off spinner and will black out completely.\n" +
                    "To a thief, the device appears completely powered off.\n\n" +
                    "📸 <i>If the thief touches the screen, silent front-camera snapshots and GPS telemetry will be captured and sent to Telegram!</i>\n\n" +
                    "Send <code>/wake</code> at any time to restore normal screen operation."
        )
    }
}
