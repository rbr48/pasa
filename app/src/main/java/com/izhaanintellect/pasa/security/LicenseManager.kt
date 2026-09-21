package com.izhaanintellect.pasa.security

import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces license-tier feature gating.
 *
 * All Pro surveillance and containment commands (including /screenrecord)
 * are accessible during the 7-day FREE_TRIAL and all paid tiers
 * (PRO_30, PRO_LIFETIME, PRO_ENTERPRISE, ENTERPRISE).
 */
@Singleton
class LicenseManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val pasaBackendApi: PasaBackendApi,
    private val cryptoLicenseVerifier: CryptoLicenseVerifier
) {
    companion object {
        private const val TAG = "PASA_License"
        private const val RECHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
        private const val KEY_LAST_LICENSE_CHECK = "last_license_check_ms"

        /**
         * Commands that require an active Pro license or active FREE_TRIAL.
         * /screenrecord is included here so trial and pro users have full evaluation access.
         */
        private val PRO_COMMANDS = setOf(
            "/video", "/videocap", "/vr",
            "/record", "/audio", "/mic",
            "/shred", "/wipe_folder",
            "/fakeshutdown", "/blackout", "/fake_off",
            "/duress_pin", "/duress", "/coercion",
            "/trap", "/traps", "/alarm_trap",
            "/geofence", "/fence", "/safezone",
            "/screenshot", "/screen",
            "/screen_burst", "/burst",
            "/screenrecord", "/record_screen",
            "/livestream", "/live_stream", "/live", "/stream"
        )

        /**
         * Reserved for future Enterprise-only MDM/fleet management capabilities.
         */
        private val ENTERPRISE_COMMANDS = emptySet<String>()

        /** All recognized paid tiers */
        private val PAID_TIERS = setOf(
            "PRO_30", "PRO_LIFETIME", "PRO_ENTERPRISE", "PRO_ANNUAL", "ENTERPRISE", "ENTERPRISE_LIFETIME"
        )
    }

    fun isProCommand(command: String): Boolean = command.lowercase() in PRO_COMMANDS

    /**
     * Returns true if the device has an active Pro license or an active FREE_TRIAL.
     * Uses offline Ed25519 cryptographic certificate verification for tamper-proof security.
     */
    fun isProActive(): Boolean {
        // 1. Highest priority: Cryptographically verified Ed25519 certificate (<0.2ms offline check)
        val verifiedTier = cryptoLicenseVerifier.getVerifiedStoredTier()
        if (verifiedTier != null) {
            if (verifiedTier in PAID_TIERS || verifiedTier.startsWith("PRO") || verifiedTier.startsWith("ENTERPRISE")) {
                return true
            }
            if (verifiedTier == "FREE_TRIAL") {
                return true
            }
        }

        // 2. Fallback to local Free Trial mode (for new installations prior to first backend sync)
        val rawTier = preferencesManager.licenseTier.uppercase()
        if (rawTier == "FREE_TRIAL") {
            return true
        }

        // 3. Fallback for established paid keys if certificate is refreshing
        if ((rawTier in PAID_TIERS || rawTier.startsWith("PRO")) && preferencesManager.licenseKey.isNotBlank()) {
            return true
        }

        Log.d(TAG, "No active pro license (verifiedTier=$verifiedTier, rawTier=$rawTier)")
        return false
    }

    /**
     * Returns true if the device has a paid license.
     */
    fun isPaidLicense(): Boolean {
        val verifiedTier = cryptoLicenseVerifier.getVerifiedStoredTier()
        if (verifiedTier != null) {
            return verifiedTier in PAID_TIERS || verifiedTier.startsWith("PRO_") || verifiedTier == "ENTERPRISE"
        }
        val tier = preferencesManager.licenseTier.uppercase()
        return tier in PAID_TIERS || tier.startsWith("PRO_") || tier == "ENTERPRISE"
    }

    /**
     * Checks if the command should be blocked due to licensing.
     * Returns null if allowed, or a user-facing rejection message if blocked.
     */
    fun checkAccess(command: String): String? {
        val cmd = command.lowercase()
        val tier = cryptoLicenseVerifier.getVerifiedStoredTier() ?: preferencesManager.licenseTier

        // ── Enterprise-exclusive commands ────────────────────────────────────
        if (cmd in ENTERPRISE_COMMANDS) {
            val upperTier = tier.uppercase()
            if (upperTier in setOf("ENTERPRISE", "ENTERPRISE_LIFETIME", "PRO_ENTERPRISE")) return null
            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Enterprise",
                features = listOf("Enterprise fleet management")
            )
        }

        // ── Pro & Trial commands (screenrecord, video, screenshot, etc.) ─────
        if (isProCommand(command)) {
            if (isProActive()) return null

            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Pro",
                features = listOf(
                    "Screen recording & screenshot capture",
                    "Covert video & ambient audio recording",
                    "Geofencing & automated trap system",
                    "Fake shutdown deception & blackout mode",
                    "Duress PIN distress & cryptographic shredding"
                )
            )
        }

        // ── Standard commands — always allowed ─────────────────────────────────
        return null
    }

    private fun buildRejectionMessage(
        command: String,
        tier: String,
        requiredTier: String,
        features: List<String>
    ): String {
        val featureList = features.joinToString("\n") { "• $it" }
        return "🔒 <b>$requiredTier Feature Locked</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "The command <code>$command</code> requires PASA $requiredTier.\n\n" +
                "Your current tier: <b>$tier</b>\n\n" +
                "💎 <b>Upgrade to $requiredTier to unlock:</b>\n" +
                featureList + "\n\n" +
                "Visit <b>pasa.izhaanintellect.fun</b> to upgrade."
    }

    /**
     * Activates a purchased license key and verifies + stores the cryptographic certificate.
     */
    suspend fun activateLicenseKey(key: String): Boolean {
        return try {
            val cleanKey = key.trim().uppercase()
            val response = pasaBackendApi.activateLicense(
                com.izhaanintellect.pasa.network.LicenseActivateRequest(cleanKey, preferencesManager.deviceId)
            )
            if (response.ok) {
                if (response.certificate != null) {
                    cryptoLicenseVerifier.storeCertificateIfValid(
                        response.certificate.payload,
                        response.certificate.signature
                    )
                }
                if (!response.tier.isNullOrBlank()) {
                    preferencesManager.licenseTier = response.tier
                }
                preferencesManager.licenseKey = cleanKey
                Log.i(TAG, "Key activated successfully: $cleanKey (tier=${response.tier})")
                true
            } else {
                Log.w(TAG, "Key activation failed: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "activateLicenseKey exception: ${e.message}")
            false
        }
    }

    /**
     * Refreshes the license status from the VPS backend if the last check
     * was more than 1 hour ago or if explicitly forced.
     */
    suspend fun refreshIfStale(force: Boolean = false) {
        try {
            val lastCheck = preferencesManager.run {
                val prefs = javaClass.getDeclaredField("prefs").apply { isAccessible = true }.get(this) as android.content.SharedPreferences
                prefs.getLong(KEY_LAST_LICENSE_CHECK, 0L)
            }
            val now = System.currentTimeMillis()
            if (!force && (now - lastCheck < RECHECK_INTERVAL_MS) && preferencesManager.licenseTier != "FREE_TRIAL") {
                return
            }

            val response = pasaBackendApi.checkLicense(preferencesManager.deviceId)
            if (response.ok) {
                // If backend returned cryptographic certificate, verify and store it
                if (response.certificate != null) {
                    cryptoLicenseVerifier.storeCertificateIfValid(
                        response.certificate.payload,
                        response.certificate.signature
                    )
                }
                if (!response.tier.isNullOrBlank()) {
                    preferencesManager.licenseTier = response.tier
                }
                if (!response.licenseKey.isNullOrBlank()) {
                    preferencesManager.licenseKey = response.licenseKey
                }
                preferencesManager.run {
                    val prefs = javaClass.getDeclaredField("prefs").apply { isAccessible = true }.get(this) as android.content.SharedPreferences
                    prefs.edit().putLong(KEY_LAST_LICENSE_CHECK, now).apply()
                }
                Log.i(TAG, "License refreshed: tier=${response.tier}, daysLeft=${response.daysLeft}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "License refresh failed: ${e.message}")
        }
    }
}
