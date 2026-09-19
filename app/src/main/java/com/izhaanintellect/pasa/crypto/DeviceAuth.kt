package com.izhaanintellect.pasa.crypto

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant

/**
 * Proof-of-possession for every call the agent makes to the VPS control plane.
 * Each request carries a short-lived ES256 JWS signed by the hardware Keystore identity,
 * bound to the HTTP method and path, preventing cross-endpoint replay.
 */
object DeviceAuth {
    private const val LIFETIME_SECONDS = 60L
    private const val PROOF_HEADER_NAME = "X-Pasa-Device-Proof"
    private val random = SecureRandom()

    fun headerName(): String = PROOF_HEADER_NAME

    fun proof(deviceId: String, method: String, path: String): String {
        val now = Instant.now()
        val header = JSONObject()
            .put("alg", "ES256")
            .put("typ", "pasa-device-proof+jwt")
        val payload = JSONObject()
            .put("deviceId", deviceId)
            .put("htm", method.uppercase())
            .put("htu", path)
            .put("iat", now.epochSecond)
            .put("exp", now.plusSeconds(LIFETIME_SECONDS).epochSecond)
            .put("jti", newJti())
        return DeviceIdentity.signCompactJws(header, payload)
    }

    private fun newJti(): String {
        val bytes = ByteArray(18).also(random::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
