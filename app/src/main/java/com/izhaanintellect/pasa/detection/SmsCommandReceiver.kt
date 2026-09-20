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
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Out-of-band offline command receiver via GSM SMS messages.
 * Format: PASA (credential) (command) (args...)
 * where credential is the current 6-digit TOTP code (preferred; enroll via
 * /smssetup) or, until TOTP is enrolled, the master password (deprecated).
 * Example: PASA 493021 /locate
 * Example: PASA 493021 /lock 5892 Lost phone
 * Example: PASA 493021 /ring
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
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val FAILED_WINDOW_MS = 15 * 60 * 1000L // 15 mins
        private const val WIPE_CONFIRM_WINDOW_MS = 60 * 1000L // 60s

        private val failedAttempts = java.util.concurrent.ConcurrentHashMap<String, MutableList<Long>>()
        @Volatile private var pendingWipeTimestamp = 0L
        @Volatile private var pendingWipeSender = ""

        private fun isRateLimited(sender: String): Boolean {
            val now = System.currentTimeMillis()
            val attempts = failedAttempts[sender] ?: return false
            synchronized(attempts) {
                attempts.removeAll { now - it > FAILED_WINDOW_MS }
                return attempts.size >= MAX_FAILED_ATTEMPTS
            }
        }

        private fun recordFailure(sender: String) {
            val now = System.currentTimeMillis()
            val attempts = failedAttempts.computeIfAbsent(sender) { mutableListOf() }
            synchronized(attempts) {
                attempts.removeAll { now - it > FAILED_WINDOW_MS }
                attempts.add(now)
            }
        }

        private fun recordSuccess(sender: String) {
            failedAttempts.remove(sender)
        }
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

        // 0. Rate limiting protection against brute force
        if (isRateLimited(senderPhone)) {
            Log.w(TAG, "SMS Command Dropped: Sender $senderPhone is temporarily rate-limited.")
            return
        }

        val parts = rawText.split("\\s+".toRegex())
        if (parts.size < 3) {
            Log.w(TAG, "Malformed SMS command: Insufficient arguments")
            return
        }

        val providedCredential = parts[1]
        val rawCmd = parts[2].lowercase()
        val command = if (rawCmd.startsWith("/")) rawCmd else "/$rawCmd"
        val args = parts.drop(3)

        // Authenticate: prefer a TOTP code (never exposes the master password over
        // SMS). Fall back to the master password only if TOTP is not yet enrolled,
        // so existing setups keep working until the owner runs /smssetup.
        val totpSecret = preferencesManager.smsTotpSecret
        var usedMasterPassword = false

        val authorized = when {
            totpSecret.isNotBlank() -> Totp.verify(totpSecret, providedCredential).also {
                if (!it) Log.w(TAG, "SMS Command Rejected: invalid TOTP code")
            }
            authManager.verifyMasterPassword(providedCredential) -> {
                Log.w(TAG, "SMS authenticated with master password (deprecated). Run /smssetup to switch to TOTP.")
                usedMasterPassword = true
                true
            }
            else -> {
                Log.w(TAG, "SMS Command Rejected: invalid credential")
                false
            }
        }

        if (!authorized) {
            recordFailure(senderPhone)
            return
        }

        recordSuccess(senderPhone)
        Log.i(TAG, "SMS Command Verified: $command ${args.joinToString(" ")}")

        val warningSuffix = if (usedMasterPassword) "\n[Notice: SMS pwd auth is deprecated. Use /smssetup for TOTP]" else ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (command) {
                    "/locate", "/gps", "/where" -> {
                        val loc = locationTracker.getCurrentLocation()
                        if (loc != null) {
                            val reply = "PASA GPS: https://maps.google.com/?q=${loc.latitude},${loc.longitude} (Acc: ${loc.accuracy.toInt()}m)$warningSuffix"
                            sendSmsReply(senderPhone, reply)
                        } else {
                            sendSmsReply(senderPhone, "PASA: GPS fix in progress, please retry in 30s.$warningSuffix")
                        }
                    }
                    "/lock" -> {
                        commandExecutor.executeDirect(
                            command = "/lock",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Screen locked & Lost Mode applied.$warningSuffix")
                    }
                    "/unlock" -> {
                        commandExecutor.executeDirect(
                            command = "/unlock",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Lost Mode released & unlocked.$warningSuffix")
                    }
                    "/ring" -> {
                        commandExecutor.executeDirect(
                            command = "/ring",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Emergency siren triggered.$warningSuffix")
                    }
                    "/snap", "/photo" -> {
                        commandExecutor.executeDirect(
                            command = "/snap",
                            args = if (args.isNotEmpty()) args else listOf("front"),
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Camera capture complete. Dispatched to Telegram.$warningSuffix")
                    }
                    "/fakeshutdown", "/blackout" -> {
                        commandExecutor.executeDirect(
                            command = "/fakeshutdown",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Fake shutdown activated.$warningSuffix")
                    }
                    "/wake" -> {
                        commandExecutor.executeDirect(
                            command = "/wake",
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: Device awakened from blackout.$warningSuffix")
                    }
                    "/wipe" -> {
                        pendingWipeTimestamp = System.currentTimeMillis()
                        pendingWipeSender = senderPhone
                        sendSmsReply(
                            senderPhone,
                            "⚠️ DANGER: Remote factory reset requested! Reply within 60s with: PASA <credential> /wipe_confirm <master_password>$warningSuffix"
                        )
                    }
                    "/wipe_confirm" -> {
                        val elapsed = System.currentTimeMillis() - pendingWipeTimestamp
                        if (elapsed <= WIPE_CONFIRM_WINDOW_MS && pendingWipeSender == senderPhone) {
                            pendingWipeTimestamp = 0L
                            pendingWipeSender = ""
                            val res = commandExecutor.executeDirect(
                                command = "/wipe_confirm",
                                args = args,
                                chatId = preferencesManager.ownerChatIdLong
                            )
                            sendSmsReply(senderPhone, "PASA: ${res.message.take(120)}")
                        } else {
                            sendSmsReply(senderPhone, "PASA: Wipe request expired or not initiated. Send /wipe first.")
                        }
                    }
                    else -> {
                        val res = commandExecutor.executeDirect(
                            command = command,
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(senderPhone, "PASA: ${res.message.take(100)}$warningSuffix")
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
