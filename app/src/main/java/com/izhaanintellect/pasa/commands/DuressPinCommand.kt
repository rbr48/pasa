package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuressPinCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
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

        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            val configured = !preferencesManager.duressPin.isNullOrBlank()
            return CommandResult(
                success = true,
                message = "🆘 <b>Duress Coercion PIN Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Status: ${if (configured) "✅ <b>ACTIVE (Configured)</b>" else "⚠️ <b>Not Set</b>"}\n" +
                        "Lockscreen Keypad Detection: ${if (a11yActive) "✅ Active" else "⚠️ Disabled (Enable PASA in Accessibility Settings)"}\n" +
                        "Hardware Escrow Unlock: ${if (isTokenActive) "✅ Armed (Physical unlock ready)" else "⚠️ Pending Arming (Run /set_os_pin or set duress PIN to arm)"}\n\n" +
                        "<i>Usage: <code>/duress_pin &lt;4-8 digits&gt;</code> to set or <code>/duress_pin clear</code> to remove.</i>\n\n" +
                        "<i>When entered on the device lockscreen keypad, the phone will unlock to Home screen while silently dispatching high-priority SOS alerts, GPS, and photos to Telegram.</i>"
            )
        }

        val target = args[0].trim()

        if (target.equals("clear", ignoreCase = true) || target.equals("remove", ignoreCase = true)) {
            preferencesManager.duressPin = null
            Log.i(TAG, "Duress PIN cleared")
            return CommandResult(
                success = true,
                message = "✅ <b>Duress Coercion PIN Removed.</b>"
            )
        }

        if (!target.matches(PIN_REGEX)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid PIN format!</b> Must be 4 to 8 digits (numeric only)."
            )
        }

        if (authManager.hasMasterPassword() && authManager.verifyMasterPassword(target)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Duress PIN Conflict!</b>\n" +
                        "The Duress PIN cannot be identical to your Master PIN. Please choose a distinct decoy PIN to ensure accurate anti-coercion detection."
            )
        }

        preferencesManager.duressPin = target
        Log.i(TAG, "Duress PIN successfully configured")

        val a11yNotice = if (!a11yActive) {
            "\n\n⚠️ <b>Important:</b> Enable PASA in <b>Phone Settings &gt; Accessibility</b> so PASA can detect keypad taps on your native lockscreen."
        } else {
            "\n\n✅ <i>Accessibility Service is active and monitoring keypad distress triggers.</i>"
        }

        val tokenNotice = if (!isTokenActive) {
            val actIntent = com.izhaanintellect.pasa.ui.EscrowActivationActivity.createIntent(context, null)
            com.izhaanintellect.pasa.util.SecurityActivityLauncher.launch(
                context = context,
                intent = actIntent,
                notificationId = com.izhaanintellect.pasa.ui.EscrowActivationActivity.NOTIFICATION_ID,
                notificationTitle = "🛡️ Arm Duress Hardware Unlock",
                notificationText = "Confirm your current lockscreen PIN to authorize automatic emergency unlocking.",
                wakeScreen = true,
                ongoing = false
            )
            "\n\n🔑 <b>Action Needed on Phone:</b> Your phone screen has illuminated with a system prompt. Enter your current lockscreen PIN once to arm the hardware escrow token. This enables your Duress PIN to automatically unlock the physical lockscreen."
        } else {
            "\n\n🔐 <i>Hardware Escrow Token is armed: Entering <code>$target</code> will clear the physical lockscreen and open the Home screen!</i>"
        }

        return CommandResult(
            success = true,
            message = "🆘 <b>Duress Coercion PIN Configured!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔑 <b>Duress PIN:</b> <code>$target</code>\n\n" +
                    "🛡️ <i>If forced to unlock your phone under threat or coercion, enter <code>$target</code> on your lockscreen keypad or PASA overlay. The phone will instantly unlock to Home while silently dispatching front-camera mugshots, live GPS, and an emergency SOS beacon to Telegram!</i>" +
                    a11yNotice +
                    tokenNotice
        )
    }
}
