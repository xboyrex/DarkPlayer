package dev.xboyrex.darkplayer.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Abstraction for credential encryption so production enforces fail-closed Android Keystore
 * hardware-backed encryption while JVM unit tests can inject an isolated test cipher (F01).
 */
interface CredentialCipher {
    fun encrypt(plaintext: String): Result<String>
    fun decrypt(stored: String): Result<String>
}

/**
 * Production fail-closed Android Keystore AES-256-GCM cipher (F01 / NP-03 / VP-09).
 * Never falls back to plaintext or an in-memory key if Keystore or encryption fails.
 */
object SecureCredentialStore : CredentialCipher {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "nova_network_credential_key_v1"
    internal const val TRANSFORMATION = "AES/GCM/NoPadding"
    internal const val GCM_IV_LENGTH = 12
    internal const val GCM_TAG_BITS = 128
    internal const val PREFIX = "ENCv1:"

    private fun getOrCreateKeystoreSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plaintext: String): Result<String> {
        if (plaintext.isEmpty() || plaintext.startsWith(PREFIX)) {
            return Result.success(plaintext)
        }
        return runCatching {
            val secretKey = getOrCreateKeystoreSecretKey()
            encryptWithKey(plaintext, secretKey)
        }.recoverCatching { cause ->
            throw GeneralSecurityException("Credential encryption failed closed", cause)
        }
    }

    override fun decrypt(stored: String): Result<String> {
        if (stored.isEmpty()) return Result.success("")
        if (!stored.startsWith(PREFIX)) {
            // Legacy unencrypted record support during one-way migration
            return Result.success(stored)
        }
        return runCatching {
            val secretKey = getOrCreateKeystoreSecretKey()
            decryptWithKey(stored, secretKey)
        }.recoverCatching { cause ->
            throw GeneralSecurityException("Credential decryption failed", cause)
        }
    }

    internal fun encryptWithKey(plaintext: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    internal fun decryptWithKey(stored: String, secretKey: SecretKey): String {
        val raw = Base64.getDecoder().decode(stored.removePrefix(PREFIX))
        if (raw.size <= GCM_IV_LENGTH) {
            throw GeneralSecurityException("Ciphertext too short")
        }
        val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}

/**
 * Isolated AES-256-GCM cipher for JVM unit tests where AndroidKeyStore is not mounted.
 */
class JvmTestCredentialCipher : CredentialCipher {
    private val key: SecretKey = run {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        SecretKeySpec(bytes, "AES")
    }

    override fun encrypt(plaintext: String): Result<String> = runCatching {
        if (plaintext.isEmpty() || plaintext.startsWith(SecureCredentialStore.PREFIX)) {
            plaintext
        } else {
            SecureCredentialStore.encryptWithKey(plaintext, key)
        }
    }

    override fun decrypt(stored: String): Result<String> = runCatching {
        if (stored.isEmpty()) {
            ""
        } else if (!stored.startsWith(SecureCredentialStore.PREFIX)) {
            stored
        } else {
            SecureCredentialStore.decryptWithKey(stored, key)
        }
    }
}
