package dev.xboyrex.darkplayer.core.data.repository

import dev.xboyrex.darkplayer.core.data.security.CredentialCipher
import dev.xboyrex.darkplayer.core.data.security.SecureCredentialStore
import dev.xboyrex.darkplayer.core.database.dao.NetworkConnectionDao
import dev.xboyrex.darkplayer.core.database.entities.NetworkConnectionEntity
import dev.xboyrex.darkplayer.core.model.NetworkAuthentication
import dev.xboyrex.darkplayer.core.model.NetworkConnection
import dev.xboyrex.darkplayer.core.model.NetworkProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class LocalNetworkConnectionRepository(
    private val networkConnectionDao: NetworkConnectionDao,
) : NetworkConnectionRepository {

    private var credentialCipher: CredentialCipher = SecureCredentialStore

    internal constructor(
        networkConnectionDao: NetworkConnectionDao,
        credentialCipher: CredentialCipher,
    ) : this(networkConnectionDao = networkConnectionDao) {
        this.credentialCipher = credentialCipher
    }

    override fun getConnections(): Flow<List<NetworkConnection>> =
        networkConnectionDao.getAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun getConnection(id: Long): NetworkConnection? =
        networkConnectionDao.getById(id = id)?.toModel()

    override suspend fun upsert(connection: NetworkConnection): Long =
        networkConnectionDao.upsert(connection.toEntity())

    override suspend fun delete(id: Long) = networkConnectionDao.deleteById(id = id)

    private fun NetworkConnectionEntity.toModel() = NetworkConnection(
        id = id,
        name = name,
        protocol = runCatching {
            NetworkProtocol.valueOf(value = protocol)
        }.getOrDefault(NetworkProtocol.SMB),
        host = host,
        port = port,
        path = path,
        username = username,
        password = credentialCipher.decrypt(password).getOrDefault(""),
        useHttps = useHttps,
        authentication = runCatching { NetworkAuthentication.valueOf(value = authentication) }
            .getOrDefault(NetworkAuthentication.PASSWORD),
        privateKeyFileName = privateKeyFileName,
        privateKeyPassphrase = credentialCipher.decrypt(privateKeyPassphrase).getOrDefault(""),
        hostKeyFingerprint = hostKeyFingerprint,
    )

    private fun NetworkConnection.toEntity() = NetworkConnectionEntity(
        id = id,
        name = name,
        protocol = protocol.name,
        host = host,
        port = port,
        path = path,
        username = username,
        // Fail closed (F01): never persist plaintext if encryption fails
        password = credentialCipher.encrypt(password).getOrThrow(),
        useHttps = useHttps,
        authentication = authentication.name,
        privateKeyFileName = privateKeyFileName,
        privateKeyPassphrase = credentialCipher.encrypt(privateKeyPassphrase).getOrThrow(),
        hostKeyFingerprint = hostKeyFingerprint,
    )
}
