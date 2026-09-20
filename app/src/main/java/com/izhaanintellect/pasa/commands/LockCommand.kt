package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote device locking and Lost Mode Guard activation.
 * Supports standard locking, custom emergency PIN lock, Master PIN fallback, and Device Owner Kiosk Mode.
 */
@Singleton
class LockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/lock"
    override val description = "Lock device or enforce Lost Mode with custom PIN"
    override val usage = "/lock | /lock <pin> | /lock <pin> <message> | /lock message <text>"

    companion object {
        private const val TAG = "PASA_Lock"
        private val PIN_REGEX = Regex("^[0-9]{4,8}$")
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        if (!dpm.isAdminActive(adminComponent)) {
            return CommandResult(
                success = false,
                message = "❌ Device Administrator is not active. Cannot lock device."
            )
        }

        return try {
            val isDeviceOwner = PasaDeviceAdmin.isDeviceOwner(context)

            // Optional: allow explicit instant sleep via "/lock instant" or "/lock now"
            if (args.isNotEmpty() && (args[0].equals("instant", ignoreCase = true) || args[0].equals("now", ignoreCase = true))) {
                dpm.lockNow()
                Log.i(TAG, "Instant hardware lock executed")
                return CommandResult(
                    success = true,
                    message = "🔒 Device screen turned off and locked immediately."
                )
            }

            var pinToSet: String? = null
            var messageText: String

            if (args.isEmpty()) {
                // Syntax: /lock (no args) -> Enforce Lost Mode with Master PIN or auto-generated emergency PIN
                messageText = "This device has been reported lost. Please contact the owner."
                if (!authManager.hasMasterPassword() && preferencesManager.activeLockPin.isNullOrBlank()) {
                    pinToSet = (1000..9999).random().toString()
                }
            } else {
                val firstArg = args[0].trim()
                if (firstArg.matches(PIN_REGEX)) {
                    // Syntax: /lock <PIN> [optional message...]
                    pinToSet = firstArg
                    val remaining = args.drop(1).joinToString(" ").trim()
                    messageText = if (remaining.isNotBlank()) {
                        remaining
                    } else {
                        "This device has been reported lost. Please contact the owner."
                    }
                } else if (firstArg.equals("message", ignoreCase = true)) {
                    // Syntax: /lock message <text>
                    messageText = args.drop(1).joinToString(" ").trim()
                    if (messageText.isBlank()) messageText = "Please return this device to its owner."
                } else {
                    // Syntax: /lock <text>
                    messageText = args.joinToString(" ").trim()
                }
            }

            // Configure Lost Mode state
            if (pinToSet != null) {
                preferencesManager.activeLockPin = pinToSet
            }
            preferencesManager.isLostModeActive = true
            preferencesManager.lostModeMessage = messageText

            // If Device Owner is active, harden device (Airplane mode, USB transfer, status bar, uninstall blocked)
            var ownerHardeningMsg = ""
            if (isDeviceOwner) {
                PasaDeviceAdmin.configureLockTask(context)
                PasaDeviceAdmin.setUninstallBlocked(context, true)
                PasaDeviceAdmin.setComprehensiveLockdown(context, true)
                try {
                    dpm.setDeviceOwnerLockScreenInfo(adminComponent, messageText)
                } catch (_: Exception) {}
                ownerHardeningMsg = "\n👑 <b>Knox Device Owner:</b> Kiosk Lock Task, Airplane Mode/USB lockout & uninstall blocked."
            }

            // 1. Lock OS hardware keyguard
            dpm.lockNow()

            // 2. Launch full-screen Lost Mode Guard over lockscreen via resilient SecurityActivityLauncher
            val alertIntent = AlertMessageActivity.createIntent(
                context = context,
                message = messageText,
                enforcePin = true
            )

            SecurityActivityLauncher.launch(
                context = context,
                intent = alertIntent,
                notificationId = AlertMessageActivity.NOTIFICATION_ID,
                notificationTitle = "🛡️ LOST MODE GUARD ACTIVE",
                notificationText = messageText,
                wakeScreen = true,
                ongoing = true,
                silentNotification = false
            )

            val unlockInstructions = when {
                pinToSet != null -> "🔑 <b>Emergency PIN:</b> <code>$pinToSet</code>\n"
                preferencesManager.activeLockPin != null -> "🔑 <b>Active PIN:</b> <code>${preferencesManager.activeLockPin}</code>\n"
                else -> "🔑 <b>Unlock with:</b> Your <b>Master PIN</b> on the phone keypad.\n"
            }

            CommandResult(
                success = true,
                message = "🔒 <b>Knox Lost Mode Guard Activated!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        unlockInstructions +
                        "💬 <b>Lock Message:</b> \"$messageText\"$ownerHardeningMsg\n\n" +
                        "<i>The phone is trapped in the Lost Mode lockscreen. Enter the PIN on the device keypad or send <code>/unlock</code> from Telegram to release.</i>"
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "Lock failed due to security exception", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Lock failed", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        }
    }
}
