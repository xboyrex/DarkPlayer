package com.mayank.darkplayer.core.media.network

import com.mayank.darkplayer.core.media.network.clients.FtpClient
import com.mayank.darkplayer.core.media.network.clients.SftpClient
import com.mayank.darkplayer.core.media.network.clients.SmbClient
import com.mayank.darkplayer.core.media.network.clients.WebDavClient
import com.mayank.darkplayer.core.media.network.keys.SshKeyStore
import com.mayank.darkplayer.core.model.NetworkConnection
import com.mayank.darkplayer.core.model.NetworkProtocol
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
