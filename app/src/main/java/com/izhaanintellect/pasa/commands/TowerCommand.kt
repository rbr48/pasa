package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import android.telephony.*
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts live cellular base station / cell tower telemetry across all active SIMs.
 * Enables indoor positioning and triangulation when GPS satellites are blocked by concrete/basements.
 */
@Singleton
class TowerCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/tower"
    override val description = "Extract live cell tower telemetry & indoor triangulation"
    override val usage = "/tower"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        // Ensure permissions are locked if Device Owner
        if (PasaDeviceAdmin.isDeviceOwner(context)) {
            try {
                PasaDeviceAdmin.selfHealPermissions(context)
            } catch (_: Exception) {}
        }

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return CommandResult(false, "❌ TelephonyManager service unavailable.")

        val networkOperatorName = tm.networkOperatorName.ifBlank { "Unknown Carrier" }
        val networkTypeStr = getNetworkTypeName(tm)

        val cellInfoList: List<CellInfo>? = try {
            tm.allCellInfo
        } catch (e: SecurityException) {
            return CommandResult(false, "❌ Telephony permission required to scan cell towers: ${e.message}")
        } catch (e: Exception) {
            return CommandResult(false, "❌ Error scanning cell towers: ${e.message}")
        }

        if (cellInfoList.isNullOrEmpty()) {
            return CommandResult(
                success = true,
                message = "🗼 <b>Cellular Network Info</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Carrier: <b>$networkOperatorName</b>\n" +
                        "• Radio Tech: <b>$networkTypeStr</b>\n" +
                        "• Cell Tower: <i>No neighboring base station telemetry returned by modem.</i>\n\n" +
                        "💡 <i>Try again in a few seconds or when cellular signal stabilizes.</i>"
            )
        }

        val sb = StringBuilder()
        sb.append("🗼 <b>Live Cell Tower Telemetry</b>\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📡 <b>Primary Carrier:</b> $networkOperatorName ($networkTypeStr)\n\n")

        for (info in cellInfoList) {
            val isRegistered = info.isRegistered

            when (info) {
                is CellInfoLte -> {
                    val id = info.cellIdentity
                    val ss = info.cellSignalStrength
                    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString ?: "${id.mcc}" else "${id.mcc}"
                    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString ?: "${id.mnc}" else "${id.mnc}"
                    val ci = id.ci
                    val tac = id.tac
                    val pci = id.pci
                    val dbm = ss.dbm

                    if (ci != Int.MAX_VALUE && ci != -1) {
                        sb.append(if (isRegistered) "🔹 <b>Active LTE Cell:</b>\n" else "▫️ <b>Neighbor LTE Cell:</b>\n")
                        sb.append("   • MCC/MNC: <code>$mcc/$mnc</code>\n")
                        sb.append("   • Cell ID (CI): <code>$ci</code>\n")
                        sb.append("   • TAC: <code>$tac</code> | PCI: <code>$pci</code>\n")
                        sb.append("   • Signal: <b>$dbm dBm</b>\n")
                        if (isRegistered) {
                            sb.append("   🗺️ <a href=\"https://www.cellmapper.net/map?MCC=$mcc&MNC=$mnc\">View CellMapper</a>\n")
                        }
                        sb.append("\n")
                    }
                }

                is CellInfoGsm -> {
                    val id = info.cellIdentity
                    val ss = info.cellSignalStrength
                    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString ?: "${id.mcc}" else "${id.mcc}"
                    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString ?: "${id.mnc}" else "${id.mnc}"
                    val cid = id.cid
                    val lac = id.lac
                    val dbm = ss.dbm

                    if (cid != Int.MAX_VALUE && cid != -1) {
                        sb.append(if (isRegistered) "🔹 <b>Active GSM 2G/3G Cell:</b>\n" else "▫️ <b>Neighbor GSM Cell:</b>\n")
                        sb.append("   • MCC/MNC: <code>$mcc/$mnc</code>\n")
                        sb.append("   • CID: <code>$cid</code> | LAC: <code>$lac</code>\n")
                        sb.append("   • Signal: <b>$dbm dBm</b>\n\n")
                    }
                }

                is CellInfoWcdma -> {
                    val id = info.cellIdentity
                    val ss = info.cellSignalStrength
                    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString ?: "${id.mcc}" else "${id.mcc}"
                    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString ?: "${id.mnc}" else "${id.mnc}"
                    val cid = id.cid
                    val lac = id.lac
                    val dbm = ss.dbm

                    if (cid != Int.MAX_VALUE && cid != -1) {
                        sb.append(if (isRegistered) "🔹 <b>Active WCDMA 3G Cell:</b>\n" else "▫️ <b>Neighbor WCDMA Cell:</b>\n")
                        sb.append("   • MCC/MNC: <code>$mcc/$mnc</code>\n")
                        sb.append("   • CID: <code>$cid</code> | LAC: <code>$lac</code>\n")
                        sb.append("   • Signal: <b>$dbm dBm</b>\n\n")
                    }
                }

                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                        val ss = info.cellSignalStrength
                        sb.append("🔹 <b>Active 5G New Radio (NR) Cell:</b>\n")
                        sb.append("   • Signal: <b>${ss.dbm} dBm</b>\n\n")
                    }
                }
            }
        }

        sb.append("🛡️ <i>Cell tower telemetry operational for indoor positioning.</i>")
        return CommandResult(success = true, message = sb.toString())
    }

    private fun getNetworkTypeName(tm: TelephonyManager): String {
        return try {
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_UMTS -> "3G HSPA/UMTS"
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G EDGE/GPRS"
                else -> "Cellular"
            }
        } catch (_: Exception) { "Cellular" }
    }
}
