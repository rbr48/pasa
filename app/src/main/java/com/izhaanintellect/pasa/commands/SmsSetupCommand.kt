package com.izhaanintellect.pasa.commands

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
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
 *   /smssetup <master_password>          — show the current otpauth URI + secret
 *   /smssetup <master_password> reset    — generate a brand-new secret (invalidates old)
 */
@Singleton
class SmsSetupCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/smssetup"
    override val description = "Set up TOTP for secure offline SMS commands (Requires Master Password)"
    override val usage = "/smssetup <master_password> [reset]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (authManager.hasMasterPassword()) {
            val candidate = args.firstOrNull()?.trim()
            if (candidate.isNullOrBlank()) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>SMS TOTP Setup (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        To reveal or rotate your offline SMS command TOTP secret, Master Password verification is required.

                        <b>Syntax:</b> <code>/smssetup &lt;master_password&gt; [reset]</code>
                        <b>Example:</b> <code>/smssetup MySecretPass123</code>
                        <b>Rotate Secret:</b> <code>/smssetup MySecretPass123 reset</code>
                    """.trimIndent()
                )
            }

            if (!authManager.verifyMasterPassword(candidate)) {
                return CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. SMS TOTP access rejected."
                )
            }
        }

        val reset = (if (authManager.hasMasterPassword()) args.getOrNull(1) else args.getOrNull(0))?.lowercase() in setOf("reset", "new", "regenerate")

        var secret = preferencesManager.smsTotpSecret
        val generated = secret.isBlank() || reset
        if (generated) {
            secret = Totp.generateSecretBase32()
            preferencesManager.smsTotpSecret = secret
        }

        val account = preferencesManager.ownerChatId.ifBlank { "owner" }
        val uri = Totp.otpauthUri(secret, account = account, issuer = "PASA")
        val chunkedSecret = secret.chunked(4).joinToString(" ")

        // Generate high-resolution QR code 100% locally on-device (Zero-Knowledge, Zero Third-Party Leaks)
        val qrFile = renderQrCodeLocally(uri)

        val header = if (generated) "🔐 <b>SMS TOTP Enrolled</b>" else "🔐 <b>SMS TOTP Active</b>"
        return CommandResult(
            success = true,
            photoFile = qrFile,
            message = """
                $header
                ━━━━━━━━━━━━━━━━━━━━
                <b>Offline GSM SMS Command Backdoor</b>

                🔑 <b>Secret Key (for Authenticator):</b>
                <code>$secret</code>
                <i>Formatted:</i> <code>$chunkedSecret</code>

                📷 <b>Authenticator QR Code:</b>
                <i>Generated 100% offline on-device and attached below. Scan with Google Authenticator, Aegis, or Bitwarden.</i>

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

    private fun renderQrCodeLocally(content: String, size: Int = 512): File? {
        return try {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            val qrFile = File(context.cacheDir, "totp_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(qrFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            qrFile
        } catch (e: Exception) {
            android.util.Log.e("SmsSetupCommand", "Failed to render offline QR code: ${e.message}", e)
            null
        }
    }
}

