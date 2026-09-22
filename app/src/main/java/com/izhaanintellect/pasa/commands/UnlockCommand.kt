package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote unlock via Telegram (one of two remote-only unlock methods).
 *
 * PRODUCTION-READY SECURITY:
 * ✅ Remote-only (no local PIN)
 * ✅ Requires Telegram bot access
 * ✅ Complements SMS unlock (/sms_unlock)
 */
@Singleton
class UnlockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/unlock"
    override val description = "Remotely unlock device via Telegram"
    override val usage = "/unlock"

    companion object {
        private const val TAG = "PASA_TelegramUnlock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!preferencesManager.isLostModeActive) {
            Log.i(TAG, "⚠️ /unlock called but Lost Mode not active")
            return CommandResult(
                success = true,
                message = "ℹ️ Device is not currently in Lost Mode."
            )
        }

        Log.i(TAG, "🔓 Processing remote unlock via Telegram")

        try {
            // 1. Clear Lost Mode preferences
            preferencesManager.isLostModeActive = false
            preferencesManager.lostModeMessage = ""
            Log.i(TAG, "✅ Lost Mode preferences cleared")

            // 2. Clear Device Owner lockdown restrictions & restore keyguard
            try {
                com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setComprehensiveLockdown(context, false)
                com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setUninstallBlocked(context, false)

                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                val adminComponent = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.getComponentName(context)
                if (dpm != null && com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                    // Restore biometrics (fingerprint/face unlock) unless explicitly turned off by /biometrics command
                    val disabledFeatures = if (preferencesManager.biometricsDisabled) {
                        android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT or
                        (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS or
                            android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FACE
                        } else 0)
                    } else {
                        0
                    }
                    try {
                        dpm.setKeyguardDisabledFeatures(adminComponent, disabledFeatures)
                        Log.i(TAG, "✅ Keyguard features restored (disabledFeatures=$disabledFeatures)")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Could not restore keyguard features: ${e.message}")
                    }

                    // Clear device owner lockscreen warning message
                    try {
                        dpm.setDeviceOwnerLockScreenInfo(adminComponent, null)
                        Log.i(TAG, "✅ Lockscreen owner message cleared")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Could not clear lockscreen message: ${e.message}")
                    }
                }
                Log.i(TAG, "✅ Device Owner restrictions cleared")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Partial Device Owner cleanup: ${e.message}")
            }

            // 3. Broadcast dismissal to AlertMessageActivity
            try {
                val dismissIntent = Intent(AlertMessageActivity.ACTION_DISMISS_LOST_MODE).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(dismissIntent)
                Log.i(TAG, "✅ Dismissal broadcast sent")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Broadcast failed: ${e.message}")
            }

            // 4. Dismiss notification
            try {
                com.izhaanintellect.pasa.util.SecurityActivityLauncher.dismissNotification(
                    context,
                    AlertMessageActivity.NOTIFICATION_ID
                )
                Log.i(TAG, "✅ Notification dismissed")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Notification dismiss failed: ${e.message}")
            }

            return CommandResult(
                success = true,
                message = "🔓 <b>Device Unlocked via Telegram</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ Lost Mode Guard has been deactivated.\n" +
                        "Device is now fully accessible."
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Unlock failed: ${e.message}", e)
            return CommandResult(
                success = false,
                message = "❌ Unlock failed: ${e.message}"
            )
        }
    }
}
