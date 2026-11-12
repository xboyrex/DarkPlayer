package dev.xboyrex.darkplayer.core.data.repository

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class LocalVaultPinRepository(private val context: Context) : VaultPinRepository {

    private val preferences by lazy {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    }

    override suspend fun hasPinSet(): Boolean = withContext(Dispatchers.IO) {
        preferences.contains(KEY_PIN_HASH)
    }

    override suspend fun setPin(pin: String) = withContext(Dispatchers.IO) {
        val salt = generateSalt()
        preferences.edit {
            putString(
                KEY_PIN_HASH,
                hashPinPbkdf2(
                    pin = pin,
                    saltHex = salt,
                ),
            )
            putString(
                KEY_SALT,
                salt,
            )
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            putBoolean(
                KEY_BIOMETRIC_ENABLED,
                false,
            )
        }
    }

    override suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lockoutUntil = preferences.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        if (lockoutUntil > now) {
            return@withContext false
        }

        val storedHash = preferences.getString(
            KEY_PIN_HASH,
            null,
        ) ?: return@withContext false
        val salt = preferences.getString(
            KEY_SALT,
            null,
        ) ?: return@withContext false

        val matched = if (storedHash.startsWith(PBKDF2_PREFIX)) {
            val candidate = hashPinPbkdf2(pin = pin, saltHex = salt)
            MessageDigest.isEqual(
                candidate.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8),
            )
        } else {
            val legacyCandidate = hashPinLegacySha256(pin = pin, salt = salt)
            val ok = MessageDigest.isEqual(
                legacyCandidate.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8),
            )
            if (ok) {
                // Transparently upgrade legacy SHA-256 verifier to PBKDF2-HMAC-SHA256
                preferences.edit {
                    putString(KEY_PIN_HASH, hashPinPbkdf2(pin = pin, saltHex = salt))
                }
            }
            ok
        }

        if (matched) {
            preferences.edit {
                putInt(KEY_FAILED_ATTEMPTS, 0)
                putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            }
            true
        } else {
            val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val cooldownMs = when {
                attempts >= 10 -> 10 * 60 * 1_000L
                attempts >= 8 -> 2 * 60 * 1_000L
                attempts >= 5 -> 30 * 1_000L
                else -> 0L
            }
            preferences.edit {
                putInt(KEY_FAILED_ATTEMPTS, attempts)
                if (cooldownMs > 0L) {
                    putLong(KEY_LOCKOUT_UNTIL_MS, now + cooldownMs)
                }
            }
            false
        }
    }

    override suspend fun hasShownHideConfirmation(): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(
            KEY_HIDE_CONFIRMATION_SHOWN,
            false,
        )
    }

    override suspend fun isBiometricEnabled(): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(
            KEY_BIOMETRIC_ENABLED,
            false,
        )
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferences.edit {
            putBoolean(
                KEY_BIOMETRIC_ENABLED,
                enabled,
            )
        }
    }

    override suspend fun setHideConfirmationShown() = withContext(Dispatchers.IO) {
        preferences.edit {
            putBoolean(
                KEY_HIDE_CONFIRMATION_SHOWN,
                true,
            )
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(size = 16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun hashPinPbkdf2(pin: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return PBKDF2_PREFIX + derived.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun hashPinLegacySha256(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "vault_security_prefs"
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_SALT = "vault_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled"
        private const val KEY_HIDE_CONFIRMATION_SHOWN = "vault_hide_confirmation_shown"
        private const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "vault_lockout_until_ms"
        private const val PBKDF2_PREFIX = "pbkdf2_v2$"
        private const val PBKDF2_ITERATIONS = 120_000
    }
}
