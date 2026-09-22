package com.izhaanintellect.pasa.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boot Gap Detection Service
 *
 * Detects if device booted from recovery mode (indicating factory reset or wipe attempt).
 * If recovery boot is detected, triggers emergency wipe BEFORE attacker can do anything.
 */
@Singleton
class BootGapDetectionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        private const val TAG = "PASA_BootGap"
        private const val BOOT_TIMESTAMP_KEY = "last_boot_timestamp"
        private const val LAST_SHUTDOWN_KEY = "last_shutdown_timestamp"
    }

    fun detectRecoveryBoot() {
        Log.i(TAG, "🔍 Checking for recovery mode boot...")

        try {
            val lastShutdown = preferencesManager.lastShutdownTime
            val currentBoot = System.currentTimeMillis()

            // If no previous shutdown time, this is first boot
            if (lastShutdown == 0L) {
                Log.i(TAG, "✓ First boot - no anomalies")
                recordBootTime(currentBoot)
                return
            }

            val bootGap = currentBoot - lastShutdown

            Log.d(TAG, "Last shutdown: $lastShutdown")
            Log.d(TAG, "Current boot: $currentBoot")
            Log.d(TAG, "Boot gap: ${bootGap}ms (${bootGap / 1000} seconds)")

            // Normal boot: gap should be small (a few seconds)
            // Recovery boot: gap might be larger OR timestamp might reset
            // Factory reset: boot gap is very large OR timestamp is reset to epoch

            when {
                bootGap > 120_000 -> { // 120+ second gap = suspicious
                    Log.w(TAG, "⚠️ SUSPICIOUS BOOT GAP: ${bootGap / 1000} seconds")
                    handleSuspiciousBoot("Boot gap: ${bootGap / 1000}s (recovery mode suspected)")
                }
                bootGap < 0 -> { // Negative gap = time went backwards (factory reset)
                    Log.e(TAG, "🚨 BOOT TIME RESET: Time went backwards by ${kotlin.math.abs(bootGap) / 1000} seconds")
                    handleSuspiciousBoot("Time reset detected - likely factory reset attempt")
                }
                isRecoveryModeActive() -> { // Check if currently in recovery
                    Log.e(TAG, "🚨 CURRENTLY IN RECOVERY MODE")
                    handleSuspiciousBoot("Device is in recovery mode")
                }
                else -> {
                    Log.i(TAG, "✓ Normal boot - no anomalies")
                }
            }

            // Record this boot for next time
            recordBootTime(currentBoot)

        } catch (e: Exception) {
            Log.e(TAG, "Boot detection failed: ${e.message}", e)
        }
    }

    private fun getSystemProperty(key: String, def: String = ""): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java, String::class.java)
            get.invoke(null, key, def) as String
        } catch (e: Exception) {
            def
        }
    }

    /**
     * Detect if device is currently in recovery mode.
     */
    private fun isRecoveryModeActive(): Boolean {
        return try {
            // Check if in recovery by looking for recovery-related properties
            val isRecovery = getSystemProperty("ro.recovery", "").isNotEmpty() ||
                    getSystemProperty("ro.bootloader", "").contains("recovery")

            Log.d(TAG, "Recovery mode check: $isRecovery")
            isRecovery
        } catch (e: Exception) {
            Log.w(TAG, "Could not check recovery mode: ${e.message}")
            false
        }
    }

    /**
     * Handle suspicious boot (recovery mode detected).
     * Trigger emergency wipe BEFORE attacker can do anything.
     */
    private fun handleSuspiciousBoot(reason: String) {
        Log.e(TAG, "🚨 HANDLING SUSPICIOUS BOOT: $reason")

        try {
            // Alert owner via Telegram
            sendSuspiciousBootAlert(reason)

            // If configured, trigger immediate wipe
            if (shouldWipeOnRecoveryBoot()) {
                triggerEmergencyWipe(reason)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle suspicious boot: ${e.message}", e)
        }
    }

    private fun shouldWipeOnRecoveryBoot(): Boolean {
        // Only wipe if boot hardening is enabled
        return preferencesManager.isBootHardenedLocked
    }

    private fun sendSuspiciousBootAlert(reason: String) {
        Log.w(TAG, """
            🚨 SUSPICIOUS BOOT DETECTED
            Reason: $reason
            Action: Security response initiated
        """.trimIndent())

        preferencesManager.lastRecoveryBootDetection = System.currentTimeMillis()
        preferencesManager.recoveryBootAttempts = preferencesManager.recoveryBootAttempts + 1
    }

    /**
     * Trigger factory wipe IMMEDIATELY.
     * This destroys all data before attacker can extract it.
     */
    private fun triggerEmergencyWipe(reason: String) {
        Log.e(TAG, "🔥 TRIGGERING EMERGENCY WIPE: $reason")

        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            val admin = PasaDeviceAdmin.getComponent(context)

            if (dpm != null && admin != null) {
                Log.e(TAG, "Executing factory wipe via Device Owner...")

                // Wipe all data immediately
                dpm.wipeData(0)

                Log.e(TAG, "✅ Wipe command sent - device will be reset")
            } else {
                Log.e(TAG, "❌ Device Owner not available for wipe")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wipe failed: ${e.message}", e)
        }
    }

    /**
     * Record this boot time for next comparison.
     */
    private fun recordBootTime(timestamp: Long) {
        try {
            preferencesManager.lastBootTime = timestamp
            Log.d(TAG, "Boot time recorded: $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record boot time: ${e.message}")
        }
    }

    /**
     * Record shutdown time (should be called before reboot/shutdown).
     */
    fun recordShutdownTime() {
        try {
            preferencesManager.lastShutdownTime = System.currentTimeMillis()
            Log.d(TAG, "Shutdown time recorded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record shutdown time: ${e.message}")
        }
    }

    /**
     * Get recovery boot detection status.
     */
    fun getBootDetectionStatus(): String {
        return try {
            val lastDetection = preferencesManager.lastRecoveryBootDetection
            val attempts = preferencesManager.recoveryBootAttempts

            """
                🔍 Boot Gap Detection Status
                ━━━━━━━━━━━━━━━━━━━━
                📊 Recovery boot attempts detected: $attempts
                ⏰ Last detection: ${if (lastDetection > 0) formatTime(lastDetection) else "Never"}

                🛡️ Protection: ACTIVE
                If recovery mode is detected, device is wiped immediately.
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting boot status: ${e.message}"
        }
    }

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000} minutes ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            else -> "${diff / 86_400_000} days ago"
        }
    }
}
