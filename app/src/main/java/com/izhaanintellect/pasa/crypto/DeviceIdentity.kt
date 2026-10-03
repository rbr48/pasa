package com.izhaanintellect.pasa.crypto

import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import com.nimbusds.jose.crypto.impl.ECDSA
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * The device's non-exportable hardware-backed enrollment identity.
 * Private key generated inside Android Keystore (StrongBox when available)
 * and never leaves the secure element.
 */
object DeviceIdentity {
    private const val KEY_ALIAS = "pasa-device-signing-v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val P256_SIGNATURE_LENGTH = 64

    fun ensureKey(context: Context, attestationChallenge: ByteArray? = null): Boolean {
        val store = keystore()
        if (store.containsAlias(KEY_ALIAS)) return false

        val strongBoxAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }

        try {
            generate(strongBox = strongBoxAvailable, attestationChallenge = attestationChallenge)
        } catch (e: Exception) {
            if (strongBoxAvailable) {
                generate(strongBox = false, attestationChallenge = attestationChallenge)
            } else {
                throw e
            }
        }
        return true
    }

    private fun generate(strongBox: Boolean, attestationChallenge: ByteArray?) {
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }
        if (attestationChallenge != null) {
            builder.setAttestationChallenge(attestationChallenge)
        }

        val spec = builder.build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    fun exists(): Boolean = keystore().containsAlias(KEY_ALIAS)

    fun publicJwk(): JSONObject {
        val public = keystore().getCertificate(KEY_ALIAS)?.publicKey as? ECPublicKey
            ?: error("Device identity key is missing")
        val fieldSize = (public.params.curve.field.fieldSize + 7) / 8
        return JSONObject()
            .put("kty", "EC")
            .put("crv", "P-256")
            .put("x", public.w.affineX.toFixedWidthBase64Url(fieldSize))
            .put("y", public.w.affineY.toFixedWidthBase64Url(fieldSize))
    }

    fun attestationChain(): List<String> =
        (keystore().getCertificateChain(KEY_ALIAS) ?: emptyArray<Certificate>())
            .map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }

    fun securityLevel(): String {
        val key = keystore().getKey(KEY_ALIAS, null) as? PrivateKey ?: return "UNKNOWN"
        val info = runCatching {
            KeyFactory.getInstance(key.algorithm, KEYSTORE).getKeySpec(key, KeyInfo::class.java)
        }.getOrNull() ?: return "UNKNOWN"

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            return if (info.isInsideSecureHardware) "SECURE_HARDWARE" else "SOFTWARE"
        }
        return when (info.securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TEE"
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
            else -> "UNKNOWN"
        }
    }

    fun signCompactJws(header: JSONObject, payload: JSONObject): String {
        val key = keystore().getKey(KEY_ALIAS, null) as? PrivateKey
            ?: error("Device identity key is missing")
        val signingInput =
            "${header.toString().toBase64Url()}.${payload.toString().toBase64Url()}"
        val der = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(key)
            update(signingInput.toByteArray(Charsets.UTF_8))
            sign()
        }
        val jose = ECDSA.transcodeSignatureToConcat(der, P256_SIGNATURE_LENGTH)
        return "$signingInput.${Base64.encodeToString(jose, BASE64_URL_FLAGS)}"
    }

    private fun keystore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    private fun String.toBase64Url() =
        Base64.encodeToString(toByteArray(Charsets.UTF_8), BASE64_URL_FLAGS)

    private fun java.math.BigInteger.toFixedWidthBase64Url(width: Int): String {
        val raw = toByteArray()
        val fixed = ByteArray(width)
        when {
            raw.size == width -> raw.copyInto(fixed)
            raw.size > width -> raw.copyInto(fixed, 0, raw.size - width, raw.size)
            else -> raw.copyInto(fixed, width - raw.size)
        }
        return Base64.encodeToString(fixed, BASE64_URL_FLAGS)
    }
}
