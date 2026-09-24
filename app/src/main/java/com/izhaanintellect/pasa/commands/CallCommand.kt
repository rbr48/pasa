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
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely dials an outbound phone call via TelecomManager or ACTION_CALL intent.
 * Supports Dual-SIM selection and speakerphone routing.
 *
 * Usage:
 *   /call <number>                     — Call using default SIM, speaker on
 *   /call <number> [sim1|sim2]         — Call using specific SIM slot
 *   /call <number> earpiece            — Call using earpiece instead of speaker
 *   /call <number> sim2 speaker        — SIM 2 + speakerphone
 *   /call status                       — Show available SIM accounts
 */
@Singleton
class CallCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/call"
    override val description = "Remotely place outbound phone call with optional SIM and audio routing selection"
    override val usage = "/call <number> [sim1|sim2] [speaker|earpiece]"

    companion object {
        private const val TAG = "PASA_CallCommand"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.isEmpty()) {
            return showHelp()
        }

        val firstArg = args[0].lowercase().trim()

        // /call status — show available SIM phone accounts
        if (firstArg == "status" || firstArg == "sims" || firstArg == "accounts") {
            return getSimStatus()
        }

        val targetRaw = args[0].trim()

        // Parse optional SIM selector and audio routing from remaining args
        var simSlot: Int? = null        // null = default SIM, 0 = SIM 1, 1 = SIM 2
        var speakerRequested = false
        var earpieceRequested = false

        for (arg in args.drop(1)) {
            when (arg.lowercase().trim()) {
                "sim1", "sim 1", "slot1", "slot0", "1" -> simSlot = 0
                "sim2", "sim 2", "slot2", "slot1", "2" -> simSlot = 1
                "speaker", "handsfree", "loud"         -> speakerRequested = true
                "earpiece", "silent", "quiet"          -> earpieceRequested = true
            }
        }

        // Default: speaker on (useful for remote monitoring)
        val useSpeaker = speakerRequested || !earpieceRequested

        // Sanitize number
        val cleanNumber = targetRaw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (cleanNumber.length < 3) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Phone Number:</b> '<code>$targetRaw</code>' is too short or malformed.\n\nUsage: <code>/call +8801700000000 [sim1|sim2]</code>"
            )
        }

        // Self-heal CALL_PHONE permission via Device Owner
        ensureCallPermission()

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return CommandResult(
                success = false,
                message = "❌ <b>Permission Denied:</b> <code>CALL_PHONE</code> permission is missing.\n" +
                        "If Device Owner is active, run <code>/self_heal</code> to auto-grant."
            )
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val telecomManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            } else null

            val uri = Uri.fromParts("tel", cleanNumber, null)

            // Resolve the PhoneAccountHandle for the requested SIM slot
            val accountHandle: PhoneAccountHandle? = if (simSlot != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resolvePhoneAccount(telecomManager, simSlot)
            } else null

            // Place the call
            if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val extras = Bundle().apply {
                    if (useSpeaker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
                    }
                    // Pin the call to the specific SIM PhoneAccount if one was resolved
                    if (accountHandle != null) {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
                    }
                }
                telecomManager.placeCall(uri, extras)
                Log.i(TAG, "Call placed via TelecomManager to $cleanNumber (simSlot=$simSlot, speaker=$useSpeaker)")
            } else {
                launchCallIntent(cleanNumber, simSlot)
            }

            // Post-dial speakerphone routing
            if (useSpeaker && audioManager != null) {
                delay(1200L)
                try {
                    audioManager.mode = AudioManager.MODE_IN_CALL
                    audioManager.isSpeakerphoneOn = true
                } catch (ae: Exception) {
                    Log.w(TAG, "Speakerphone toggle: ${ae.message}")
                }
            }

            val routeTag = if (useSpeaker) "🔊 Speakerphone" else "📱 Earpiece"
            val simTag   = buildSimTag(simSlot, accountHandle)

            CommandResult(
                success = true,
                message = """
                    📞 <b>Outbound Call Initiated</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    📱 <b>Target:</b> <code>$cleanNumber</code>
                    📡 <b>SIM Slot:</b> $simTag
                    🔈 <b>Audio:</b> $routeTag
                    📶 <b>Status:</b> <b>DIALING VIA CELLULAR RADIO</b>

                    ℹ️ <i>The device is placing the call now.</i>
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

    /**
     * Resolves a PhoneAccountHandle for a specific SIM slot index.
     * On Android M+ with READ_PHONE_STATE, we enumerate call-capable accounts
     * and pick the one matching the subscription at the requested SIM slot.
     */
    @Suppress("DEPRECATION")
    private fun resolvePhoneAccount(
        telecomManager: TelecomManager?,
        slot: Int
    ): PhoneAccountHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || telecomManager == null) return null

        return try {
            // Check READ_PHONE_STATE permission before accessing subscription info
            val hasReadPhone = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasReadPhone) {
                Log.w(TAG, "READ_PHONE_STATE not granted — cannot resolve SIM slot $slot")
                return null
            }

            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subscriptions = subManager?.activeSubscriptionInfoList

            if (subscriptions.isNullOrEmpty()) {
                Log.w(TAG, "No active subscriptions found")
                return null
            }

            // activeSubscriptionInfoList is ordered by SIM slot index
            val targetSub = subscriptions.getOrNull(slot)
            if (targetSub == null) {
                Log.w(TAG, "No subscription at slot $slot (only ${subscriptions.size} SIM(s) active)")
                return null
            }

            val targetSubId = targetSub.subscriptionId

            // Find the call-capable PhoneAccount that maps to this subscriptionId
            val callCapableAccounts = telecomManager.callCapablePhoneAccounts
            for (handle in callCapableAccounts) {
                val account = telecomManager.getPhoneAccount(handle) ?: continue
                // SIM-based accounts have subscriptionId encoded in their id or extras
                val accountIdStr = handle.id
                if (accountIdStr.contains(targetSubId.toString())) {
                    Log.i(TAG, "Resolved PhoneAccount for slot $slot: $handle")
                    return handle
                }
            }

            // Fallback: return the account at index [slot] if count matches
            if (callCapableAccounts.size > slot) {
                val fallback = callCapableAccounts[slot]
                Log.i(TAG, "Using fallback PhoneAccount[$slot]: $fallback")
                return fallback
            }

            Log.w(TAG, "Could not resolve PhoneAccount for slot $slot")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving phone account: ${e.message}")
            null
        }
    }

    private fun launchCallIntent(number: String, simSlot: Int?) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Android <M SIM hint (Samsung/AOSP OEM extras — best-effort)
            if (simSlot != null) {
                putExtra("com.android.phone.extra.slot", simSlot)
                putExtra("slot", simSlot)
                putExtra("simslot", simSlot)
            }
        }
        context.startActivity(intent)
        Log.i(TAG, "Call placed via Intent.ACTION_CALL to $number (simSlot=$simSlot)")
    }

    private fun buildSimTag(slot: Int?, handle: PhoneAccountHandle?): String {
        if (slot == null) return "Default SIM"
        val slotLabel = "SIM ${slot + 1}"
        return if (handle != null) "$slotLabel (resolved)" else "$slotLabel (best-effort)"
    }

    private fun getSimStatus(): CommandResult {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return CommandResult(true, "📡 <b>SIM Status</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>SIM account info requires Android 6.0+.</i>")
            }

            val hasReadPhone = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val subManager = if (hasReadPhone)
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            else null

            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val accounts = telecomManager?.callCapablePhoneAccounts

            val sb = StringBuilder()
            sb.appendLine("📡 <b>Available SIM Phone Accounts</b>")
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

            if (accounts.isNullOrEmpty()) {
                sb.appendLine("<i>No call-capable phone accounts found.</i>")
            } else {
                accounts.forEachIndexed { idx, handle ->
                    val account = telecomManager.getPhoneAccount(handle)
                    val label = account?.label?.toString() ?: handle.id
                    val slotHint = if (idx < (subManager?.activeSubscriptionInfoList?.size ?: 0)) {
                        " — Slot ${idx + 1}"
                    } else ""
                    sb.appendLine("${idx + 1}. 📶 <b>$label</b>$slotHint")
                    sb.appendLine("   Usage: <code>/call &lt;number&gt; sim${idx + 1}</code>")
                }
            }

            // Also list subscriptions
            if (hasReadPhone && subManager != null) {
                val subs = subManager.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    sb.appendLine()
                    sb.appendLine("📱 <b>Active Subscriptions:</b>")
                    subs.forEachIndexed { idx, sub ->
                        sb.appendLine("• SIM ${idx + 1}: <b>${sub.displayName}</b> (${sub.carrierName}) — Sub ID: <code>${sub.subscriptionId}</code>")
                    }
                }
            }

            sb.appendLine()
            sb.append("💡 Usage: <code>/call +8801700000000 sim1 speaker</code>")

            CommandResult(true, sb.toString().trimEnd())
        } catch (e: Exception) {
            CommandResult(false, "❌ Failed to read SIM accounts: ${e.message}")
        }
    }

    private fun showHelp(): CommandResult {
        return CommandResult(
            success = false,
            message = """
                📞 <b>Remote Outbound Calling</b>
                ━━━━━━━━━━━━━━━━━━━━
                Place an immediate phone call from the device's cellular radio.

                ⚡ <b>Usage:</b>
                • <code>/call &lt;number&gt;</code>                  — Default SIM, speakerphone
                • <code>/call &lt;number&gt; sim1</code>             — Force SIM 1
                • <code>/call &lt;number&gt; sim2</code>             — Force SIM 2
                • <code>/call &lt;number&gt; earpiece</code>         — Route to earpiece
                • <code>/call &lt;number&gt; sim2 speaker</code>     — SIM 2 + speakerphone
                • <code>/call status</code>                    — Show available SIM accounts

                <b>Examples:</b>
                • <code>/call +8801700000000</code>
                • <code>/call +8801700000000 sim2</code>
                • <code>/call +1234567890 sim1 speaker</code>

                💡 <i>Speaker is default. Use <code>earpiece</code> for silent dialing.</i>
            """.trimIndent()
        )
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
