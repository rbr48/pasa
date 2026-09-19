package com.izhaanintellect.pasa.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles AES-256/GCM encryption and decryption using Android KeyStore.
 * Provides secure key storage, file/byte encryption, and database passphrase generation.
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_Crypto"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_ALIAS = "pasa_master_key"
        private const val DB_KEY_ALIAS = "pasa_db_key"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    /**
     * Retrieves or generates a secure, dynamic 256-bit passphrase for SQLCipher.
     * The raw passphrase is encrypted via AndroidKeyStore before being cached in preferences.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existingEncrypted = preferencesManager.dbPassphrase
        if (existingEncrypted.isNotBlank()) {
            try {
                val decoded = Base64.decode(existingEncrypted, Base64.NO_WRAP)
                return decrypt(decoded, DB_KEY_ALIAS)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt existing DB passphrase, regenerating", e)
            }
        }

        // Generate a new 32-byte (256-bit) cryptographically random passphrase
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)

        // Encrypt with KeyStore
        val encrypted = encrypt(randomBytes, DB_KEY_ALIAS)
        preferencesManager.dbPassphrase = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        Log.i(TAG, "Generated and stored new Keystore-backed database encryption key")
        return randomBytes
    }

    /**
     * Encrypts data using AES-256/GCM with a hardware-backed key from Android KeyStore.
     * Returns: IV (12 bytes) + Ciphertext.
     */
    fun encrypt(data: ByteArray, alias: String = DEFAULT_ALIAS): ByteArray {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        return result
    }

    /**
     * Decrypts data produced by [encrypt].
     * Expects the first 12 bytes to be the GCM IV.
     */
    fun decrypt(data: ByteArray, alias: String = DEFAULT_ALIAS): ByteArray {
        require(data.size > GCM_IV_LENGTH) { "Invalid encrypted data length" }
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)

        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val data = FileInputStream(inputFile).use { it.readBytes() }
        val encrypted = encrypt(data)
        FileOutputStream(outputFile).use { it.write(encrypted) }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        val data = FileInputStream(inputFile).use { it.readBytes() }
        val decrypted = decrypt(data)
        FileOutputStream(outputFile).use { it.write(decrypted) }
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        if (keyStore.containsAlias(alias)) {
            val key = keyStore.getKey(alias, null) as? SecretKey
            if (key != null) return key
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        val key = keyGenerator.generateKey()
        Log.d(TAG, "New KeyStore key generated: $alias")
        return key
    }
}
