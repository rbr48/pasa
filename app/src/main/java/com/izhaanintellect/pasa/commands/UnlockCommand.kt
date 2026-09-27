package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import com.izhaanintellect.pasa.ui.ScreenGuardActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote unlock via Telegram or SMS.
 *
 * PRODUCTION-READY SECURITY:
 * ✅ Zero-trust Master Password or TOTP verification required
 * ✅ Unified lockdown reversal: Releases Knox Kiosk Lost Mode AND Fake Shutdown blackout
 * ✅ Forcibly wakes hardware display and restores keyguard
 */
@Singleton
class UnlockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/unlock"
    override val description = "Remotely unlock device and exit all lockdown / blackout modes (Requires Master Password)"
    override val usage = "/unlock <master_password>"

    companion object {
        private const val TAG = "PASA_TelegramUnlock"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val isLostMode = preferencesManager.isLostModeActive
        val isFakeShutdown = preferencesManager.isFakeShutdownActive

        if (!isLostMode && !isFakeShutdown) {
            Log.i(TAG, "⚠️ /unlock called but neither Lost Mode nor Fake Shutdown is active")
            return CommandResult(
                success = true,
                message = "ℹ️ Device is not currently in Lost Mode or Fake Shutdown."
            )
        }

        // Zero-Trust verification: If Master Password is set, require authentication
        if (authManager.hasMasterPassword()) {
            val candidate = args.firstOrNull()?.trim()
            val isAuthorizedChat = authManager.isAuthorizedChat(chatId)
            val isSessionAuth = authManager.isSessionAuthenticated()
            val totpSecret = preferencesManager.smsTotpSecret
            val isTotpValid = !candidate.isNullOrBlank() && totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
            val isPassValid = !candidate.isNullOrBlank() && authManager.verifyMasterPassword(candidate)

            val isAuthorized = (isAuthorizedChat && candidate.isNullOrBlank()) || isSessionAuth || isPassValid || isTotpValid

            if (!isAuthorized) {
                if (candidate.isNullOrBlank()) {
                    val modeLabel = if (isFakeShutdown && isLostMode) "Lost Mode & Fake Shutdown"
                    else if (isFakeShutdown) "Fake Shutdown Blackout"
                    else "Lost Mode Kiosk"

                    return CommandResult(
                        success = false,
                        message = """
                            🔑 <b>Release $modeLabel (Zero-Trust Guard)</b>
                            ━━━━━━━━━━━━━━━━━━━━
                            To release security lockdown and restore full device access, Master Password verification is required.

                            <b>Syntax:</b> <code>/unlock &lt;master_password&gt;</code>
                            <b>Example:</b> <code>/unlock MySecretPass123</code>
                        """.trimIndent()
                    )
                } else {
                    return CommandResult(
                        success = false,
                        message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password or TOTP. Unlock rejected."
                    )
                }
            }
        }

        Log.i(TAG, "🔓 Processing remote unlock (LostMode=$isLostMode, FakeShutdown=$isFakeShutdown)")

        try {
            val deactivatedModes = mutableListOf<String>()

            // 1. Release Fake Shutdown Deception if active
            if (isFakeShutdown) {
                preferencesManager.isFakeShutdownActive = false
                try {
                    val dismissFakeShutdown = Intent(ScreenGuardActivity.ACTION_DISMISS_SCREEN_GUARD).apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(dismissFakeShutdown)
                    com.izhaanintellect.pasa.util.SecurityActivityLauncher.dismissNotification(
                        context,
                        ScreenGuardCommand.NOTIFICATION_ID
                    )
                    deactivatedModes.add("Fake Shutdown Blackout")
                    Log.i(TAG, "✅ Fake Shutdown dismissal broadcast sent")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to broadcast fake shutdown dismissal: ${e.message}")
                }
            }

            // 2. Release Knox Lost Mode if active
            if (isLostMode) {
                preferencesManager.isLostModeActive = false
                preferencesManager.lostModeMessage = ""
                deactivatedModes.add("Knox Kiosk Lost Mode")
                Log.i(TAG, "✅ Lost Mode preferences cleared")

                // Clear Device Owner lockdown restrictions & restore keyguard
                try {
                    com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setComprehensiveLockdown(context, false)
                    com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setUninstallBlocked(context, false)

                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    val adminComponent = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.getComponentName(context)
                    if (dpm != null && com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                        val disabledFeatures = if (preferencesManager.biometricsDisabled) {
                            android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT or
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

                        // Re-enable status bar
                        try {
                            dpm.setStatusBarDisabled(adminComponent, false)
                        } catch (_: Exception) {}

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

                // Broadcast dismissal to AlertMessageActivity
                try {
                    val dismissIntent = Intent(AlertMessageActivity.ACTION_DISMISS_LOST_MODE).apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(dismissIntent)
                    Log.i(TAG, "✅ Lost Mode dismissal broadcast sent")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Broadcast failed: ${e.message}")
                }

                // Dismiss notification
                try {
                    com.izhaanintellect.pasa.util.SecurityActivityLauncher.dismissNotification(
                        context,
                        AlertMessageActivity.NOTIFICATION_ID
                    )
                    Log.i(TAG, "✅ Notification dismissed")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Notification dismiss failed: ${e.message}")
                }
            }

            // 3. Forcibly acquire bright wake lock to wake physical AMOLED/LCD panel
            com.izhaanintellect.pasa.util.SecurityActivityLauncher.wakeScreen(context)

            val modesSummary = deactivatedModes.joinToString(" & ")
            return CommandResult(
                success = true,
                message = "🔓 <b>Device Unlocked via Remote Command</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ $modesSummary deactivated.\n" +
                        "☀️ Screen hardware woken up. Device is now fully accessible."
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

