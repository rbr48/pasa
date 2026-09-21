package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely resets or updates the device's native Android OS lock screen password/PIN.
 * Utilizes Android Enterprise Escrow Token API (resetPasswordWithToken).
 * Requires Enterprise Device Owner permissions.
 */
@Singleton
class SetOsPinCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/set_os_pin"
    override val description = "Reset Android OS hardware lockscreen PIN (Device Owner)"
    override val usage = "/set_os_pin <new_pin>"

    companion object {
        private val PIN_REGEX = Regex("^[0-9]{4,16}$")
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val isOwner = PasaDeviceAdmin.isDeviceOwner(context)
        if (!isOwner) {
            return CommandResult(
                success = false,
                message = "👑 <b>Device Owner Required:</b>\n" +
                        "Changing the hardware OS lockscreen password requires Android Enterprise Device Owner.\n\n" +
                        "Activate via ADB on PC:\n" +
                        "<code>adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code>"
            )
        }

        val newPin = args.firstOrNull()?.trim()
        if (newPin.isNullOrBlank()) {
            val isTokenActive = try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                dpm.isResetPasswordTokenActive(PasaDeviceAdmin.getComponentName(context))
            } catch (_: Exception) { false }

            return CommandResult(
                success = false,
                message = "🔑 <b>Reset OS Lockscreen PIN</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "<b>Syntax:</b> <code>/set_os_pin &lt;new_pin&gt;</code>\n" +
                        "<b>Example:</b> <code>/set_os_pin 5892</code>\n\n" +
                        "<b>Device Owner:</b> ✅ Active\n" +
                        "<b>Escrow Token Active:</b> ${if (isTokenActive) "✅ Ready (Hardware escrow armed)" else "⚠️ Waiting for initial unlock verification"}\n\n" +
                        "<i>Note: The PIN must be 4 to 16 digits.</i>"
            )
        }

        if (!newPin.matches(PIN_REGEX)) {
            return CommandResult(
                success = false,
                message = "❌ Invalid PIN format. The new OS lock screen PIN must be between 4 and 16 numeric digits."
            )
        }

        val (success, message) = PasaDeviceAdmin.resetDevicePassword(context, newPin, preferencesManager)

        if (!success && !PasaDeviceAdmin.isResetPasswordTokenActive(context)) {
            val actIntent = com.izhaanintellect.pasa.ui.EscrowActivationActivity.createIntent(context, newPin)
            com.izhaanintellect.pasa.util.SecurityActivityLauncher.launch(
                context = context,
                intent = actIntent,
                notificationId = com.izhaanintellect.pasa.ui.EscrowActivationActivity.NOTIFICATION_ID,
                notificationTitle = "🔑 Authorize Remote Lockscreen Reset",
                notificationText = "Confirm your current lockscreen PIN to authorize remote PIN changes.",
                wakeScreen = true,
                ongoing = false
            )

            return CommandResult(
                success = false,
                message = "🔑 <b>One-Time Authorization Prompt Sent to Phone Screen</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "📱 <b>Action needed on device:</b> Your phone screen has just illuminated with the system credential prompt.\n\n" +
                        "👉 <b>Enter your current lockscreen PIN on the phone screen now.</b>\n\n" +
                        "⚡ <i>Once verified, Android Keyguard will instantly arm the escrow token, apply the new PIN (<code>$newPin</code>), and notify you here!</i>"
            )
        }

        return if (success) {
            CommandResult(
                success = true,
                message = "🔐 <b>OS Lockscreen PIN Updated!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ The physical device lockscreen PIN has been permanently changed.\n\n" +
                        "🔑 <b>New Hardware PIN:</b> <code>$newPin</code>\n\n" +
                        "<i>The previous phone lockscreen password/biometrics have been overwritten.</i>"
            )
        } else {
            CommandResult(
                success = false,
                message = "❌ <b>OS Password Reset Failed:</b>\n$message"
            )
        }
    }
}
