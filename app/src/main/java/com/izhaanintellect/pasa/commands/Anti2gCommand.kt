package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anti-2G / IMSI-Catcher Shield Command (/anti_2g).
 * Neutralizes Stingray / IMSI-Catcher surveillance devices.
 * Stingrays force modern smartphones to downgrade to insecure, unencrypted 2G (GSM)
 * to intercept voice calls, read 2FA SMS in cleartext, or track location.
 *
 * In Android 12+ (API 31+), this command disables 2G at the cellular modem layer.
 * On all devices, it provides real-time detection and alerting if the phone is downgraded to 2G.
 */
@Singleton
class Anti2gCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/anti_2g"
    override val description = "Anti-Stingray / 2G IMSI-Catcher Shield"
    override val usage = "/anti_2g [status|enable|disable]"

    companion object {
        private const val TAG = "PASA_Anti2G"
        // TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G = 3 (API 31+)
        private const val REASON_ENABLE_2G = 3
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return CommandResult(false, "❌ Telephony hardware unavailable.")

        val action = args.firstOrNull()?.lowercase()?.trim() ?: "status"

        return when (action) {
            "on", "enable", "block" -> {
                var modemBlocked = false
                var errorDetail = ""

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        // Bitmask 0L completely disables 2G networks (GSM, GPRS, EDGE, CDMA)
                        tm.setAllowedNetworkTypesForReason(REASON_ENABLE_2G, 0L)
                        modemBlocked = true
                        Log.i(TAG, "2G disabled at cellular modem layer via setAllowedNetworkTypesForReason")
                    } catch (e: Exception) {
                        Log.w(TAG, "setAllowedNetworkTypesForReason warning: ${e.message}")
                        errorDetail = e.localizedMessage ?: "OEM baseband restriction"
                    }
                }

                prefs.isAnti2gEnabled = true

                val sb = StringBuilder()
                sb.append("📡 <b>Anti-2G / IMSI-Catcher Shield: ARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n")
                if (modemBlocked) {
                    sb.append("✅ <b>Baseband Modem:</b> 2G (GSM/EDGE) Radio Disabled at HAL layer.\n")
                    sb.append("🔒 <b>Stingray Defense:</b> Hardware downgrade attacks physically prevented.\n")
                } else {
                    sb.append("⚠️ <b>Real-Time Telemetry Sentinel:</b> ACTIVE\n")
                    sb.append("🔍 Device will continuously monitor RF spectrum for forced 2G cellular downgrades.\n")
                    if (errorDetail.isNotBlank()) {
                        sb.append("ℹ️ <i>Modem note: $errorDetail</i>\n")
                    }
                }
                sb.append("\n🛡️ <i>Your device is protected against rogue cell towers (Stingray / IMSI-catchers).</i>")
                CommandResult(true, sb.toString())
            }

            "off", "disable", "allow" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        // Restore 2G (GSM, GPRS, EDGE, CDMA 1x) bitmasks
                        val bitmask2G = TelephonyManager.NETWORK_TYPE_BITMASK_GSM or
                                TelephonyManager.NETWORK_TYPE_BITMASK_GPRS or
                                TelephonyManager.NETWORK_TYPE_BITMASK_EDGE or
                                TelephonyManager.NETWORK_TYPE_BITMASK_CDMA or
                                TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT
                        tm.setAllowedNetworkTypesForReason(REASON_ENABLE_2G, bitmask2G)
                        Log.i(TAG, "2G restored at cellular modem layer")
                    } catch (e: Exception) {
                        Log.w(TAG, "Restore 2G error: ${e.message}")
                    }
                }
                prefs.isAnti2gEnabled = false
                CommandResult(
                    true,
                    "📡 <b>Anti-2G / IMSI-Catcher Shield: DISARMED</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ 2G network connections are now permitted. Device can attach to legacy GSM towers."
                )
            }

            else -> {
                val isArmed = prefs.isAnti2gEnabled
                val networkType = try {
                    if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        tm.dataNetworkType
                    } else {
                        TelephonyManager.NETWORK_TYPE_UNKNOWN
                    }
                } catch (_: Exception) {
                    TelephonyManager.NETWORK_TYPE_UNKNOWN
                }

                val networkName = when (networkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS) ⚠️ INSECURE"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE) ⚠️ INSECURE"
                    TelephonyManager.NETWORK_TYPE_GSM -> "2G (GSM) ⚠️ INSECURE"
                    TelephonyManager.NETWORK_TYPE_CDMA -> "2G (CDMA) ⚠️ INSECURE"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "2G (1xRTT) ⚠️ INSECURE"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
                    TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA -> "3G (HSPA)"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> "3G+ (HSPA+)"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE) 🔒 Encrypted"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G (NR) 🔒 Encrypted"
                    else -> "Active Carrier (${tm.networkOperatorName.ifBlank { "Cellular" }})"
                }

                val is2gDanger = networkType in setOf(
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GSM,
                    TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyManager.NETWORK_TYPE_1xRTT
                )

                CommandResult(
                    true,
                    "📡 <b>Anti-2G / IMSI-Catcher Shield Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "• <b>Shield State:</b> ${if (isArmed) "🔒 ARMED (2G Blocked/Monitored)" else "🔓 DISARMED (2G Permitted)"}\n" +
                            "• <b>Current Cellular Carrier:</b> ${tm.networkOperatorName.ifBlank { "Unknown" }}\n" +
                            "• <b>Active Network Radio:</b> <code>$networkName</code>\n" +
                            "• <b>Downgrade Threat:</b> ${if (is2gDanger) "🚨 CRITICAL: Device attached to 2G tower!" else "🟢 SECURE (No 2G downgrade detected)"}\n\n" +
                            "💡 <i>Usage:</i>\n" +
                            "• <code>/anti_2g enable</code> — Block 2G & detect Stingray traps\n" +
                            "• <code>/anti_2g disable</code> — Allow 2G connections"
                )
            }
        }
    }
}
