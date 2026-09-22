package com.izhaanintellect.pasa.commands

import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.CryptoLicenseVerifier
import com.izhaanintellect.pasa.security.LicenseManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks commercial license status and activates Pro keys with Ed25519 verification.
 * Operates autonomously in Direct Sovereign Mode without requiring persistent VPS telemetry.
 */
@Singleton
class LicenseCommand @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val licenseManager: LicenseManager,
    private val cryptoLicenseVerifier: CryptoLicenseVerifier
) : Command {

    override val name = "/license"
    override val description = "Check license tier or activate Pro key"
    override val usage = "/license [activate <KEY> | status]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            val verifiedTier = cryptoLicenseVerifier.getVerifiedStoredTier() ?: preferencesManager.licenseTier
            val isPro = licenseManager.isProActive()
            val deviceId = preferencesManager.deviceId
            val key = preferencesManager.licenseKey.ifBlank { "None (Free Evaluation)" }
            val modeStr = if (preferencesManager.useBackendServer) "VPS Cloud Relay" else "100% Sovereign (Direct Telegram)"

            val statusText = buildString {
                append("🔐 <b>PASA License & Sovereign Identity</b>\n")
                append("━━━━━━━━━━━━━━━━━━━━\n")
                append("• <b>Device ID:</b> <code>$deviceId</code>\n")
                append("• <b>Active Tier:</b> <b>$verifiedTier</b>\n")
                append("• <b>Pro Access:</b> ${if (isPro) "✅ Active" else "🔒 Inactive"}\n")
                append("• <b>Bound Key:</b> <code>$key</code>\n")
                append("• <b>Verification:</b> Hardware-backed Ed25519 (<0.2ms offline)\n")
                append("• <b>Architecture:</b> $modeStr\n\n")
                append("<i>To bind a purchased key:</i>\n")
                append("<code>/license activate PASA-PRO-XXXX-XXXX</code>")
            }
            return CommandResult(success = true, message = statusText)
        }

        if (args[0].equals("activate", ignoreCase = true) || args[0].equals("bind", ignoreCase = true)) {
            val key = if (args.size > 1) args[1].trim() else ""
            if (key.isBlank()) {
                return CommandResult(success = false, message = "⚠️ Please specify a valid license key:\n<code>/license activate PASA-PRO-XXXX-XXXX</code>")
            }

            val success = licenseManager.activateLicenseKey(key)
            if (success) {
                val newTier = cryptoLicenseVerifier.getVerifiedStoredTier() ?: preferencesManager.licenseTier
                return CommandResult(
                    success = true,
                    message = "✅ <b>License Activated Successfully!</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Tier:</b> <b>$newTier</b>\n" +
                            "• <b>Key:</b> <code>$key</code>\n" +
                            "• <b>Security:</b> Cryptographic Ed25519 Certificate Enrolled\n\n" +
                            "All Pro features and surveillance capabilities are now fully unlocked."
                )
            } else {
                return CommandResult(
                    success = false,
                    message = "❌ <b>License Activation Failed</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "Unable to verify or bind key <code>$key</code>.\n" +
                            "Please ensure your key is valid and you have an active network connection."
                )
            }
        }

        return CommandResult(success = false, message = "Usage: <code>/license</code> or <code>/license activate PASA-PRO-XXXX-XXXX</code>")
    }
}
