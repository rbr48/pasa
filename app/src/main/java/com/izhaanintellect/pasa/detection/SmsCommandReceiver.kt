package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.izhaanintellect.pasa.bot.CommandExecutor
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.security.AuthManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Out-of-band offline command receiver via GSM SMS messages.
 * Format: PASA password command args...
 * Example: PASA mypassword123 /locate
 * Example: PASA mypassword123 /lock 5892 Lost phone
 * Example: PASA mypassword123 /ring
 */
@AndroidEntryPoint
class SmsCommandReceiver : BroadcastReceiver() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var commandExecutor: CommandExecutor
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "PASA_SMS"
        private const val PREFIX = "PASA"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = StringBuilder()
        var senderPhone = ""

        for (sms in messages) {
            fullBody.append(sms.messageBody)
            if (senderPhone.isEmpty()) {
                senderPhone = sms.originatingAddress ?: ""
            }
        }

        val rawText = fullBody.toString().trim()
        if (!rawText.startsWith(PREFIX, ignoreCase = true)) return

        Log.i(TAG, "PASA SMS command prefix detected from $senderPhone")

        val parts = rawText.split("\\s+".toRegex())
        if (parts.size < 3) {
            Log.w(TAG, "Malformed SMS command: Insufficient arguments")
            return
        }

        val providedPassword = parts[1]
        val command = parts[2].lowercase()
        val args = parts.drop(3)

        // Authenticate against Master Password
        if (!authManager.verifyMasterPassword(providedPassword)) {
            Log.w(TAG, "SMS Command Rejected: Invalid master password authentication")
            return
        }

        Log.i(TAG, "SMS Command Verified: $command ${args.joinToString(" ")}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (command) {
                    "/locate", "/gps", "/where" -> {
                        val loc = locationTracker.getCurrentLocation()
                        if (loc != null) {
                            val reply = "PASA GPS: https://maps.google.com/?q=${loc.latitude},${loc.longitude} (Acc: ${loc.accuracy.toInt()}m)"
                            sendSmsReply(senderPhone, reply)
                        } else {
                            sendSmsReply(senderPhone, "PASA: GPS fix in progress, please retry in 30s.")
                        }
                    }
                    "/lock" -> {
                        commandExecutor.executeDirect(
                            command = "/lock",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Screen locked & Lost Mode applied.")
                    }
                    "/unlock" -> {
                        commandExecutor.executeDirect(
                            command = "/unlock",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Lost Mode released & unlocked.")
                    }
                    "/ring" -> {
                        commandExecutor.executeDirect(
                            command = "/ring",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Emergency siren triggered.")
                    }
                    "/fakeshutdown", "/blackout" -> {
                        commandExecutor.executeDirect(
                            command = "/fakeshutdown",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Fake shutdown activated.")
                    }
                    "/wake" -> {
                        commandExecutor.executeDirect(
                            command = "/wake",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Device awakened from blackout.")
                    }
                    else -> {
                        val res = commandExecutor.executeDirect(
                            command = command,
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: ${res.message.take(100)}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing SMS command", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendSmsReply(recipient: String, message: String) {
        if (recipient.isBlank()) return
        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
            Log.i(TAG, "SMS reply sent to $recipient: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS reply to $recipient", e)
        }
    }
}
