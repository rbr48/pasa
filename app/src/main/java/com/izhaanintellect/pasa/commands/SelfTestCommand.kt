package com.izhaanintellect.pasa.commands

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.izhaanintellect.pasa.accessibility.AccessibilityScreenCaptureService
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.camera.ScreenshotManager
import com.izhaanintellect.pasa.crypto.DeviceIdentity
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.util.OemProtectionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `/selftest` — Comprehensive automated hardware, cryptographic, and OS health audit.
 *
 * Runs an on-demand 9-point system verification:
 * 1. 📍 GNSS Location & Satellite Engines (Fused + AOSP)
 * 2. 👁️ Accessibility Screen Engine
 * 3. 🛡️ Device Policy Lockdown (Admin & Owner)
 * 4. 🔐 Hardware Cryptography (TEE / StrongBox)
 * 5. 📸 Visual Sensors (Front + Rear cameras)
 * 6. 🎙️ Acoustic Sensors & Dynamic FGS
 * 7. 🔋 Background Doze & OEM Killer Whitelist
 * 8. 📶 GSM Radio & SMS Out-of-Band Fallback
 * 9. 🌐 C2 Cloud Control Plane & License Status
 */
@Singleton
class SelfTestCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val pasaBackendApi: PasaBackendApi,
    private val screenshotManager: ScreenshotManager
) : Command {

    override val name = "/selftest"
    override val description = "Run comprehensive 9-point hardware & security health audit"
    override val usage = "/selftest"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        var passedPoints = 0
        val totalPoints = 9

        val report = StringBuilder()
        report.append("🛡️ <b>PASA Sentinel System Health Audit</b>\n")
        report.append("━━━━━━━━━━━━━━━━━━━━\n")

        // 1. GNSS Location
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val netEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocPerm && (gpsEnabled || netEnabled)) {
            passedPoints++
            val providers = listOfNotNull(
                if (gpsEnabled) "GPS" else null,
                if (netEnabled) "Cell/WiFi" else null
            ).joinToString("+")
            report.append("📍 <b>GNSS Location:</b> ✅ Operational ($providers)\n")
        } else {
            report.append("📍 <b>GNSS Location:</b> ⚠️ Degraded (${if (!hasLocPerm) "No Permission" else "GPS Disabled"})\n")
        }

        // 2. Accessibility Screen Engine
        val a11yEnabled = screenshotManager.isAccessibilityServiceEnabled()
        val a11yBound = AccessibilityScreenCaptureService.instance != null
        if (a11yEnabled) {
            passedPoints++
            val status = if (a11yBound) "Active & Bound" else "Enabled (Connecting)"
            report.append("👁️ <b>Screen Capture:</b> ✅ $status\n")
        } else {
            report.append("👁️ <b>Screen Capture:</b> ❌ Disabled (Enable in Accessibility)\n")
        }

        // 3. Device Policy Lockdown
        val isAdmin = PasaDeviceAdmin.isAdminActive(context)
        val isOwner = PasaDeviceAdmin.isDeviceOwner(context)
        if (isOwner) {
            passedPoints++
            report.append("👑 <b>Device Policy:</b> ✅ Device Owner (Knox-grade)\n")
        } else if (isAdmin) {
            passedPoints++
            report.append("👑 <b>Device Policy:</b> ✅ Device Admin (Standard)\n")
        } else {
            report.append("👑 <b>Device Policy:</b> ⚠️ Inactive (Run /device_owner)\n")
        }

        // 4. Hardware Cryptography & Keystore
        val keyExists = DeviceIdentity.exists()
        val secLevel = DeviceIdentity.securityLevel()
        if (keyExists && secLevel != "UNKNOWN") {
            passedPoints++
            report.append("🔐 <b>Cryptography:</b> ✅ Active ($secLevel Keystore)\n")
        } else {
            report.append("🔐 <b>Cryptography:</b> ⚠️ Software Fallback ($secLevel)\n")
        }

        // 5. Camera Sensors
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val camList = runCatching { cm?.cameraIdList ?: emptyArray() }.getOrDefault(emptyArray())
        val hasCamPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCamPerm && camList.size >= 2) {
            passedPoints++
            report.append("📸 <b>Camera Sensors:</b> ✅ Front + Rear Ready (${camList.size} IDs)\n")
        } else if (hasCamPerm && camList.isNotEmpty()) {
            passedPoints++
            report.append("📸 <b>Camera Sensors:</b> ⚠️ Monocular Ready (${camList.size} ID)\n")
        } else {
            report.append("📸 <b>Camera Sensors:</b> ❌ Missing Permission\n")
        }

        // 6. Microphone & Audio
        val hasMicFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val hasAudioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMicFeature && hasAudioPerm) {
            passedPoints++
            report.append("🎙️ <b>Audio Microphone:</b> ✅ Dynamic FGS Ready\n")
        } else {
            report.append("🎙️ <b>Audio Microphone:</b> ⚠️ ${if (!hasAudioPerm) "No Permission" else "No Hardware"}\n")
        }

        // 7. Battery Whitelist & OEM Survival
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isDozeIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
        val isAggressiveOem = OemProtectionHelper.isAggressiveOem()
        if (isDozeIgnored) {
            passedPoints++
            report.append("🔋 <b>Doze Whitelist:</b> ✅ Unrestricted" + (if (isAggressiveOem) " (${Build.MANUFACTURER})" else "") + "\n")
        } else {
            report.append("🔋 <b>Doze Whitelist:</b> ⚠️ Optimized (May sleep in background)\n")
        }

        // 8. GSM Cellular & SMS Radio
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simReady = tm?.simState == TelephonyManager.SIM_STATE_READY
        val hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        if (simReady && hasSmsPerm) {
            passedPoints++
            report.append("📶 <b>GSM SMS Radio:</b> ✅ SIM Ready (Air-gap active)\n")
        } else if (simReady) {
            passedPoints++
            report.append("📶 <b>GSM SMS Radio:</b> ⚠️ SIM Ready (SMS Perm Pending)\n")
        } else {
            report.append("📶 <b>GSM SMS Radio:</b> ⚠️ No SIM Card Detected\n")
        }

        // 9. C2 Cloud Gateway & License
        val startPing = System.currentTimeMillis()
        val c2Check = withContext(Dispatchers.IO) {
            runCatching {
                pasaBackendApi.checkLicense(preferencesManager.deviceId)
            }.getOrNull()
        }
        val latencyMs = System.currentTimeMillis() - startPing

        if (c2Check != null && c2Check.ok) {
            passedPoints++
            val tier = c2Check.tier ?: preferencesManager.licenseTier
            report.append("🌐 <b>C2 Control Plane:</b> ✅ Connected (${latencyMs}ms | $tier)\n")
        } else {
            report.append("🌐 <b>C2 Control Plane:</b> ⚠️ Unreachable (${latencyMs}ms)\n")
        }

        // Score calculation
        val scorePercent = ((passedPoints.toDouble() / totalPoints) * 100).toInt()
        val ratingBadge = when {
            scorePercent >= 90 -> "🟢 <b>RATING: 9.8 / 10 (Maximum Sovereign Defense)</b>"
            scorePercent >= 75 -> "🟡 <b>RATING: 8.5 / 10 (Strong Defense)</b>"
            else -> "🔴 <b>RATING: 6.5 / 10 (Action Needed)</b>"
        }

        report.append("━━━━━━━━━━━━━━━━━━━━\n")
        report.append("📊 <b>Audit Score:</b> $passedPoints/$totalPoints tests passed ($scorePercent%)\n")
        report.append("$ratingBadge\n\n")

        if (scorePercent < 100) {
            report.append("💡 <i>Recommendations to achieve 100%:</i>\n")
            if (!a11yEnabled) {
                report.append("• Toggle ON <b>PASA Sentinel</b> in Android <i>Settings > Accessibility</i>.\n")
            }
            if (!isDozeIgnored) {
                report.append("• Set Battery to <b>Unrestricted</b> in App Info.\n")
            }
            if (!isOwner) {
                report.append("• Provision Device Owner with <code>/device_owner</code> for Knox anti-uninstall.\n")
            }
        } else {
            report.append("✨ <i>All subsystems operational. Device is fully fortified against physical theft and forensic extraction.</i>")
        }

        return CommandResult(success = true, message = report.toString())
    }
}
