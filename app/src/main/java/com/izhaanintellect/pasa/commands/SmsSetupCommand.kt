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
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                java.net.URLEncoder.encode(uri, "UTF-8")
        val chunkedSecret = secret.chunked(4).joinToString(" ")

        val header = if (generated) "🔐 <b>SMS TOTP Enrolled</b>" else "🔐 <b>SMS TOTP Active</b>"
        return CommandResult(
            success = true,
            message = """
                $header
                ━━━━━━━━━━━━━━━━━━━━
                <b>Offline GSM SMS Command Backdoor</b>

                🔑 <b>Secret Key (for Authenticator):</b>
                <code>$secret</code>
                <i>Formatted:</i> <code>$chunkedSecret</code>

                📷 <b>Authenticator QR Code:</b>
                <a href="$qrUrl">👉 Tap here to open / scan QR Code</a>

                ━━━━━━━━━━━━━━━━━━━━
                📲 <b>How to Send an SMS Command:</b>
                Send an SMS from any phone to this device's SIM number:
                <code>PASA &lt;6-digit-code-or-MasterPassword&gt; /unlock</code>

                <b>Supported Commands:</b>
                • <code>PASA 839201 /unlock</code> (or your Master Password)
                • <code>PASA 839201 /locate</code> (returns Google Maps link)
                • <code>PASA 839201 /ring</code> (sounds emergency siren)
                • <code>PASA 839201 /lock</code> (engages lost mode kiosk)

                <i>Run <code>/smssetup reset</code> if you ever wish to rotate your TOTP secret.</i>
            """.trimIndent()
        )
    }
}
