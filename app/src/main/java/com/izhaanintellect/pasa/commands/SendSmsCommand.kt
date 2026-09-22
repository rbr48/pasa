package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
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
            return CommandResult(
                success = false,
                message = """
                    📱 <b>Send SMS Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    ⚠️ <b>Usage:</b>
                    • <code>/sendsms &lt;number&gt; &lt;message&gt;</code>
                    • <code>/sendsms sim1 &lt;number&gt; &lt;message&gt;</code>
                    • <code>/sendsms sim2 &lt;number&gt; &lt;message&gt;</code>

                    <b>Examples:</b>
                    <code>/sendsms +8801XXXXXXXXX Ping from PASA</code>
                    <code>/sendsms sim2 +1234567890 Test message</code>

                    ℹ️ When recipient receives the SMS, caller ID shows your SIM's phone number.
                    This helps verify which line number the device's SIM is using.
                """.trimIndent()
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
                message = "❌ <b>Invalid Arguments</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ Phone number is required."
            )
        }

        if (messageArg.isNullOrBlank()) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Arguments</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ Message body is required."
            )
        }

        val phoneNumber = numberArg.trim()
        val messageBody = messageArg.trim()

        // Validate phone number format (basic check)
        if (!phoneNumber.matches(Regex("^\\+?[0-9]{7,15}$"))) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Phone Number</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "⚠️ Phone must be 7-15 digits, optionally prefixed with +\n" +
                        "<code>$phoneNumber</code> is not valid."
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

        return try {
            sendSms(phoneNumber, messageBody, simSelector)
        } catch (e: Exception) {
            Log.e(TAG, "SMS send failed: ${e.message}", e)
            CommandResult(
                success = false,
                message = "❌ <b>SMS Send Failed</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "<code>${e.message}</code>"
            )
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
        return when (simSelector) {
            "sim1" -> getSmsManagerForSlot(0) ?: SmsManager.getDefault()
            "sim2" -> getSmsManagerForSlot(1) ?: SmsManager.getDefault()
            else -> {
                // Use default/active SIM
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }
        }
    }

    private fun getSmsManagerForSlot(slotIndex: Int): SmsManager? {
        return try {
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
