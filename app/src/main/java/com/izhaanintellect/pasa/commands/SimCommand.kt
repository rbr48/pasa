package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays detailed SIM card information including:
 * - Active SIM slots and count
 * - Carrier names / network operators
 * - Phone numbers (if available on SIM)
 * - Signal strength per SIM
 * - Network type (2G/3G/4G/5G)
 * - MCC/MNC codes
 *
 * Commands:
 *   /sim          — Display all active SIM information
 *   /sim [slot]   — Display specific SIM slot info (1 or 2)
 */
@Singleton
class SimCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/sim"
    override val description = "Display active SIM slots, carrier info, and signal"
    override val usage = "/sim [slot]"

    companion object {
        private const val TAG = "PASA_Sim"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val slotFilter = args.firstOrNull()?.toIntOrNull()

        return try {
            val simInfo = getSIMInformation(slotFilter)
            CommandResult(success = true, message = simInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SIM info: ${e.message}", e)
            CommandResult(
                success = false,
                message = "❌ <b>SIM Information Error</b>\n━━━━━━━━━━━━━━━━━━━━\n<code>${e.message}</code>"
            )
        }
    }

    private fun getSIMInformation(slotFilter: Int?): String {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)

        val activeSubscriptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
        }

        if (activeSubscriptions.isEmpty()) {
            return """
                📱 <b>SIM Card Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⚠️ <b>No active SIM cards detected</b>

                Possible reasons:
                • Device is in airplane mode
                • SIM slot is empty or defective
                • Device is not provisioned
            """.trimIndent()
        }

        // Filter by slot if requested
        val subscriptionsToDisplay = if (slotFilter != null) {
            activeSubscriptions.getOrNull(slotFilter - 1)?.let { listOf(it) } ?: emptyList()
        } else {
            activeSubscriptions
        }

        if (subscriptionsToDisplay.isEmpty()) {
            return """
                📱 <b>SIM Card Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⚠️ <b>SIM slot $slotFilter not found</b>

                Available slots: ${activeSubscriptions.size}
                Valid range: 1-${activeSubscriptions.size}
            """.trimIndent()
        }

        val builder = StringBuilder()
        builder.append("📱 <b>SIM Card Information</b>\n")
        builder.append("━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("🔢 <b>Total Active SIMs:</b> ${activeSubscriptions.size}\n\n")

        subscriptionsToDisplay.forEachIndexed { index, subInfo ->
            val slotNumber = activeSubscriptions.indexOf(subInfo) + 1
            builder.append("<b>📡 SIM Slot $slotNumber</b>\n")

            // Carrier Name
            val carrierName = subInfo.displayName?.toString() ?: "Unknown"
            builder.append("🏢 <b>Carrier:</b> <code>$carrierName</code>\n")

            // Phone Number (if available)
            val phoneNumber = subInfo.number
            if (!phoneNumber.isNullOrBlank()) {
                builder.append("📞 <b>Number:</b> <code>$phoneNumber</code>\n")
            } else {
                builder.append("📞 <b>Number:</b> <i>[Not stored on SIM chip — use /sendsms]</i>\n")
            }

            // MCC/MNC (Mobile Country Code / Mobile Network Code)
            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subInfo.mccString ?: "" else @Suppress("DEPRECATION") subInfo.mcc.toString()
            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subInfo.mncString ?: "" else @Suppress("DEPRECATION") subInfo.mnc.toString()
            val mccMnc = mcc + mnc
            if (mccMnc.isNotBlank()) {
                builder.append("🌍 <b>MCC/MNC:</b> <code>$mccMnc</code>\n")
            }

            // Subscription ID
            builder.append("🔑 <b>Subscription ID:</b> <code>${subInfo.subscriptionId}</code>\n")

            // ICCID (SIM serial number)
            val iccid = subInfo.iccId
            if (!iccid.isNullOrBlank()) {
                builder.append("🔐 <b>ICCID:</b> <code>${iccid.take(10)}...***</code>\n")
            }

            // Signal Strength for this subscription
            val signalStrength = getSignalStrength(subInfo.subscriptionId)
            builder.append("📶 <b>Signal:</b> $signalStrength\n")

            // Network Type
            val networkType = getNetworkType(subInfo.subscriptionId)
            builder.append("📡 <b>Network Type:</b> $networkType\n")

            // ISO Country Code
            val isoCountry = subInfo.countryIso
            if (!isoCountry.isNullOrBlank()) {
                builder.append("🗺️ <b>Country:</b> <code>${isoCountry.uppercase()}</code>\n")
            }

            // SIM State
            val simState = getSimState(subInfo.subscriptionId)
            builder.append("🔓 <b>SIM State:</b> $simState\n")

            if (index < subscriptionsToDisplay.size - 1) {
                builder.append("\n")
            }
        }

        builder.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        builder.append("💡 <b>Discovery Tips:</b>\n")
        builder.append("• <code>/sendsms &lt;your_other_number&gt; ping</code> — Caller ID will display this SIM's exact phone number!\n")
        builder.append("• <code>/sendsms sim1|sim2 &lt;number&gt; &lt;msg&gt;</code> — Force specific SIM\n")
        builder.append("• <code>/tower</code> — Dual-SIM cell tower triangulation")

        return builder.toString()
    }

    private fun getSignalStrength(subscriptionId: Int): String {
        return try {
            val telephonyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getSystemService(TelephonyManager::class.java)
                    ?.createForSubscriptionId(subscriptionId)
                    ?: context.getSystemService(TelephonyManager::class.java)
            } else {
                context.getSystemService(TelephonyManager::class.java)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val signalStrengthObj = telephonyManager?.signalStrength
                if (signalStrengthObj != null) {
                    val level = signalStrengthObj.level // 0-4
                    val bars = "█".repeat((level + 1).coerceAtMost(5)) + "░".repeat((4 - level).coerceAtLeast(0))
                    "<code>$bars</code> (Level $level/4)"
                } else {
                    "⚠️ <i>Unable to read</i>"
                }
            } else {
                "⚠️ <i>Requires READ_PHONE_STATE</i>"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get signal strength: ${e.message}")
            "⚠️ <i>Error</i>"
        }
    }

    private fun getNetworkType(subscriptionId: Int): String {
        return try {
            val telephonyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getSystemService(TelephonyManager::class.java)
                    ?.createForSubscriptionId(subscriptionId)
                    ?: context.getSystemService(TelephonyManager::class.java)
            } else {
                context.getSystemService(TelephonyManager::class.java)
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                when (telephonyManager?.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> "3G HSDPA"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> "3G HSUPA"
                    TelephonyManager.NETWORK_TYPE_CDMA -> "2G CDMA"
                    TelephonyManager.NETWORK_TYPE_EVDO_0 -> "3G EVDO"
                    TelephonyManager.NETWORK_TYPE_EVDO_A -> "3G EVDO-A"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "2G 1xRTT"
                    TelephonyManager.NETWORK_TYPE_IDEN -> "2G iDEN"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    else -> "Active Cellular"
                }
            } else {
                "⚠️ Permission required"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get network type: ${e.message}")
            "⚠️ Error"
        }
    }

    private fun getSimState(subscriptionId: Int): String {
        return try {
            val telephonyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getSystemService(TelephonyManager::class.java)
                    ?.createForSubscriptionId(subscriptionId)
                    ?: context.getSystemService(TelephonyManager::class.java)
            } else {
                context.getSystemService(TelephonyManager::class.java)
            }

            when (telephonyManager?.simState) {
                TelephonyManager.SIM_STATE_READY -> "✅ Ready"
                TelephonyManager.SIM_STATE_ABSENT -> "❌ Absent"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "🔒 PIN Required"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "🔓 PUK Required"
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "🚫 Network Locked"
                TelephonyManager.SIM_STATE_UNKNOWN -> "❓ Unknown"
                else -> "⚠️ Other"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get SIM state: ${e.message}")
            "⚠️ Error"
        }
    }
}
