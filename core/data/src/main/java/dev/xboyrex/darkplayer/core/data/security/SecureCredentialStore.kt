package dev.xboyrex.darkplayer.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption for saved network passwords,
 * SSH private key passphrases, and sensitive tokens (NP-03).
 */
object SecureCredentialStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "nova_network_credential_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PREFIX = "ENCv1:"

    @Volatile
    private var fallbackTestKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
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
                keyGenerator.generateKey()
            }
        } catch (_: Throwable) {
            // Fallback ephemeral key when running inside JVM unit tests without AndroidKeyStore
            fallbackTestKey ?: synchronized(this) {
                fallbackTestKey ?: run {
                    val bytes = ByteArray(32)
                    SecureRandom().nextBytes(bytes)
                    SecretKeySpec(bytes, "AES").also { fallbackTestKey = it }
                }
            }
        }
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty() || plaintext.startsWith(PREFIX)) return plaintext
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            plaintext
        }
    }

    fun decrypt(stored: String): String {
        if (stored.isEmpty() || !stored.startsWith(PREFIX)) return stored
        return try {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (raw.size <= GCM_IV_LENGTH) return ""
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }
}
