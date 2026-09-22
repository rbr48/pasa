package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `/duress_pin` — Configure emergency coercion distress trigger PIN.
 *
 * PRODUCTION-READY FIXES:
 * ✅ PIN encrypted with AES-256-GCM (not plain text)
 * ✅ Hardware token validation before success
 * ✅ No empty PIN accepted
 * ✅ Better error messaging
 * ✅ Attempt tracking integration
 */
@Singleton
class DuressPinCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager,
    private val duressAttemptTracker: com.izhaanintellect.pasa.detection.DuressAttemptTracker
) : Command {

    override val name = "/duress_pin"
    override val description = "Configure decoy coercion PIN for emergency SOS"
    override val usage = "/duress_pin <4-8 digits> | /duress_pin clear | /duress_pin status"

    companion object {
        private const val TAG = "PASA_DuressPin"
        private val PIN_REGEX = Regex("^[0-9]{4,8}$")
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val a11yActive = com.izhaanintellect.pasa.accessibility.AccessibilityScreenCaptureService.instance != null
        val isTokenActive = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isResetPasswordTokenActive(context)
        val configured = !preferencesManager.duressPin.isNullOrBlank()

        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            val remainingLockoutMs = duressAttemptTracker.getRemainingLockoutMs()
            val lockoutStatus = if (remainingLockoutMs > 0) {
                val secs = remainingLockoutMs / 1000
                "🔒 <b>Locked Out</b> (${secs}s remaining)"
            } else {
                "✅ Ready"
            }

            return CommandResult(
                success = true,
                message = "🆘 <b>Duress Coercion PIN Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Status: ${if (configured) "✅ <b>ACTIVE (Configured)</b>" else "⚠️ <b>Not Set</b>"}\n" +
                        "PIN Security: ${if (configured) "🔐 Encrypted (AES-256-GCM)" else "⚠️ N/A"}\n" +
                        "Attempt Lockout: $lockoutStatus\n" +
                        "Lockscreen Keypad Detection: ${if (a11yActive) "✅ Active" else "⚠️ Disabled (Enable in Accessibility)"}\n" +
                        "Hardware Escrow Token: ${if (isTokenActive) "✅ Armed" else "❌ Not Active"}\n\n" +
                        "<i>Usage: <code>/duress_pin &lt;4-8 digits&gt;</code> to set, <code>/duress_pin clear</code> to remove.</i>"
            )
        }

        val target = args[0].trim()

        if (target.equals("clear", ignoreCase = true) || target.equals("remove", ignoreCase = true)) {
            preferencesManager.duressPin = null
            duressAttemptTracker.resetAttempts()
            Log.i(TAG, "🗑️ Duress PIN cleared and attempt counter reset")
            return CommandResult(
                success = true,
                message = "✅ <b>Duress Coercion PIN Removed.</b>\n━━━━━━━━━━━━━━━━━━━━\nAttempt counter reset."
            )
        }

        // Validate PIN format
        if (!target.matches(PIN_REGEX)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid PIN Format</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "PIN must be 4-8 digits (numeric only).\n" +
                        "Examples: <code>1234</code>, <code>98765432</code>"
            )
        }

        // Check if PIN is same as master password
        if (authManager.hasMasterPassword() && authManager.verifyMasterPassword(target)) {
            return CommandResult(
                success = false,
                message = "❌ <b>PIN Conflict!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Duress PIN cannot match your Master PIN.\n" +
                        "Choose a different 4-8 digit code."
            )
        }

        // Validate hardware token is active BEFORE storing PIN
        if (!isTokenActive) {
            Log.w(TAG, "⚠️ Cannot activate duress PIN: hardware token not active")
            return CommandResult(
                success = false,
                message = "❌ <b>Hardware Token Not Active</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Before enabling Duress PIN, you must arm the Hardware Escrow Token.\n\n" +
                        "📋 <b>To arm token:</b>\n" +
                        "1. Run <code>/set_os_pin &lt;4-8 digits&gt;</code>\n" +
                        "2. Or run <code>/duress_pin &lt;pin&gt;</code> and complete the on-device prompt\n\n" +
                        "The token must be active for Duress PIN to automatically unlock the device."
            )
        }

        // All validation passed - store PIN (encrypted via PreferencesManager)
        preferencesManager.duressPin = target
        duressAttemptTracker.resetAttempts()  // Reset attempt counter on successful config
        Log.i(TAG, "✅ Duress PIN configured securely (AES-256-GCM encrypted)")

        val a11yNotice = if (!a11yActive) {
            "\n\n⚠️ <b>Accessibility Service Needed:</b>\n" +
            "Enable PASA in <b>Settings > Accessibility</b> for lockscreen keypad detection.\n" +
            "Without this, duress PIN won't trigger."
        } else {
            "\n\n✅ <i>Accessibility Service is active and monitoring keypad triggers.</i>"
        }

        return CommandResult(
            success = true,
            message = "🆘 <b>Duress Coercion PIN CONFIGURED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔑 <b>PIN:</b> <code>$target</code> (Encrypted: AES-256-GCM)\n" +
                    "🔐 <b>Hardware Token:</b> ✅ Armed\n\n" +
                    "🛡️ <i>If forced to unlock under physical threat:</i>\n" +
                    "• Enter <code>$target</code> on lockscreen keypad\n" +
                    "• Phone instantly unlocks to Home screen\n" +
                    "• Silently sends live GPS, front-camera photos, SOS alert to Telegram\n" +
                    "• Auto-hides all crypto/banking apps (Sterile Sandbox)\n" +
                    "• Starts continuous GPS tracking (every 2 minutes)\n\n" +
                    "⚠️ <b>Protection Against Brute Force:</b>\n" +
                    "• Attempt 1-2: 5-10 second lockout\n" +
                    "• Attempt 3-4: 20-40 second lockout\n" +
                    "• Attempt 5+: Exponential backoff (up to 24 hours)\n" +
                    "• Attempt 20: AUTOMATIC FACTORY RESET (fail-safe)" +
                    a11yNotice
        )
    }
}
