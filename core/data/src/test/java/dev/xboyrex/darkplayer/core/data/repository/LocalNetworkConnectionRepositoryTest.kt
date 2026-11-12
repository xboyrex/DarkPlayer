package dev.xboyrex.darkplayer.core.data.repository

import dev.xboyrex.darkplayer.core.data.security.CredentialCipher
import dev.xboyrex.darkplayer.core.data.security.JvmTestCredentialCipher
import dev.xboyrex.darkplayer.core.database.dao.NetworkConnectionDao
import dev.xboyrex.darkplayer.core.database.entities.NetworkConnectionEntity
import dev.xboyrex.darkplayer.core.model.NetworkAuthentication
import dev.xboyrex.darkplayer.core.model.NetworkConnection
import dev.xboyrex.darkplayer.core.model.NetworkProtocol
import java.security.GeneralSecurityException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkConnectionRepositoryTest {
    private val dao = FakeNetworkConnectionDao()
    private val cipher = JvmTestCredentialCipher()
    private val repository = LocalNetworkConnectionRepository(
        networkConnectionDao = dao,
        credentialCipher = cipher,
    )

    @Test
    fun `upsert encrypts credentials at rest and getConnection decrypts them`() = runTest {
        val connection = NetworkConnection(
            name = "SFTP",
            protocol = NetworkProtocol.SFTP,
            host = "10.0.2.2",
            username = "alice",
            password = "super-secret-password",
            authentication = NetworkAuthentication.SSH_KEY,
            privateKeyFileName = "123.key",
            privateKeyPassphrase = "passphrase",
            hostKeyFingerprint = "SHA256:abc",
        )

        assertEquals(
            1L,
            repository.upsert(connection = connection),
        )
        val rawEntity = dao.getById(1L)!!
        assertNotEquals("super-secret-password", rawEntity.password)
        assertNotEquals("passphrase", rawEntity.privateKeyPassphrase)
        assertTrue(rawEntity.password.startsWith("ENCv1:"))
        assertTrue(rawEntity.privateKeyPassphrase.startsWith("ENCv1:"))

        assertEquals(
            connection.copy(id = 1),
            repository.getConnection(id = 1),
        )
    }

    @Test
    fun `upsert fails closed and never writes plaintext when cipher fails`() = runTest {
        val failingCipher = object : CredentialCipher {
            override fun encrypt(plaintext: String): Result<String> =
                Result.failure(GeneralSecurityException("Keystore unavailable"))
            override fun decrypt(stored: String): Result<String> =
                Result.failure(GeneralSecurityException("Keystore unavailable"))
        }
        val strictRepo = LocalNetworkConnectionRepository(
            networkConnectionDao = dao,
            credentialCipher = failingCipher,
        )
        val connection = NetworkConnection(
            name = "SMB",
            protocol = NetworkProtocol.SMB,
            host = "10.0.2.2",
            password = "plaintext-must-not-be-saved",
        )

        assertThrows(GeneralSecurityException::class.java) {
            kotlinx.coroutines.runBlocking {
                strictRepo.upsert(connection)
            }
        }
        assertEquals(null, dao.getById(1L))
    }

    @Test
    fun `unknown stored authentication falls back to password`() = runTest {
        dao.seed(
            connection = NetworkConnectionEntity(
                id = 7,
                name = "Legacy",
                protocol = NetworkProtocol.SFTP.name,
                host = "10.0.2.2",
                port = 22,
                authentication = "UNKNOWN",
            ),
        )

        assertEquals(
            NetworkAuthentication.PASSWORD,
            repository.getConnection(id = 7)?.authentication,
        )
    }

    private class FakeNetworkConnectionDao : NetworkConnectionDao {
        private val connections =
            MutableStateFlow<List<NetworkConnectionEntity>>(value = emptyList())

        fun seed(connection: NetworkConnectionEntity) {
            connections.value = connections.value + connection
        }

        override suspend fun upsert(connection: NetworkConnectionEntity): Long {
            val id = connection.id.takeIf { it != 0L } ?: 1L
            val savedConnection = connection.copy(id = id)
            connections.value = connections.value.filterNot { it.id == id } + savedConnection
            return id
        }

        override fun getAll(): Flow<List<NetworkConnectionEntity>> = connections

        override suspend fun getById(id: Long): NetworkConnectionEntity? =
            connections.value.firstOrNull { it.id == id }

        override suspend fun deleteById(id: Long) {
            connections.value = connections.value.filterNot { it.id == id }
        }
    }
}
