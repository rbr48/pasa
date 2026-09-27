package com.izhaanintellect.pasa.commands

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends SMS messages to any phone number through the device's cellular radio.
 * When the recipient receives the SMS, the caller ID shows the SIM's phone number.
 *
 * Supports dual-SIM device targeting:
 *   /sendsms <number> <message>          (uses default active SIM)
 *   /sendsms sim1 <number> <message>     (forces SIM 1)
 *   /sendsms sim2 <number> <message>     (forces SIM 2)
 *
 * Example:
 *   /sendsms +8801XXXXXXXXX Ping from PASA
 *   /sendsms sim2 +1234567890 Test message
 */
@Singleton
class SendSmsCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/sendsms"
    override val description = "Send SMS to verify SIM line number or alert"
    override val usage = "/sendsms <number> <message> | /sendsms [sim1|sim2] <number> <message>"

    companion object {
        private const val TAG = "PASA_SendSms"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            val buttons = com.izhaanintellect.pasa.bot.InlineKeyboardMarkup(
                inlineKeyboard = listOf(
                    listOf(
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("📶 Active SIMs & Signal", callbackData = "cmd:sim"),
                        com.izhaanintellect.pasa.bot.InlineKeyboardButton("🔙 Back to Data Hub", callbackData = "menu:data_hub")
                    )
                )
            )
            return CommandResult(
                success = true,
                message = """
                    ✉️ <b>Direct Cellular Outbound SMS</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    Transmits an SMS directly through the device's cellular radio.

                    📝 <b>Format &amp; Templates (Tap to copy):</b>
                    • <code>/sendsms &lt;number&gt; &lt;message&gt;</code>
                    • <code>/sendsms sim1 &lt;number&gt; &lt;message&gt;</code>
                    • <code>/sendsms sim2 &lt;number&gt; &lt;message&gt;</code>

                    <b>Examples:</b>
                    <code>/sendsms +8801700000000 Emergency: Verify device location.</code>
                    <code>/sendsms sim1 +8801700000000 Ping from PASA</code>
                    <code>/sendsms sim2 +8801700000000 Ping from PASA</code>

                    ℹ️ <i>When the recipient receives the SMS, caller ID exposes the phone number of the device's SIM.</i>
                """.trimIndent(),
                replyMarkup = buttons
            )
        }

        // Parse arguments: check if first arg is a SIM selector (sim1/sim2)
        var simSelector = "default"
        var numberArg: String? = null
        var messageArg: String? = null

        val firstArg = args[0].lowercase()
        if (firstArg in setOf("sim1", "sim2", "default")) {
            simSelector = if (firstArg == "default") "default" else firstArg
            numberArg = args.getOrNull(1)
            messageArg = args.drop(2).joinToString(" ").ifBlank { null }
        } else {
            numberArg = args.getOrNull(0)
            messageArg = args.drop(1).joinToString(" ").ifBlank { null }
        }

        // Validate arguments
        if (numberArg.isNullOrBlank()) {
            return CommandResult(
                success = false,
                message = "❌ <b>Missing Phone Number</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "<b>Usage:</b> <code>/sendsms &lt;number&gt; &lt;message&gt;</code>"
            )
        }

        if (messageArg.isNullOrBlank()) {
            val prefix = if (simSelector != "default") "$simSelector " else ""
            return CommandResult(
                success = false,
                message = "❌ <b>Missing SMS Message Body</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "Recipient: <code>$numberArg</code>\n\n" +
                        "<b>Please send with your message body:</b>\n" +
                        "<code>/sendsms $prefix$numberArg Your emergency message here</code>"
            )
        }

        val rawNumber = numberArg.trim()
        // Sanitize phone number (strip whitespace, parens, hyphens, keep leading + and digits)
        val phoneNumber = rawNumber.filter { it.isDigit() || it == '+' }
        val messageBody = messageArg.trim()

        // Validate phone number format (supports short codes like 121 and international numbers)
        if (phoneNumber.length < 3 || phoneNumber.length > 16 || !phoneNumber.matches(Regex("^\\+?[0-9]{3,15}$"))) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Phone Number</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "⚠️ Phone must be 3-15 digits (e.g. <code>+8801700000000</code> or <code>121</code>).\n" +
                        "Provided: <code>$rawNumber</code>"
            )
        }

        // Message length validation (SMS max is ~160 chars per message, ~153 for Unicode)
        if (messageBody.length > 459) { // 3 SMS messages
            return CommandResult(
                success = false,
                message = "❌ <b>Message Too Long</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "⚠️ Maximum 459 characters allowed (3 SMS messages).\n" +
                        "Your message is ${messageBody.length} characters."
            )
        }

        // Self-heal SMS and Phone State permissions via Device Owner
        ensureSmsPermission()

        return try {
            sendSms(phoneNumber, messageBody, simSelector)
        } catch (e: Exception) {
            Log.e(TAG, "SMS send failed: ${e.message}", e)
            CommandResult(
                success = false,
                message = "❌ <b>SMS Send Failed</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "<code>${e.localizedMessage ?: e.message}</code>"
            )
        }
    }

    private fun ensureSmsPermission() {
        if (PasaDeviceAdmin.isDeviceOwner(context)) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = PasaDeviceAdmin.getComponentName(context)
                listOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                ).forEach { perm ->
                    dpm.setPermissionGrantState(
                        admin,
                        context.packageName,
                        perm,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not self-heal SMS permissions via Device Owner: ${e.message}")
            }
        }
    }

    private fun sendSms(phoneNumber: String, messageBody: String, simSelector: String): CommandResult {
        val smsManager: SmsManager = getSmsManager(simSelector)

        // Divide message into SMS parts (handles long messages)
        val parts = smsManager.divideMessage(messageBody)
        val partCount = parts.size

        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

        // Don't log phone number or message content - information disclosure risk
        Log.i(TAG, "SMS sent via $simSelector ($partCount parts)")

        val simDisplay = when (simSelector) {
            "sim1" -> "SIM 1"
            "sim2" -> "SIM 2"
            else -> "Default SIM"
        }

        return CommandResult(
            success = true,
            message = """
                ✅ <b>SMS Sent Successfully</b>
                ━━━━━━━━━━━━━━━━━━━━
                📱 <b>To:</b> <code>$phoneNumber</code>
                🔗 <b>Via:</b> $simDisplay
                💬 <b>Parts:</b> $partCount SMS message${if (partCount > 1) "s" else ""}

                ℹ️ Recipient will see caller ID as your device's SIM number.
            """.trimIndent()
        )
    }

    private fun getSmsManager(simSelector: String): SmsManager {
        return try {
            when (simSelector) {
                "sim1" -> getSmsManagerForSlot(0) ?: getDefaultSmsManager()
                "sim2" -> getSmsManagerForSlot(1) ?: getDefaultSmsManager()
                else -> getDefaultSmsManager()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting SmsManager: ${e.message}, using default")
            getDefaultSmsManager()
        }
    }

    private fun getDefaultSmsManager(): SmsManager {
        // On dual-SIM devices SmsManager.getDefault() sends to no SIM — must use the
        // system's designated default SMS subscription ID explicitly.
        return try {
            val defaultSubId = SubscriptionManager.getDefaultSmsSubscriptionId()
            if (defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.d(TAG, "Using default SMS subscription ID: $defaultSubId")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                        ?.createForSubscriptionId(defaultSubId)
                        ?: SmsManager.getSmsManagerForSubscriptionId(defaultSubId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(defaultSubId)
                }
            } else {
                Log.w(TAG, "No default SMS subscription — falling back to SmsManager.getDefault()")
                getFallbackSmsManager()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDefaultSmsManager failed (${e.message}), using fallback")
            getFallbackSmsManager()
        }
    }

    private fun getFallbackSmsManager(): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private fun getSmsManagerForSlot(slotIndex: Int): SmsManager? {
        return try {
            val hasReadPhone = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasReadPhone) {
                Log.w(TAG, "READ_PHONE_STATE not granted — cannot resolve SIM slot $slotIndex")
                return null
            }

            // Get subscription IDs for all active SIMs
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val activeSubscriptions = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()

            if (slotIndex >= activeSubscriptions.size) {
                Log.w(TAG, "SIM slot $slotIndex not available (only ${activeSubscriptions.size} active SIMs)")
                return null
            }

            val subscriptionId = activeSubscriptions[slotIndex].subscriptionId

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)?.createForSubscriptionId(subscriptionId)
                    ?: SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SmsManager for slot $slotIndex: ${e.message}", e)
            null
        }
    }
}
