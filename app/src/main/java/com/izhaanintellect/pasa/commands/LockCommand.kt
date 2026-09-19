package com.izhaanintellect.pasa.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote device locking and Lost Mode Guard activation.
 * Supports standard locking, custom emergency PIN lock, and Device Owner Kiosk Mode.
 */
@Singleton
class LockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
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

            if (args.isEmpty()) {
                // Standard instant lock
                dpm.lockNow()
                Log.i(TAG, "Device screen locked via standard /lock")
                return CommandResult(
                    success = true,
                    message = "🔒 Device locked immediately."
                )
            }

            var pinToSet: String? = null
            var messageText: String

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

            // Configure Lost Mode state
            if (pinToSet != null) {
                preferencesManager.activeLockPin = pinToSet
                preferencesManager.isLostModeActive = true
            }
            preferencesManager.lostModeMessage = messageText

            // If Device Owner is active, harden device:
            var ownerHardeningMsg = ""
            if (isDeviceOwner) {
                PasaDeviceAdmin.configureLockTask(context)
                PasaDeviceAdmin.setUninstallBlocked(context, true)
                PasaDeviceAdmin.setComprehensiveLockdown(context, true)
                try {
                    dpm.setDeviceOwnerLockScreenInfo(adminComponent, messageText)
                } catch (_: Exception) {}
                ownerHardeningMsg = "\n👑 <b>Device Owner:</b> Kiosk Lock Task, Airplane Mode/USB lockout & uninstall blocked."
            }

            // Launch full-screen Lost Mode Guard over lockscreen
            try {
                val alertIntent = AlertMessageActivity.createIntent(
                    context = context,
                    message = messageText,
                    enforcePin = (pinToSet != null)
                )
                context.startActivity(alertIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start AlertMessageActivity: ${e.message}")
            }

            // Call OS lock
            dpm.lockNow()

            if (pinToSet != null) {
                CommandResult(
                    success = true,
                    message = "🔒 <b>Lost Mode Guard Activated!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "🔑 <b>Emergency PIN:</b> <code>$pinToSet</code>\n" +
                            "💬 <b>Lock Message:</b> \"$messageText\"$ownerHardeningMsg\n\n" +
                            "<i>The phone is trapped in the Lost Mode screen. Enter PIN <code>$pinToSet</code> on the phone keypad or send <code>/unlock</code> from Telegram to release.</i>"
                )
            } else {
                CommandResult(
                    success = true,
                    message = "🔒 Device locked with lock-screen message: \"$messageText\"$ownerHardeningMsg"
                )
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Lock failed due to security exception", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Lock failed", e)
            CommandResult(success = false, message = "❌ Lock failed: ${e.message}")
        }
    }
}
