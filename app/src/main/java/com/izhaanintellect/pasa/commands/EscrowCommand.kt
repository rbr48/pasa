package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.InlineKeyboardButton
import com.izhaanintellect.pasa.bot.InlineKeyboardMarkup
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.EscrowActivationActivity
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android Device Owner hardware escrow password token.
 * Arms, checks status, and launches Keyguard credential verification prompt.
 */
@Singleton
class EscrowCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/escrow"
    override val description = "Arm or check Knox hardware escrow password token [Device Owner]"
    override val usage = "/escrow [status|arm]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "👑 <b>Device Owner Required:</b>\n" +
                        "Hardware Escrow Tokens require Android Enterprise Device Owner.\n\n" +
                        "Activate via ADB on PC:\n" +
                        "<code>adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code>"
            )
        }

        val action = args.firstOrNull()?.lowercase()?.trim() ?: "status"
        val isTokenActive = PasaDeviceAdmin.isResetPasswordTokenActive(context)

        if (action in setOf("arm", "activate", "setup") || (!isTokenActive && action != "status")) {
            // Ensure token is enrolled with Keyguard
            PasaDeviceAdmin.ensureResetPasswordToken(context, preferencesManager)

            if (isTokenActive) {
                return CommandResult(
                    success = true,
                    message = "🔐 <b>Hardware Escrow Token Already Armed!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "✅ Token is active in Android Keyguard.\n" +
                            "• Remote PIN resets (<code>/set_os_pin</code>): Ready\n" +
                            "• Duress coercion unlock (<code>/duress_pin</code>): Ready\n" +
                            "• SIM tray lock PIN rotation (<code>/sim_tray_lock</code>): Ready"
                )
            }

            // Launch EscrowActivationActivity to prompt user on phone screen
            val actIntent = EscrowActivationActivity.createIntent(context)
            SecurityActivityLauncher.launch(
                context = context,
                intent = actIntent,
                notificationId = EscrowActivationActivity.NOTIFICATION_ID,
                notificationTitle = "🔐 Authorize Hardware Escrow Token",
                notificationText = "Enter your lockscreen PIN on the phone screen to arm remote password resets.",
                wakeScreen = true,
                ongoing = false
            )

            return CommandResult(
                success = true,
                message = "🔐 <b>Hardware Escrow Token Authorization Prompt Dispatched!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "📱 <b>Action needed on device:</b> Your phone screen has just illuminated with the system credential prompt.\n\n" +
                        "👉 <b>Enter your current lockscreen PIN on the phone screen now.</b>\n\n" +
                        "⚡ <i>Once verified, Android Keyguard will instantly arm the escrow token, enabling remote OS PIN resets and Duress unlocks!</i>"
            )
        }

        // Status
        if (isTokenActive) {
            return CommandResult(
                success = true,
                message = "🔐 <b>Hardware Escrow Token Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Status: ✅ <b>ARMED & ACTIVE</b>\n" +
                        "• Architecture: Android Keyguard Synthetic Password Escrow\n" +
                        "• Remote OS PIN Reset (<code>/set_os_pin</code>): ✅ Ready\n" +
                        "• Decoy Duress Coercion (<code>/duress_pin</code>): ✅ Armed\n" +
                        "• Cryptographic SIM Tray Lock (<code>/sim_tray_lock</code>): ✅ Armed\n\n" +
                        "🔒 <i>Your device is 100% authorized for Knox-grade hardware lockscreen management.</i>"
            )
        } else {
            val armKeyboard = InlineKeyboardMarkup(
                inlineKeyboard = listOf(
                    listOf(
                        InlineKeyboardButton("🔐 Arm Escrow Token Now", callbackData = "cmd:escrow:arm")
                    ),
                    listOf(
                        InlineKeyboardButton("🔄 Refresh Status", callbackData = "cmd:escrow:status"),
                        InlineKeyboardButton("🔙 Hub Menu", callbackData = "menu:device_owner_hub")
                    )
                )
            )

            return CommandResult(
                success = true,
                message = "🔐 <b>Hardware Escrow Token Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Status: ⚠️ <b>Pending Activation (Not Active)</b>\n" +
                        "• Token Enrollment: Enrolled in memory\n\n" +
                        "💡 <b>Why is it pending?</b>\n" +
                        "Android security architecture requires you to enter your current lockscreen PIN once on the system prompt to authorize cryptographic token arming.\n\n" +
                        "👉 <b>Tap the button below</b> to illuminate the phone screen with the authorization prompt!",
                replyMarkup = armKeyboard
            )
        }
    }
}
