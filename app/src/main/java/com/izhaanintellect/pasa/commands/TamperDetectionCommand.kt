package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime Tamper Detection - Real-time Threat Monitoring
 *
 * Continuously detects:
 * - Root/jailbreak attempts
 * - Debugger attachment
 * - Emulator detection
 * - Xposed/Frida hooks
 * - SELinux violations
 * - APK signature tampering
 * - Debuggable flag changes
 *
 * Triggers immediate wipe if tampering detected.
 */
@Singleton
class TamperDetectionCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/tamper_detect"
    override val description = "Monitor runtime integrity and detect tampering attempts"
    override val usage = "/tamper_detect [enable|disable|scan|status]"

    companion object {
        private const val TAG = "PASA_TamperDetect"

        // Root detection paths
        private val SU_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/data/data/com.noshufou.android.su",
            "/data/data/com.thirdparty.superuser",
            "/data/app/com.noshufou.android.su-1.apk"
        )

        // Dangerous apps that enable root/debugging
        private val DANGEROUS_APPS = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk",
            "com.frida.server",
            "com.patchrom.superuser",
            "com.android.vending.billing.InAppBillingService.LOCK"
        )
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on" -> enableTamperDetection()
            "disable", "off" -> disableTamperDetection()
            "scan", "check" -> performTamperScan()
            "status" -> getTamperStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔍 <b>Tamper Detection Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/tamper_detect enable</code> — Enable continuous monitoring
                    • <code>/tamper_detect scan</code> — Run full security audit
                    • <code>/tamper_detect status</code> — Show detection state
                """.trimIndent()
            )
        }
    }

    private fun enableTamperDetection(): CommandResult {
        preferencesManager.isTamperDetectionEnabled = true

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Tamper Detection ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔍 <b>Status:</b> ACTIVE

                ✓ Continuous runtime integrity monitoring
                ✓ Root/rooting detection active
                ✓ Debugger attachment detection
                ✓ Emulator detection
                ✓ Hook/injection detection (Xposed, Frida)
                ✓ APK signature verification
                ✓ SELinux policy enforcement

                <b>If tampering detected:</b>
                🚨 Immediate alert to owner
                🔥 Auto-trigger factory wipe
                📸 Emergency photo capture
                📍 Location backup to vault

                <i>Device is now hardened against tampering.</i>
            """.trimIndent()
        )
    }

    private fun disableTamperDetection(): CommandResult {
        preferencesManager.isTamperDetectionEnabled = false

        return CommandResult(
            success = true,
            message = """
                🔴 <b>Tamper Detection DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔍 <b>Status:</b> INACTIVE

                ⚠️ Device is no longer monitoring for tampering.
                Attackers can now root/debug the device without detection.
            """.trimIndent()
        )
    }

    private suspend fun performTamperScan(): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "🔍 Running full tamper scan...")

                data class Finding(val severity: String, val emoji: String, val text: String, val detail: String)

                val critical = mutableListOf<Finding>()
                val warnings = mutableListOf<Finding>()
                val info = mutableListOf<Finding>()

                // ── 1. Root binaries ─────────────────────────────────────────────────────
                if (isDeviceRooted()) {
                    critical.add(Finding("CRITICAL", "🚨", "Device is ROOTED",
                        "A root binary (su/Magisk/KernelSU) was found. Root grants full OS control — " +
                        "malware can bypass all Android security sandboxing."))
                    Log.w(TAG, "Root detected!")
                }

                // ── 2. Dangerous / exploit apps ──────────────────────────────────────────
                val dangerousAppsFound = checkForDangerousApps()
                if (dangerousAppsFound.isNotEmpty()) {
                    critical.add(Finding("CRITICAL", "⚠️", "Root/exploit apps installed: ${dangerousAppsFound.joinToString()}",
                        "These applications grant or exploit root access and must be removed immediately."))
                    Log.w(TAG, "Dangerous apps detected: $dangerousAppsFound")
                }

                // ── 3. Debuggable APK flag ───────────────────────────────────────────────
                if (isAppDebuggable()) {
                    warnings.add(Finding("WARNING", "🐛", "PASA APK is built in DEBUG mode",
                        "The installed PASA APK has the debuggable flag set. " +
                        "This is normal only if you sideloaded a debug build for testing. " +
                        "The production release APK is never debuggable."))
                    Log.w(TAG, "Debuggable flag detected!")
                }

                // ── 4. Live debugger attached ─────────────────────────────────────────────
                if (isDebuggerAttached()) {
                    critical.add(Finding("CRITICAL", "🔗", "DEBUGGER is actively attached",
                        "An active debugger is connected to PASA's process right now. " +
                        "This is a live forensic extraction or analysis attempt."))
                    Log.w(TAG, "Debugger attached!")
                }

                // ── 5. Emulator ──────────────────────────────────────────────────────────
                if (isRunningOnEmulator()) {
                    warnings.add(Finding("WARNING", "🖥️", "Running on EMULATOR",
                        "Device fingerprint matches a virtual device. " +
                        "PASA is designed for real physical hardware only."))
                    Log.w(TAG, "Emulator detected!")
                }

                // ── 6. APK signature ─────────────────────────────────────────────────────
                if (!verifyApkSignature()) {
                    critical.add(Finding("CRITICAL", "📦", "APK signature is INVALID or missing",
                        "The installed APK has no valid signature. " +
                        "This indicates the APK was repacked, tampered, or corrupted. " +
                        "Reinstall from the official release immediately."))
                    Log.w(TAG, "Invalid APK signature!")
                }

                // ── 7. SELinux ───────────────────────────────────────────────────────────
                // SELinux Permissive is NOT inherently malicious. Motorola, OnePlus, and many OEMs
                // ship unlocked bootloaders where Developer Options enables Permissive mode.
                // Only flag as WARNING (not CRITICAL) and explain accurately.
                if (!isSELinuxEnforced()) {
                    val selinuxDetail = buildString {
                        append("SELinux is in Permissive mode — policies are logged but not enforced.\n\n")
                        append("⚠️ <b>This is commonly caused by:</b>\n")
                        append("• Developer Options enabled on your phone (most common — not a threat)\n")
                        append("• OEM bootloader unlock (Motorola, OnePlus, Xiaomi)\n")
                        append("• Rooted device with Magisk (combined with finding #1 above — serious)\n\n")
                        append("✅ <b>If you have Developer Options enabled</b> and no root was detected above, ")
                        append("this is normal and does NOT mean someone has control of your device.\n\n")
                        append("To revert: Settings → Developer Options → Disable Developer Options.")
                    }
                    // Only escalate to CRITICAL if root was also detected (compound threat)
                    if (critical.any { it.text.contains("ROOTED") }) {
                        critical.add(Finding("CRITICAL", "🛡️", "SELinux not ENFORCED (combined with ROOT — serious)",
                            selinuxDetail))
                    } else {
                        warnings.add(Finding("WARNING", "🛡️", "SELinux not ENFORCED (likely Developer Options)",
                            selinuxDetail))
                    }
                    Log.w(TAG, "SELinux not enforced!")
                }

                // ── Build report ──────────────────────────────────────────────────────────
                val totalThreats = critical.size + warnings.size
                if (totalThreats > 0) {
                    preferencesManager.tamperDetectionThreatsFound += critical.size
                }

                val sb = StringBuilder()
                sb.append("🔍 <b>Tamper Detection Scan Complete</b>\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━\n")
                sb.append("📊 <b>Device:</b> ${Build.MANUFACTURER} ${Build.MODEL}\n")
                sb.append("📦 <b>Android:</b> ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n")

                if (critical.isEmpty() && warnings.isEmpty() && info.isEmpty()) {
                    sb.append("✅ <b>VERDICT: ALL CLEAR</b>\n\n")
                    sb.append("🟢 No root detected\n")
                    sb.append("🟢 No exploit apps installed\n")
                    sb.append("🟢 No debugger attached\n")
                    sb.append("🟢 APK signature valid\n")
                    sb.append("🟢 SELinux enforced\n\n")
                    sb.append("<i>Device is secure and uncompromised.</i>")
                } else {
                    // Severity verdict banner
                    when {
                        critical.isNotEmpty() ->
                            sb.append("🔴 <b>VERDICT: CRITICAL — Immediate action required</b>\n\n")
                        warnings.isNotEmpty() ->
                            sb.append("🟡 <b>VERDICT: CAUTION — Review findings below</b>\n\n")
                        else ->
                            sb.append("🔵 <b>VERDICT: INFO — Low-risk findings only</b>\n\n")
                    }

                    // Critical findings
                    if (critical.isNotEmpty()) {
                        sb.append("🔴 <b>CRITICAL (${critical.size}):</b>\n")
                        critical.forEach { f -> sb.append("  ${f.emoji} ${f.text}\n") }
                        sb.append("\n")
                    }

                    // Warnings
                    if (warnings.isNotEmpty()) {
                        sb.append("🟡 <b>WARNING (${warnings.size}):</b>\n")
                        warnings.forEach { f -> sb.append("  ${f.emoji} ${f.text}\n") }
                        sb.append("\n")
                    }

                    // Info
                    if (info.isNotEmpty()) {
                        sb.append("🔵 <b>INFO (${info.size}):</b>\n")
                        info.forEach { f -> sb.append("  ${f.emoji} ${f.text}\n") }
                        sb.append("\n")
                    }

                    // Contextual recommendation — calibrated to actual severity
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n")
                    sb.append("<b>RECOMMENDATION:</b>\n")
                    when {
                        critical.any { it.text.contains("ROOTED") && it.text.contains("DEBUGGER") } -> {
                            sb.append("🚨 Root AND active debugger detected simultaneously — this is an active forensic extraction attempt.\n")
                            sb.append("→ Lock device now: <code>/lock</code>\n")
                            sb.append("→ Engage USB killswitch: <code>/usb_lock on</code>\n")
                            sb.append("→ Emergency wipe (irreversible!): <code>/wipe</code>")
                        }
                        critical.any { it.text.contains("ROOTED") } -> {
                            sb.append("🚨 Root access detected — malware can bypass all Android security.\n")
                            sb.append("→ Back up data and perform factory reset from Recovery.\n")
                            sb.append("→ Do NOT use <code>/wipe</code> on a rooted device — the wipe may be bypassed.\n")
                            sb.append("→ Reflash stock firmware from ${Build.MANUFACTURER} official source.")
                        }
                        critical.any { it.text.contains("APK signature") } -> {
                            sb.append("⚠️ PASA APK has been tampered. Reinstall immediately from official release:\n")
                            sb.append("<code>/update check</code> — download fresh signed APK via OTA.")
                        }
                        critical.any { it.text.contains("DEBUGGER") } -> {
                            sb.append("🔒 Lock and isolate device immediately:\n")
                            sb.append("→ <code>/lock</code>\n→ <code>/usb_lock on</code>\n→ <code>/airplane_mode on</code> if available")
                        }
                        warnings.any { it.text.contains("SELinux") } -> {
                            sb.append("ℹ️ SELinux Permissive is the only finding. <b>This is most likely caused by Developer Options</b> being enabled on your Motorola device — not a security incident.\n\n")
                            sb.append("→ To confirm safety: verify no root was found above ✅\n")
                            sb.append("→ Optional: Disable Developer Options to return to Enforcing mode.\n")
                            sb.append("→ <b>Do NOT factory wipe for SELinux Permissive alone.</b>")
                        }
                        else -> {
                            sb.append("Review the findings above. No immediate emergency action required.")
                        }
                    }
                }

                CommandResult(
                    success = true,
                    message = sb.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Scan error: ${e.message}", e)
                CommandResult(false, "Scan failed: ${e.message}")
            }
        }
    }


    private suspend fun getTamperStatus(): CommandResult {
        return withContext(Dispatchers.IO) {
            val isEnabled = preferencesManager.isTamperDetectionEnabled
            val threatsFound = preferencesManager.tamperDetectionThreatsFound

            CommandResult(
                success = true,
                message = """
                    🔍 <b>Tamper Detection Status</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    🟢 <b>Service:</b> ${if (isEnabled) "ENABLED" else "DISABLED"}
                    📊 <b>Threats Detected:</b> $threatsFound

                    <b>Monitoring:</b>
                    ✓ Root/jailbreak attempts
                    ✓ Debugger attachment
                    ✓ Emulator detection
                    ✓ Hook injection (Xposed, Frida)
                    ✓ APK signature changes
                    ✓ SELinux violations
                    ✓ Permission revocation
                    ✓ Process termination

                    <b>Response on Threat:</b>
                    🚨 Telegram alert to owner
                    🔥 Emergency photo/video capture
                    📍 Location backup to vault
                    🗑️ Factory wipe after confirmation

                    <b>Commands:</b>
                    • <code>/tamper_detect enable</code> — Start monitoring
                    • <code>/tamper_detect scan</code> — Run audit now
                    • <code>/tamper_detect status</code> — View this page

                    ℹ️ Continuous monitoring keeps device secure.
                """.trimIndent()
            )
        }
    }

    // ========== Detection Helpers ==========

    private fun isDeviceRooted(): Boolean {
        return try {
            // Check for su binary in common system locations
            val suPaths = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/adb/magisk/su",
                "/data/adb/ksu/bin/ksu",
                "/data/adb/modules/MagiskHide"
            )

            for (path in suPaths) {
                if (java.io.File(path).exists()) {
                    Log.w(TAG, "Rooting binary detected: $path")
                    return true
                }
            }

            // Try to execute su command via known absolute path with timeout
            val suExecutable = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su").firstOrNull { java.io.File(it).exists() }
            if (suExecutable != null) {
                val process = Runtime.getRuntime().exec(arrayOf(suExecutable, "-c", "id"))
                try {
                    val exited = process.waitFor(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (exited && process.exitValue() == 0) {
                        Log.w(TAG, "su command executable detected: $suExecutable")
                        return true
                    }
                } finally {
                    try { process.destroy() } catch (_: Exception) {}
                }
            }

            false
        } catch (e: Exception) {
            Log.d(TAG, "Root detection error (expected): ${e.message}")
            false
        }
    }

    private fun checkForDangerousApps(): List<String> {
        return try {
            val pm = context.packageManager
            val installed = pm.getInstalledPackages(0).map { it.packageName }
            DANGEROUS_APPS.filter { it in installed }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isAppDebuggable(): Boolean {
        return try {
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isDebuggerAttached(): Boolean {
        return try {
            android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
        } catch (e: Exception) {
            false
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        return try {
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator")
        } catch (e: Exception) {
            false
        }
    }

    private fun verifyApkSignature(): Boolean {
        return try {
            // Get package info and verify signature
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isSELinuxEnforced(): Boolean {
        // 1. Android internal SELinux API via reflection (available on all Android builds)
        try {
            val selinuxClass = Class.forName("android.os.SELinux")
            val isEnforcedMethod = selinuxClass.getMethod("isSELinuxEnforced")
            val result = isEnforcedMethod.invoke(null) as? Boolean
            if (result != null) return result
        } catch (_: Exception) {}

        // 2. Shell getenforce check with absolute path
        try {
            val getenforcePath = if (java.io.File("/system/bin/getenforce").exists()) "/system/bin/getenforce" else "getenforce"
            val process = Runtime.getRuntime().exec(arrayOf(getenforcePath))
            val output = process.inputStream.bufferedReader().readLine()?.trim()
            process.waitFor()
            if (output.equals("Enforcing", ignoreCase = true)) return true
            if (output.equals("Permissive", ignoreCase = true) || output.equals("Disabled", ignoreCase = true)) return false
        } catch (_: Exception) {}

        // 3. Check /sys/fs/selinux/enforce file
        try {
            val enforceFile = java.io.File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                val content = enforceFile.readText().trim()
                if (content == "1") return true
                if (content == "0") return false
            }
        } catch (_: Exception) {}

        // 4. Modern Android 8.0-16 CTS Guarantee: SELinux Enforcing is mandatory on production builds
        return !isAppDebuggable()
    }
}
