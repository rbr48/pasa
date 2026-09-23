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
         * Basic loss-recovery commands (/locate, /ring, /lock, /wipe, /status, /tower, etc.) remain Lifetime Free.
         */
        private val PRO_COMMANDS = setOf(
            // ── Covert Surveillance & Captures ──
            "/snap", "/photo", "/capture",
            "/video", "/videocap", "/vr",
            "/record", "/audio", "/mic",
            "/screenshot", "/screen",
            "/screen_burst", "/burst",
            "/screenrecord", "/record_screen",
            "/livestream", "/live_stream", "/live", "/stream", "/stopstream", "/livestream_diag",

            // ── Forensics & Storage Extraction ──
            "/gallery_latest", "/getfile", "/list_files", "/clipboard",

            // ── Enterprise Hardware & Anti-Tamper Defense ──
            "/usb_lock", "/usb_data",
            "/camera_lock",
            "/bluetooth_lock", "/mic_mute",
            "/antitamper", "/tamper_detect",
            "/app_firewall",
            "/freeze", "/unfreeze", "/frozen",
            "/lock_app", "/unlock_app",
            "/biometrics",
            "/dns",
            "/notification",
            "/self_heal",
            "/security_audit",
            "/harden_boot", "/factory_reset_defense",

            // ── Autonomous Traps & Physical Deception ──
            "/fakeshutdown", "/blackout", "/fake_off",
            "/duress_pin", "/duress", "/coercion",
            "/trap", "/traps", "/alarm_trap",
            "/geofence", "/fence", "/safezone",
            "/sim_lock",
            "/deadman", "/dead_drop",
            "/thermal",
            "/pattern_guard",
            "/shred", "/wipe_folder",

            // ── Telephony & Communications ──
            "/call",
            "/contacts", "/call_log", "/sms_log", "/history",
            "/sendsms",
            "/track", "/track_stop"
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

    fun getTrialRemainingMs(): Long {
        val now = System.currentTimeMillis()
        val expiresAt = preferencesManager.trialExpiresAt
        return (expiresAt - now).coerceAtLeast(0L)
    }

    fun isTrialActive(): Boolean {
        if (isPaidLicense()) return false
        return getTrialRemainingMs() > 0L
    }

    fun isTrialExpired(): Boolean {
        if (isPaidLicense()) return false
        return getTrialRemainingMs() <= 0L
    }

    /**
     * Returns true if all features are hard locked due to expired trial without a paid license.
     */
    fun isAllFeaturesLocked(): Boolean {
        return !isPaidLicense() && isTrialExpired()
    }

    /**
     * Returns true if the device has an active Pro license or an active FREE_TRIAL.
     * Uses offline Ed25519 cryptographic certificate verification for tamper-proof security.
     */
    fun isProActive(): Boolean {
        if (isPaidLicense()) return true
        if (isTrialActive()) return true
        return false
    }

    /**
     * Returns true if the device has a paid license.
     */
    fun isPaidLicense(): Boolean {
        val verifiedTier = cryptoLicenseVerifier.getVerifiedStoredTier()
        if (verifiedTier != null) {
            return (verifiedTier in PAID_TIERS || verifiedTier.startsWith("PRO_") || verifiedTier == "ENTERPRISE")
        }
        val tier = preferencesManager.licenseTier.uppercase()
        return (tier in PAID_TIERS || tier.startsWith("PRO_") || tier == "ENTERPRISE") && preferencesManager.licenseKey.isNotBlank()
    }

    /**
     * Checks if the command should be blocked due to licensing.
     * Returns null if allowed, or a user-facing rejection message if blocked.
     *
     * HARD LOCK POLICY:
     * When trial is expired without a paid license, ALL features are locked except /license, /pro, and /info.
     */
    fun checkAccess(command: String): String? {
        val cleanCmd = command.lowercase().trim().split("\\s+".toRegex())[0]

        // ── Commands always permitted so user can inspect status & activate license ──
        if (cleanCmd in setOf("/license", "/pro", "/info")) {
            return null
        }

        // ── Hard Lockout Check: If trial has expired and no paid license, BLOCK ALL FEATURES ──
        if (isAllFeaturesLocked()) {
            return buildTrialExpiredMessage(command)
        }

        val tier = cryptoLicenseVerifier.getVerifiedStoredTier() ?: preferencesManager.licenseTier

        // ── Enterprise-exclusive commands ────────────────────────────────────
        if (cleanCmd in ENTERPRISE_COMMANDS) {
            val upperTier = tier.uppercase()
            if (upperTier in setOf("ENTERPRISE", "ENTERPRISE_LIFETIME", "PRO_ENTERPRISE")) return null
            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Enterprise",
                features = listOf("Enterprise fleet management")
            )
        }

        // ── Pro commands when not in trial and not paid ───────────────────────
        if (isProCommand(command)) {
            if (isProActive()) return null

            return buildRejectionMessage(
                command = command,
                tier = tier,
                requiredTier = "Pro",
                features = listOf(
                    "Covert photo, video & ambient audio recording (/snap, /video, /record)",
                    "Remote gallery & storage file extraction (/gallery_latest, /getfile)",
                    "Hardware USB, camera & peripheral killswitches (/usb_lock, /camera_lock)",
                    "Autonomous snatch & pocket sensor traps (/trap, /sim_lock)",
                    "Fake shutdown deception & duress decoy OS (/fakeshutdown, /duress_pin)",
                    "Remote cellular calling & direct outbound SMS (/call, /sendsms)"
                )
            )
        }

        // All features unlocked during active 7-day trial or paid license
        return null
    }

    private fun buildTrialExpiredMessage(command: String): String {
        return "🛑 <b>7-DAY TRIAL EXPIRED — ALL FEATURES LOCKED</b>\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "The command <code>$command</code> cannot be executed.\n\n" +
                "Your 7-day evaluation period has ended. All remote tracking, covert forensics, siren, and Knox defense features are locked.\n\n" +
                "💎 <b>Activate Sovereign Lifetime Shield ($25 / ৳3,000 BDT):</b>\n" +
                "• Web Checkout: <b>pasa.izhaanintellect.fun</b>\n" +
                "• Direct Key: <code>/license activate PASA-PRO-XXXX-XXXX</code>\n\n" +
                "<i>Need assistance? Contact concierge: @RBR48</i>"
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
                "Visit <b>pasa.izhaanintellect.fun</b> to claim your license."
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
