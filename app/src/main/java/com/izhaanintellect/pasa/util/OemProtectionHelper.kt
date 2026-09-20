package com.izhaanintellect.pasa.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * OEM Background Protection & Autostart Survival Helper.
 *
 * Android manufacturers (Xiaomi/HyperOS, Samsung OneUI, Huawei/Honor, Oppo/Realme,
 * Vivo, OnePlus, Transsion) aggressively terminate background services unless the app
 * is explicitly whitelisted in OEM-specific hidden autostart and power managers.
 *
 * This helper detects the manufacturer and opens the direct OEM configuration page.
 */
object OemProtectionHelper {

    private const val TAG = "PASA_OemHelper"

    /**
     * Intent definitions for known aggressive OEM autostart and battery managers.
     */
    private val OEM_AUTOSTART_INTENTS = listOf(
        // Xiaomi / Redmi / POCO (MIUI / HyperOS)
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        // Samsung (Device Care / Battery Usage)
        Intent().setComponent(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        ),
        // Huawei / Honor (Phone Manager Protected Apps)
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            )
        ),
        // Oppo / Realme (ColorOS / Realme UI Startup Manager)
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.oplus.battery",
                "com.oplus.battery.clean.DeepCleanActivity"
            )
        ),
        // Vivo / iQOO (iManager Background Power)
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        // OnePlus (OxygenOS Auto-launch)
        Intent().setComponent(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        ),
        // Transsion (Infinix, Tecno, itel - HiOS / XOS Phone Master)
        Intent().setComponent(
            ComponentName(
                "com.transsion.phonemaster",
                "com.transsion.phonemaster.PhoneMasterActivity"
            )
        )
    )

    /**
     * Attempts to launch the OEM-specific autostart / power management activity.
     * Falls back to standard Android battery optimization settings if no OEM intent resolves.
     */
    fun openOemAutostartSettings(context: Context): Boolean {
        for (intent in OEM_AUTOSTART_INTENTS) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.i(TAG, "Launched OEM autostart activity: ${intent.component?.flattenToString()}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed launching ${intent.component?.className}: ${e.message}")
            }
        }

        // Fallback 1: Standard Ignore Battery Optimization Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.i(TAG, "Launched standard battery optimization settings")
                return true
            } catch (_: Exception) {}
        }

        // Fallback 2: Direct App Info Details
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Launched app details settings")
            return true
        } catch (_: Exception) {}

        return false
    }

    /**
     * Checks if the current device is from an OEM with notoriously aggressive background killers.
     */
    fun isAggressiveOem(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ||
                manufacturer.contains("samsung") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("infinix") ||
                manufacturer.contains("tecno")
    }

    /**
     * Returns a human-friendly name of the OEM-specific autostart protection menu.
     */
    fun getOemProtectionTitle(): String {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> "HyperOS / MIUI Autostart"
            m.contains("samsung") -> "Samsung Never Sleep Apps"
            m.contains("huawei") || m.contains("honor") -> "EMUI App Launch Protection"
            m.contains("oppo") || m.contains("realme") -> "ColorOS Startup Manager"
            m.contains("vivo") -> "Vivo Background Power Consumption"
            m.contains("oneplus") -> "OxygenOS Auto-Launch"
            m.contains("infinix") || m.contains("tecno") -> "Phone Master Auto-Start"
            else -> "Battery Optimization Whitelist"
        }
    }
}
