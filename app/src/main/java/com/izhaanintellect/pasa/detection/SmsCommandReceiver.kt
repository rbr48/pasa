package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
 * Format: PASA <credential> <command> <args...>
 * where credential is the current 6-digit TOTP code (preferred; enroll via
 * /smssetup) or the master PIN/password.
 *
 * Example: PASA 493021 /locate
 * Example: PASA 493021 /location
 * Example: PASA 493021 /status
 * Example: PASA 493021 /lock 5892 Lost phone
 * Example: PASA 493021 /ring 30
 */
@AndroidEntryPoint
class SmsCommandReceiver : BroadcastReceiver() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var commandExecutor: CommandExecutor
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var licenseManager: com.izhaanintellect.pasa.security.LicenseManager

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

        // Extract carrier subscription ID for dual-SIM matched outgoing replies
        val subId = intent.getIntExtra("subscription", -1).let {
            if (it != -1) it else intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
        }

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

        Log.i(TAG, "PASA SMS command prefix detected from $senderPhone (subId: $subId)")

        // 0. Rate limiting protection against brute force
        if (isRateLimited(senderPhone)) {
            Log.w(TAG, "SMS Command Dropped: Sender $senderPhone is temporarily rate-limited.")
            return
        }

        val bodyWithoutPrefix = rawText.replaceFirst("^(?i)PASA[:\\s]+".toRegex(), "").trim()

        // Cryptographic Air-Gapped SMS License Renewal (PASA LIC <compactPayload>)
        if (bodyWithoutPrefix.startsWith("LIC", ignoreCase = true)) {
            val payload = bodyWithoutPrefix.substring(3).trim()
            val (ok, message) = licenseManager.renewViaSmsPayload(payload)
            sendSmsReply(
                context = context,
                recipient = senderPhone,
                message = if (ok) "PASA: $message" else "PASA: License renewal failed ($message)",
                subId = subId
            )
            return
        }

        val parts = bodyWithoutPrefix.split("\\s+".toRegex())
        if (parts.size < 2) {
            Log.w(TAG, "Malformed SMS command: Insufficient arguments after prefix. Raw: $rawText")
            return
        }

        val providedCredential = parts[0].trim()
        val rawCmd = parts[1].lowercase().trim()
        val baseCmd = if (rawCmd.startsWith("/")) rawCmd else "/$rawCmd"
        // Normalize common aliases (e.g. /location -> /locate, status -> /status)
        val command = when (baseCmd) {
            "/location" -> "/locate"
            "/battery" -> "/status"
            "/reset_pin", "/set_pin", "/master_pin" -> "/set_master_pin"
            else -> baseCmd
        }
        val args = parts.drop(2)

        // Authenticate: Support BOTH TOTP (with expanded 3-step +/- 90s latency tolerance)
        // AND Master Password fallback at all times so owners are never locked out.
        val totpSecret = preferencesManager.smsTotpSecret
        var usedMasterPassword = false

        val isTotpValid = totpSecret.isNotBlank() && Totp.verify(totpSecret, providedCredential, window = 3)
        val isMasterPassValid = authManager.verifyMasterPassword(providedCredential)

        val authorized = when {
            isTotpValid -> true
            isMasterPassValid -> {
                Log.i(TAG, "SMS Command authenticated via Master Password")
                usedMasterPassword = true
                true
            }
            else -> {
                Log.w(TAG, "SMS Command Rejected: invalid credential from $senderPhone")
                false
            }
        }

        if (!authorized) {
            recordFailure(senderPhone)
            val attemptsList = failedAttempts[senderPhone]
            val failCount = attemptsList?.size ?: 1
            // Give clear feedback on initial failures so user knows why it didn't execute
            if (failCount <= 2) {
                sendSmsReply(
                    context = context,
                    recipient = senderPhone,
                    message = "PASA: Authentication failed. Verify 6-digit TOTP or Master PIN (Attempt $failCount/$MAX_FAILED_ATTEMPTS)",
                    subId = subId
                )
            }
            return
        }

        recordSuccess(senderPhone)
        Log.i(TAG, "SMS Command Verified: $command ${args.joinToString(" ")}")

        val warningSuffix = if (usedMasterPassword && totpSecret.isBlank()) "\n[Tip: Run /smssetup in Telegram to enroll in 6-digit TOTP]" else ""

        val isExemptCmd = command == "/license" || command == "/pro" || command == "/info"
        if (!isExemptCmd && licenseManager.isAllFeaturesLocked()) {
            sendSmsReply(
                context = context,
                recipient = senderPhone,
                message = "PASA: 7-day trial expired. All features locked. Activate Pro license at pasa.izhaanintellect.fun or via Telegram /license.",
                subId = subId
            )
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (command) {
                    "/locate", "/gps", "/where" -> {
                        // If Device Owner is active, forcibly power on the GNSS hardware receiver
                        if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                            try {
                                com.izhaanintellect.pasa.admin.PasaDeviceAdmin.forceLocationHardware(context, true)
                            } catch (_: Exception) {}
                        }

                        // Fast location retrieval: attempt fresh fix, or fallback to cached fix if needed
                        val freshLoc = locationTracker.getCurrentLocation()
                        val loc = freshLoc ?: locationTracker.getLastKnownLocation()

                        if (loc != null) {
                            val ageSec = (System.currentTimeMillis() - loc.time) / 1000L
                            val ageTag = if (ageSec > 120L) " [Fix: ${ageSec / 60}m ago]" else ""
                            val reply = "PASA GPS: https://maps.google.com/?q=${loc.latitude},${loc.longitude} (Acc: ${loc.accuracy.toInt()}m$ageTag)$warningSuffix"
                            sendSmsReply(context, senderPhone, reply, subId)
                        } else {
                            sendSmsReply(context, senderPhone, "PASA: Unable to resolve GPS fix. Verify Location is enabled in phone settings.$warningSuffix", subId)
                        }
                    }
                    "/usb_lock", "/usb_data" -> {
                        val res = commandExecutor.executeDirect("/usb_lock", listOf(providedCredential) + args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/camera_lock" -> {
                        val res = commandExecutor.executeDirect("/camera_lock", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/bluetooth_lock" -> {
                        val res = commandExecutor.executeDirect("/bluetooth_lock", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/mic_mute" -> {
                        val res = commandExecutor.executeDirect("/mic_mute", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/lockscreen_info" -> {
                        val res = commandExecutor.executeDirect("/lockscreen_info", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/autolock" -> {
                        val res = commandExecutor.executeDirect("/autolock", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/wifi_connect" -> {
                        val res = commandExecutor.executeDirect("/wifi_connect", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/security_audit" -> {
                        val res = commandExecutor.executeDirect("/security_audit", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg.take(300), subId)
                    }
                    "/autostart" -> {
                        val res = commandExecutor.executeDirect("/autostart", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg.take(300), subId)
                    }
                    "/license_renew", "/license_sms" -> {
                        val payload = args.joinToString(" ").trim()
                        val (ok, message) = licenseManager.renewViaSmsPayload(payload)
                        sendSmsReply(
                            context = context,
                            recipient = senderPhone,
                            message = if (ok) "PASA: $message" else "PASA: License renewal failed ($message)",
                            subId = subId
                        )
                    }
                    "/app_uninstall" -> {
                        val res = commandExecutor.executeDirect("/app_uninstall", listOf(providedCredential) + args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/call", "/dial" -> {
                        val res = commandExecutor.executeDirect("/call", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/lock_app" -> {
                        val res = commandExecutor.executeDirect("/lock_app", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/unlock_app" -> {
                        val res = commandExecutor.executeDirect("/unlock_app", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/reboot" -> {
                        val res = commandExecutor.executeDirect("/reboot", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/antitamper" -> {
                        val res = commandExecutor.executeDirect("/antitamper", listOf(providedCredential) + args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/sim_tray_lock" -> {
                        val res = commandExecutor.executeDirect("/sim_tray_lock", listOf(providedCredential) + args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/biometrics" -> {
                        val res = commandExecutor.executeDirect("/biometrics", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/deadman", "/dead_man" -> {
                        val res = commandExecutor.executeDirect("/deadman", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/thermal" -> {
                        val res = commandExecutor.executeDirect("/thermal", args, preferencesManager.ownerChatIdLong)
                        val cleanMsg = android.text.Html.fromHtml(res.message, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        sendSmsReply(context, senderPhone, cleanMsg, subId)
                    }
                    "/status" -> {
                        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                        val battery = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                        val isCharging = bm?.isCharging ?: false
                        val chargeStr = if (isCharging) "Charging" else "Battery"
                        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                        val isLocked = km?.isDeviceLocked == true
                        val lockStr = if (isLocked) "Locked" else "Unlocked"
                        val isDo = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)
                        val doStr = if (isDo) "Owner: Active" else "Owner: Inactive"
                        val reply = "PASA Status:\n• $chargeStr: $battery%\n• Screen: $lockStr\n• $doStr$warningSuffix"
                        sendSmsReply(context, senderPhone, reply, subId)
                    }
                    "/help" -> {
                        val reply = "PASA SMS Commands:\n" +
                                "• PASA <pin> /locate\n" +
                                "• PASA <pin> /status\n" +
                                "• PASA <pin> /usb_lock [on|off]\n" +
                                "• PASA <pin> /camera_lock [on|off]\n" +
                                "• PASA <pin> /bluetooth_lock [on|off]\n" +
                                "• PASA <pin> /mic_mute [on|off]\n" +
                                "• PASA <pin> /wifi_connect <ssid> [pass]\n" +
                                "• PASA <pin> /lockscreen_info <msg>\n" +
                                "• PASA <pin> /autolock <sec>\n" +
                                "• PASA <pin> /deadman [enable|disable|status]\n" +
                                "• PASA <pin> /thermal [on|off|status]\n" +
                                "• PASA <pin> /app_uninstall <pkg>\n" +
                                "• PASA <pin> /reboot\n" +
                                "• PASA <pin> /antitamper [on|off]\n" +
                                "• PASA <pin> /biometrics [on|off]\n" +
                                "• PASA <pin> /ring [sec]\n" +
                                "• PASA <pin> /lock [pin]\n" +
                                "• PASA <pin> /unlock\n" +
                                "• PASA <pin> /fakeshutdown\n" +
                                "• PASA <pin> /wake\n" +
                                "• PASA <pin> /set_master_pin <pin>"
                        sendSmsReply(context, senderPhone, reply, subId)
                    }
                    "/set_master_pin" -> {
                        val newPin = args.firstOrNull()?.trim()
                        if (!newPin.isNullOrBlank() && newPin.length in 4..32) {
                            authManager.setMasterPassword(newPin)
                            sendSmsReply(context, senderPhone, "PASA: Master PIN updated to: $newPin", subId)
                        } else {
                            sendSmsReply(context, senderPhone, "PASA: Invalid PIN. Usage: PASA <credential> /set_master_pin <4-32 characters>", subId)
                        }
                    }
                    "/lock" -> {
                        commandExecutor.executeDirect(
                            command = "/lock",
                            args = listOf(providedCredential) + args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: Screen locked & Lost Mode applied.$warningSuffix", subId)
                    }
                    "/unlock" -> {
                        commandExecutor.executeDirect(
                            command = "/unlock",
                            args = listOf(providedCredential) + args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: Lost Mode released & unlocked.$warningSuffix", subId)
                    }
                    "/ring" -> {
                        commandExecutor.executeDirect(
                            command = "/ring",
                            args = if (args.isNotEmpty()) args else listOf("60"),
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        val sec = args.firstOrNull() ?: "60"
                        sendSmsReply(context, senderPhone, "PASA: Emergency siren triggered (${sec}s).$warningSuffix", subId)
                    }
                    "/snap", "/photo" -> {
                        commandExecutor.executeDirect(
                            command = "/snap",
                            args = if (args.isNotEmpty()) args else listOf("front"),
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: Camera capture complete. Dispatched to Telegram.$warningSuffix", subId)
                    }
                    "/fakeshutdown", "/blackout" -> {
                        commandExecutor.executeDirect(
                            command = "/fakeshutdown",
                            args = listOf(providedCredential) + args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: Fake shutdown activated.$warningSuffix", subId)
                    }
                    "/wake" -> {
                        commandExecutor.executeDirect(
                            command = "/wake",
                            args = listOf(providedCredential) + args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: Device awakened from blackout.$warningSuffix", subId)
                    }
                    "/wipe" -> {
                        pendingWipeTimestamp = System.currentTimeMillis()
                        pendingWipeSender = senderPhone
                        sendSmsReply(
                            context = context,
                            recipient = senderPhone,
                            message = "⚠️ DANGER: Remote factory reset requested! Reply within 60s with: PASA <credential> /wipe_confirm <master_password>$warningSuffix",
                            subId = subId
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
                            sendSmsReply(context, senderPhone, "PASA: ${res.message.take(120)}", subId)
                        } else {
                            sendSmsReply(context, senderPhone, "PASA: Wipe request expired or not initiated. Send /wipe first.", subId)
                        }
                    }
                    else -> {
                        val res = commandExecutor.executeDirect(
                            command = command,
                            args = args,
                            chatId = preferencesManager.ownerChatIdLong
                        )
                        sendSmsReply(context, senderPhone, "PASA: ${res.message.take(100)}$warningSuffix", subId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing SMS command", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendSmsReply(context: Context, recipient: String, message: String, subId: Int = -1) {
        if (recipient.isBlank()) return
        try {
            @Suppress("DEPRECATION")
            val smsManager: SmsManager = if (subId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)?.createForSubscriptionId(subId)
                        ?: SmsManager.getSmsManagerForSubscriptionId(subId)
                } else {
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    SmsManager.getDefault()
                }
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
            Log.i(TAG, "SMS reply sent to $recipient via subId $subId: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS reply to $recipient via subId $subId", e)
            // Fallback to default SmsManager if subId failed
            try {
                @Suppress("DEPRECATION")
                val defaultSm = SmsManager.getDefault()
                val parts = defaultSm.divideMessage(message)
                defaultSm.sendMultipartTextMessage(recipient, null, parts, null, null)
                Log.i(TAG, "Fallback default SMS reply sent to $recipient")
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback default SmsManager also failed", e2)
            }
        }
    }
}
