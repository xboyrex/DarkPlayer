package dev.xboyrex.novaplayer.core.media.network

import dev.xboyrex.novaplayer.core.media.network.clients.FtpClient
import dev.xboyrex.novaplayer.core.media.network.clients.SftpClient
import dev.xboyrex.novaplayer.core.media.network.clients.SmbClient
import dev.xboyrex.novaplayer.core.media.network.clients.WebDavClient
import dev.xboyrex.novaplayer.core.media.network.keys.SshKeyStore
import dev.xboyrex.novaplayer.core.model.NetworkConnection
import dev.xboyrex.novaplayer.core.model.NetworkProtocol
import org.koin.core.annotation.Single

@Single
class DefaultNetworkClientFactory(private val sshKeyStore: SshKeyStore) : NetworkClientFactory {
    override fun create(connection: NetworkConnection): NetworkClient = when (connection.protocol) {
        NetworkProtocol.SMB -> SmbClient(connection = connection)

        NetworkProtocol.FTP -> FtpClient(connection = connection)

        NetworkProtocol.SFTP -> SftpClient(
            connection = connection,
            sshKeyStore = sshKeyStore,
        )

        NetworkProtocol.WEBDAV -> WebDavClient(connection = connection)
    }
}
