package dev.xboyrex.darkplayer.core.media.network

import dev.xboyrex.darkplayer.core.media.network.clients.FtpClient
import dev.xboyrex.darkplayer.core.media.network.clients.SftpClient
import dev.xboyrex.darkplayer.core.media.network.clients.SmbClient
import dev.xboyrex.darkplayer.core.media.network.clients.WebDavClient
import dev.xboyrex.darkplayer.core.media.network.keys.SshKeyStore
import dev.xboyrex.darkplayer.core.model.NetworkConnection
import dev.xboyrex.darkplayer.core.model.NetworkProtocol
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
