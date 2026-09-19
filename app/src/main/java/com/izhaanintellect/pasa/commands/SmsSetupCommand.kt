package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.Totp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enrolls / reveals the TOTP secret used to authenticate offline SMS commands.
 * The secret is delivered over the already-authenticated Telegram channel; the
 * owner adds it to an authenticator app (Google Authenticator, Aegis, etc.) and
 * then texts the current 6-digit code instead of the master password:
 *
 *   PASA <6-digit-code> /locate
 *
 *   /smssetup          — show the current otpauth URI + secret (generates one if none)
 *   /smssetup reset    — generate a brand-new secret (invalidates the old one)
 */
@Singleton
class SmsSetupCommand @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/smssetup"
    override val description = "Set up TOTP for secure offline SMS commands"
    override val usage = "/smssetup [reset]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val reset = args.getOrNull(0)?.lowercase() in setOf("reset", "new", "regenerate")

        var secret = preferencesManager.smsTotpSecret
        val generated = secret.isBlank() || reset
        if (generated) {
            secret = Totp.generateSecretBase32()
            preferencesManager.smsTotpSecret = secret
        }

        val account = preferencesManager.ownerChatId.ifBlank { "owner" }
        val uri = Totp.otpauthUri(secret, account = account, issuer = "PASA")

        val header = if (generated) "🔐 <b>SMS TOTP enrolled</b>" else "🔐 <b>SMS TOTP (existing)</b>"
        return CommandResult(
            success = true,
            message = """
                $header
                ━━━━━━━━━━━━━━━━━━━━
                Add this secret to an authenticator app (Google Authenticator, Aegis, etc.):

                🔑 <b>Secret:</b> <code>$secret</code>
                🔗 <b>otpauth URI:</b>
                <code>$uri</code>

                Then send offline SMS commands as:
                <code>PASA &lt;6-digit-code&gt; /locate</code>

                ⚠️ This replaces sending your master password over SMS. Keep the secret private; run <code>/smssetup reset</code> to rotate it.
            """.trimIndent()
        )
    }
}
