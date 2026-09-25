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

    /**
     * Verifies and activates an air-gapped 160-character Ed25519 SMS license payload.
     * Payload format: "<devPrefix>:<tier>:<expiresAtSeconds>|<sigBase64>"
     * Example: "pasa_6d2dc77f:PRO:1893456000|r3K9..."
     */
    fun verifyAndApplySmsLicense(smsPayload: String): Pair<Boolean, String> {
        val clean = smsPayload.trim()
        val split = if (clean.contains("|")) {
            clean.split("|", limit = 2)
        } else {
            // Fallback for colon-delimited: last segment is signature (approx 86-88 chars)
            val lastColon = clean.lastIndexOf(':')
            if (lastColon > 0) listOf(clean.substring(0, lastColon), clean.substring(lastColon + 1))
            else emptyList()
        }

        if (split.size != 2) {
            return Pair(false, "Invalid SMS license format. Expected: <deviceId>:<tier>:<expiresAt>|<signature>")
        }

        val dataStr = split[0].trim()
        val sigStr = split[1].trim()

        val dataParts = dataStr.split(":")
        if (dataParts.size < 3) {
            return Pair(false, "Malformed payload data. Expected: <deviceId>:<tier>:<expiresAt>")
        }

        val certDevPrefix = dataParts[0].trim()
        val tier = dataParts[1].trim().uppercase()
        val expiresSec = dataParts[2].trim().toLongOrNull()
            ?: return Pair(false, "Invalid expiration timestamp in payload")

        return try {
            val sigBytes = try {
                Base64.getUrlDecoder().decode(sigStr)
            } catch (_: Exception) {
                Base64.getDecoder().decode(sigStr)
            }

            if (sigBytes.size != 64) {
                return Pair(false, "Invalid Ed25519 signature size (${sigBytes.size} bytes, expected 64)")
            }

            // Cryptographically verify signature with embedded server public key
            verifier.verify(sigBytes, dataStr.toByteArray(Charsets.UTF_8))

            // Verify device target
            val myDeviceId = preferencesManager.deviceId.lowercase()
            if (certDevPrefix != "*" && !myDeviceId.startsWith(certDevPrefix.lowercase())) {
                Log.w(TAG, "SMS license target mismatch: target=$certDevPrefix, device=$myDeviceId")
                return Pair(false, "License device mismatch: Target $certDevPrefix does not match this device")
            }

            // Verify expiration
            val expiresMs = expiresSec * 1000L
            if (System.currentTimeMillis() > expiresMs) {
                return Pair(false, "License payload is expired")
            }

            // Construct and store verified certificate
            val certJson = JSONObject().apply {
                put("deviceId", preferencesManager.deviceId)
                put("tier", tier)
                put("expiresAt", expiresMs)
                put("issuedAt", System.currentTimeMillis())
                put("key", "SMS-AIRGAP-$certDevPrefix")
            }
            val payloadBase64 = Base64.getEncoder().encodeToString(certJson.toString().toByteArray(Charsets.UTF_8))
            val signatureHex = sigBytes.joinToString("") { "%02x".format(it) }

            preferencesManager.licenseCertPayload = payloadBase64
            preferencesManager.licenseCertSignature = signatureHex
            preferencesManager.licenseTier = tier
            preferencesManager.licenseKey = "SMS-AIRGAP-$certDevPrefix"

            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(expiresMs))
            Log.i(TAG, "Air-gapped SMS license applied successfully: tier=$tier, expires=$dateStr")
            Pair(true, "Offline license renewed successfully! Tier: $tier, Valid until: $dateStr")
        } catch (e: Exception) {
            Log.e(TAG, "Air-gapped SMS license verification failed: ${e.message}", e)
            Pair(false, "Cryptographic signature verification failed: ${e.message}")
        }
    }
}

