package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.security.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely updates the PASA Sentinel Master PIN/Password via Telegram C2.
 * This Master PIN is used for in-app setup verification, Kiosk mode unlock,
 * destructive action confirmation (/wipe_confirm), and offline SMS authentication.
 */
@Singleton
class SetMasterPinCommand @Inject constructor(
    private val authManager: AuthManager
) : Command {

    override val name = "/set_master_pin"
    override val description = "Set or update PASA Master Emergency PIN/Password"
    override val usage = "/set_master_pin <new_pin>"

    companion object {
        private val PIN_REGEX = Regex("^[A-Za-z0-9@#\$%^&*!_\\-]{4,32}$")
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val newPin = args.firstOrNull()?.trim()

        if (newPin.isNullOrBlank()) {
            return CommandResult(
                success = false,
                message = """
                    🔐 <b>Set Master Emergency PIN / Password</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    Updates the primary authorization PIN for PASA Sentinel.
                    
                    <b>Syntax:</b> <code>/set_master_pin &lt;new_pin&gt;</code>
                    <b>Example:</b> <code>/set_master_pin 5892</code>
                    
                    <b>Used For:</b>
                    • Authenticating offline SMS commands (<code>PASA &lt;pin&gt; /locate</code>)
                    • Dismissing Lost Mode & Kiosk lockdown
                    • In-app settings access
                    • Confirming emergency remote wipe (<code>/wipe_confirm</code>)
                    
                    <i>Rules: 4 to 32 alphanumeric or standard symbol characters.</i>
                """.trimIndent()
            )
        }

        if (!newPin.matches(PIN_REGEX)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid PIN Format:</b>\nThe Master PIN must be between 4 and 32 characters (letters, numbers, or @#\$%^&*!_-)."
            )
        }

        return try {
            authManager.setMasterPassword(newPin)
            CommandResult(
                success = true,
                message = """
                    🔐 <b>PASA Master PIN Successfully Updated!</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    ✅ Your emergency master password has been cryptographically updated.
                    
                    🔑 <b>New Master PIN:</b> <code>$newPin</code>
                    
                    📱 <b>Offline SMS Backdoor:</b>
                    You can now command your phone via SMS using:
                    <code>PASA $newPin /locate</code>
                    <code>PASA $newPin /status</code>
                    <code>PASA $newPin /unlock</code>
                    
                    <i>Note: Delete this message from Telegram after saving your PIN to maintain security hygiene.</i>
                """.trimIndent()
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                message = "❌ <b>Failed to update Master PIN:</b> ${e.message}"
            )
        }
    }
}
