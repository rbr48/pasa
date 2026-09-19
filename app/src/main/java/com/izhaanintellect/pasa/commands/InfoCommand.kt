package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns detailed device specifications, hardware, and OS information.
 */
@Singleton
class InfoCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/info"
    override val description = "Hardware, OS, and system specifications"
    override val usage = "/info"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sb = StringBuilder()
        sb.appendLine("📱 <b>PASA Device Specifications</b>")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        // Hardware
        sb.appendLine()
        sb.appendLine("📌 <b>Device:</b> ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("📌 <b>Brand / Product:</b> ${Build.BRAND} (${Build.PRODUCT})")
        sb.appendLine("📌 <b>Board / Hardware:</b> ${Build.BOARD} / ${Build.HARDWARE}")
        sb.appendLine("💻 <b>Arch / CPU:</b> ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"} (${Runtime.getRuntime().availableProcessors()} cores)")

        // OS
        sb.appendLine()
        sb.appendLine("🤖 <b>Android OS:</b> ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("🔐 <b>Security Patch:</b> ${Build.VERSION.SECURITY_PATCH}")
        sb.appendLine("🏷️ <b>Build Fingerprint:</b> <code>${Build.ID}</code>")

        // Display
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        sb.appendLine()
        sb.appendLine("🖥️ <b>Display:</b> ${metrics.widthPixels} x ${metrics.heightPixels} (${metrics.densityDpi} dpi)")

        // Telephony
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            sb.appendLine()
            sb.appendLine("📞 <b>Network Operator:</b> ${tm.networkOperatorName.ifBlank { "N/A" }}")
            sb.appendLine("📱 <b>SIM Operator:</b> ${tm.simOperatorName.ifBlank { "N/A" }}")
            sb.appendLine("🌍 <b>SIM Country ISO:</b> ${tm.simCountryIso.uppercase().ifBlank { "N/A" }}")
        } catch (e: Exception) {
            sb.appendLine("📞 <b>Telephony:</b> Restricted")
        }

        // Installed packages count
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val userApps = apps.count { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        val systemApps = apps.size - userApps

        sb.appendLine()
        sb.appendLine("📦 <b>Applications:</b> ${apps.size} installed ($userApps user apps, $systemApps system)")

        return CommandResult(success = true, message = sb.toString())
    }
}
