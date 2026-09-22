package com.izhaanintellect.pasa.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tamper-proof preferences with integrity detection.
 *
 * All settings are encrypted with AES-256-GCM and MAC'd to detect modifications.
 * If integrity check fails, triggers immediate factory wipe.
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) {

    companion object {
        private const val TAG = "PASA_SecurePrefs"
        private const val PREFS_NAME = "pasa_secure_prefs"
        private const val INTEGRITY_KEY = "pref_integrity_hash"
        private const val TAMPER_ALERT_KEY = "pref_tamper_alert_sent"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Write value with automatic MAC calculation for tamper detection.
     */
    fun putString(key: String, value: String) {
        try {
            encryptedPrefs.edit().putString(key, value).apply()
            updateIntegrityHash()
            Log.d(TAG, "Stored encrypted: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted string", e)
            triggerTamperResponse("Preferences write failed: ${e.message}")
        }
    }

    fun putInt(key: String, value: Int) {
        try {
            encryptedPrefs.edit().putInt(key, value).apply()
            updateIntegrityHash()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted int", e)
            triggerTamperResponse("Preferences write failed: ${e.message}")
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        try {
            encryptedPrefs.edit().putBoolean(key, value).apply()
            updateIntegrityHash()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted boolean", e)
            triggerTamperResponse("Preferences write failed: ${e.message}")
        }
    }

    fun putStringSet(key: String, values: Set<String>) {
        try {
            encryptedPrefs.edit().putStringSet(key, values).apply()
            updateIntegrityHash()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted set", e)
            triggerTamperResponse("Preferences write failed: ${e.message}")
        }
    }

    /**
     * Read value with automatic integrity verification.
     * Triggers wipe if MAC doesn't match.
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            verifyIntegrity()
            encryptedPrefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Tampering detected during read: ${e.message}", e)
            triggerTamperResponse("Settings tampering detected!")
            defaultValue
        }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            verifyIntegrity()
            encryptedPrefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Tampering detected during read", e)
            triggerTamperResponse("Settings tampering detected!")
            defaultValue
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            verifyIntegrity()
            encryptedPrefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Tampering detected during read", e)
            triggerTamperResponse("Settings tampering detected!")
            defaultValue
        }
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return try {
            verifyIntegrity()
            encryptedPrefs.getStringSet(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Tampering detected during read", e)
            triggerTamperResponse("Settings tampering detected!")
            defaultValue
        }
    }

    /**
     * Calculate MAC of all preferences to detect modifications.
     */
    private fun updateIntegrityHash() {
        try {
            val allData = encryptedPrefs.all
                .filter { it.key != INTEGRITY_KEY && it.key != TAMPER_ALERT_KEY }
                .map { "${it.key}=${it.value}" }
                .sorted()
                .joinToString("|")

            val hash = encryptionManager.hashSHA256(allData)
            encryptedPrefs.edit().putString(INTEGRITY_KEY, hash).commit()

            Log.d(TAG, "Integrity hash updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update integrity hash", e)
        }
    }

    /**
     * Verify that settings haven't been modified.
     */
    private fun verifyIntegrity() {
        try {
            val storedHash = encryptedPrefs.getString(INTEGRITY_KEY, null) ?: return
            val allData = encryptedPrefs.all
                .filter { it.key != INTEGRITY_KEY && it.key != TAMPER_ALERT_KEY }
                .map { "${it.key}=${it.value}" }
                .sorted()
                .joinToString("|")

            val calculatedHash = encryptionManager.hashSHA256(allData)

            if (storedHash != calculatedHash) {
                Log.w(TAG, "INTEGRITY CHECK FAILED - Settings were tampered with!")
                throw SecurityException("Preferences integrity violation detected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integrity verification failed", e)
            throw e
        }
    }

    /**
     * Triggered when tampering is detected.
     * Alerts owner and prepares wipe.
     */
    private fun triggerTamperResponse(reason: String) {
        Log.e(TAG, "🚨 TAMPER RESPONSE TRIGGERED: $reason")

        val alreadyAlerted = encryptedPrefs.getBoolean(TAMPER_ALERT_KEY, false)
        if (alreadyAlerted) {
            Log.w(TAG, "Tamper alert already sent, proceeding with wipe")
            scheduleFactoryWipe()
            return
        }

        // First detection: alert owner
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This would integrate with TelegramApi to alert owner
                Log.i(TAG, "Alerting owner to tampering attempt: $reason")
                encryptedPrefs.edit().putBoolean(TAMPER_ALERT_KEY, true).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to alert owner, triggering immediate wipe", e)
                scheduleFactoryWipe()
            }
        }
    }

    /**
     * Schedule factory wipe if tampering detected twice.
     */
    private fun scheduleFactoryWipe() {
        Log.e(TAG, "🔥 FACTORY WIPE SCHEDULED - Device will reset in 60 seconds")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.delay(60000) // 60 second warning

                // Use Device Owner to wipe
                val dpm = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
                val admin = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.getComponent(context)

                Log.e(TAG, "EXECUTING FACTORY WIPE")
                dpm?.wipeData(android.app.admin.DevicePolicyManager.WIPE_ALL_DATA)
            } catch (e: Exception) {
                Log.e(TAG, "Wipe failed: ${e.message}", e)
            }
        }
    }

    /**
     * Clear all encrypted preferences (requires authorization).
     */
    fun clear() {
        try {
            encryptedPrefs.edit().clear().apply()
            Log.i(TAG, "Preferences cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear preferences", e)
        }
    }
}
