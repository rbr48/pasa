package com.izhaanintellect.pasa.security

import android.util.Log
import com.google.crypto.tink.subtle.Ed25519Verify
import com.izhaanintellect.pasa.data.PreferencesManager
import org.json.JSONObject
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class VerifiedLicenseCertificate(
    val deviceId: String,
    val tier: String,
    val expiresAt: Long,
    val issuedAt: Long,
    val key: String,
    val isExpired: Boolean
)

@Singleton
class CryptoLicenseVerifier @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_CryptoLicense"

        // Server Ed25519 Public Key (32 bytes raw)
        const val SERVER_ED25519_PUBLIC_KEY_HEX = "d5c2f7b21cd554bc67f09c26f31edc53a270abbee8ca58db3e2bc002cbb33480"

        private val serverPublicKeyBytes: ByteArray by lazy {
            hexToBytes(SERVER_ED25519_PUBLIC_KEY_HEX)
        }

        private val verifier: Ed25519Verify by lazy {
            Ed25519Verify(serverPublicKeyBytes)
        }

        fun hexToBytes(hex: String): ByteArray {
            val clean = hex.trim()
            val len = clean.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }

    /**
     * Verifies an Ed25519 signed license certificate completely offline in <0.2ms.
     * Returns null if signature is invalid, deviceId does not match, or payload is corrupt.
     */
    fun verifyCertificate(payloadBase64: String, signatureHex: String): VerifiedLicenseCertificate? {
        if (payloadBase64.isBlank() || signatureHex.isBlank()) return null
        return try {
            val signatureBytes = hexToBytes(signatureHex.trim())
            val dataBytes = payloadBase64.trim().toByteArray(Charsets.UTF_8)

            // Verify cryptographic signature with zero network
            verifier.verify(signatureBytes, dataBytes)

            // Signature verified! Now parse the cryptographically trusted payload
            val decodedJsonStr = String(Base64.getDecoder().decode(payloadBase64.trim()), Charsets.UTF_8)
            val json = JSONObject(decodedJsonStr)

            val certDeviceId = json.optString("deviceId", "")
            val tier = json.optString("tier", "FREE_TRIAL").uppercase()
            val expiresAt = json.optLong("expiresAt", 0L)
            val issuedAt = json.optLong("issuedAt", 0L)
            val key = json.optString("key", "")

            val currentDeviceId = preferencesManager.deviceId
            if (currentDeviceId.isNotBlank() && certDeviceId.isNotBlank() &&
                !currentDeviceId.equals(certDeviceId, ignoreCase = true)) {
                Log.w(TAG, "Certificate rejected: Device ID mismatch (cert=$certDeviceId, this=$currentDeviceId)")
                return null
            }

            val isExpired = expiresAt > 0 && System.currentTimeMillis() > expiresAt

            VerifiedLicenseCertificate(
                deviceId = certDeviceId,
                tier = tier,
                expiresAt = expiresAt,
                issuedAt = issuedAt,
                key = key,
                isExpired = isExpired
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cryptographic certificate verification failed: ${e.message}")
            null
        }
    }

    /**
     * Checks the locally stored certificate in EncryptedSharedPreferences.
     * Returns the verified tier if valid and not expired, or null.
     */
    fun getVerifiedStoredTier(): String? {
        val payload = preferencesManager.licenseCertPayload
        val signature = preferencesManager.licenseCertSignature
        if (payload.isBlank() || signature.isBlank()) return null

        val cert = verifyCertificate(payload, signature) ?: return null
        if (cert.isExpired) {
            Log.w(TAG, "Stored certificate is expired (expired at ${cert.expiresAt})")
            return null
        }
        return cert.tier
    }

    /**
     * Stores a new valid certificate if it passes cryptographic verification.
     */
    fun storeCertificateIfValid(payload: String?, signature: String?): Boolean {
        if (payload.isNullOrBlank() || signature.isNullOrBlank()) return false
        val cert = verifyCertificate(payload, signature)
        if (cert != null && !cert.isExpired) {
            preferencesManager.licenseCertPayload = payload
            preferencesManager.licenseCertSignature = signature
            preferencesManager.licenseTier = cert.tier
            if (cert.key.isNotBlank()) {
                preferencesManager.licenseKey = cert.key
            }
            Log.i(TAG, "Crypto certificate stored: tier=${cert.tier}, key=${cert.key}")
            return true
        }
        return false
    }
}
