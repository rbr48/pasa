package com.izhaanintellect.pasa.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PASA configuration and state using EncryptedSharedPreferences.
 * Sensitive data (bot token, password hashes, encryption keys) is stored encrypted
 * using AES-256 via AndroidX Security.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PASA_Prefs"
        private const val PREFS_NAME = "pasa_secure_prefs"

        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_OWNER_CHAT_ID = "owner_chat_id"
        private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_BACKUP_EMAIL = "backup_email"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_TRACKING_INTERVAL = "tracking_interval"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_PENDING_OTP = "pending_otp"
        private const val KEY_OTP_TIMESTAMP = "otp_timestamp"
        private const val KEY_FAILED_UNLOCK_COUNT = "failed_unlock_count"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_UPDATE_OFFSET = "update_offset"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USE_BACKEND = "use_backend"
        const val DEFAULT_SERVER_URL = "https://izhaanintellect.fun/pasa/"
    }

    var isEncryptionAvailable = true
        private set

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            isEncryptionAvailable = false
            Log.e(TAG, "CRITICAL: Failed to create EncryptedSharedPreferences! Falling back to unencrypted SharedPreferences.", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // --- Bot Configuration ---

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value).apply()

    var ownerChatId: String
        get() = prefs.getString(KEY_OWNER_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OWNER_CHAT_ID, value).apply()

    val ownerChatIdLong: Long
        get() = ownerChatId.toLongOrNull() ?: 0L

    // --- Security & Crypto ---

    var masterPasswordHash: String
        get() = prefs.getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MASTER_PASSWORD_HASH, value).apply()

    var passwordSalt: String
        get() = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD_SALT, value).apply()

    var dbPassphrase: String
        get() = prefs.getString(KEY_DB_PASSPHRASE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DB_PASSPHRASE, value).apply()

    var backupEmail: String
        get() = prefs.getString(KEY_BACKUP_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKUP_EMAIL, value).apply()

    // --- App State ---

    var isStealthMode: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_MODE, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    // --- Tracking ---

    var isTrackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()

    var trackingIntervalMinutes: Int
        get() = prefs.getInt(KEY_TRACKING_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_TRACKING_INTERVAL, value).apply()

    var lastKnownLatitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAST_LATITUDE, 0L))
        set(value) = prefs.edit().putLong(KEY_LAST_LATITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()

    var lastKnownLongitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAST_LONGITUDE, 0L))
        set(value) = prefs.edit().putLong(KEY_LAST_LONGITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()

    // --- OTP for destructive operations ---

    var pendingOTP: String?
        get() = prefs.getString(KEY_PENDING_OTP, null)
        set(value) = prefs.edit().putString(KEY_PENDING_OTP, value).apply()

    var otpTimestamp: Long
        get() = prefs.getLong(KEY_OTP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_OTP_TIMESTAMP, value).apply()

    // --- Detection Counters ---

    var knownSimId: String?
        get() = prefs.getString("known_sim_id", null)
        set(value) = prefs.edit().putString("known_sim_id", value).apply()

    var failedUnlockCount: Int
        get() = prefs.getInt(KEY_FAILED_UNLOCK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_UNLOCK_COUNT, value).apply()

    var updateOffset: Long
        get() = prefs.getLong(KEY_UPDATE_OFFSET, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_OFFSET, value).apply()

    // --- VPS Backend Settings ---

    var serverUrl: String
        get() {
            val url = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            val normalized = if (value.trim().endsWith("/")) value.trim() else "${value.trim()}/"
            prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        }

    var deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = "pasa_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var useBackendServer: Boolean
        get() = prefs.getBoolean(KEY_USE_BACKEND, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_BACKEND, value).apply()
        
    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    // --- Cryptographic Security Settings (ASTRA Hardened Layer) ---

    var replayHighWaterMark: Long
        get() = prefs.getLong("replay_high_water_mark", 0L)
        set(value) = prefs.edit().putLong("replay_high_water_mark", value).apply()

    var replaySeenCommandIds: String
        get() = prefs.getString("replay_seen_command_ids", "") ?: ""
        set(value) = prefs.edit().putString("replay_seen_command_ids", value).apply()

    var trustedCommandKeys: Map<String, String>
        get() {
            val raw = prefs.getString("trusted_command_keys", null) ?: return emptyMap()
            val json = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return emptyMap()
            return json.keys().asSequence().associateWith { json.getString(it) }
        }
        set(value) {
            val json = org.json.JSONObject()
            value.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString("trusted_command_keys", json.toString()).apply()
        }

    fun addTrustedCommandKey(kid: String, jwk: String) {
        val current = trustedCommandKeys.toMutableMap()
        current[kid] = jwk
        trustedCommandKeys = current
    }

    var deviceKeySecurityLevel: String
        get() = prefs.getString("device_key_security_level", "UNKNOWN") ?: "UNKNOWN"
        set(value) = prefs.edit().putString("device_key_security_level", value).apply()

    /** Returns true if the minimum configuration required to run is present. */
    fun isConfigured(): Boolean {
        return botToken.isNotBlank() && ownerChatId.isNotBlank() && isSetupComplete
    }
}
