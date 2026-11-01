package dev.xboyrex.darkplayer.core.media.network

import android.net.Uri
import dev.xboyrex.darkplayer.core.media.network.clients.FtpClient
import dev.xboyrex.darkplayer.core.media.network.clients.SftpClient
import dev.xboyrex.darkplayer.core.media.network.clients.SmbClient
import dev.xboyrex.darkplayer.core.media.network.clients.WebDavClient
import dev.xboyrex.darkplayer.core.media.network.keys.SshKeyStore
import dev.xboyrex.darkplayer.core.media.network.keys.StagedSshKey
import dev.xboyrex.darkplayer.core.model.NetworkConnection
import dev.xboyrex.darkplayer.core.model.NetworkProtocol
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientFactoryTest {

    private val factory = DefaultNetworkClientFactory(sshKeyStore = FakeSshKeyStore())

    @Test
    fun `factory creates SFTP client`() {
        assertTrue(factory.create(connection = connection(NetworkProtocol.SFTP)) is SftpClient)
    }

    @Test
    fun `factory retains existing protocol client types`() {
        assertTrue(factory.create(connection = connection(NetworkProtocol.SMB)) is SmbClient)
        assertTrue(factory.create(connection = connection(NetworkProtocol.FTP)) is FtpClient)
        assertTrue(factory.create(connection = connection(NetworkProtocol.WEBDAV)) is WebDavClient)
    }

    private fun connection(protocol: NetworkProtocol) = NetworkConnection(
        name = protocol.name,
        protocol = protocol,
        host = "host",
    )

    private class FakeSshKeyStore : SshKeyStore {
        override suspend fun stage(uri: Uri): StagedSshKey = error(message = "Not used")
        override fun resolve(fileName: String): File = error(message = "Not used")
        override suspend fun commit(fileName: String): String = error(message = "Not used")
        override suspend fun delete(fileName: String) = Unit
    }
}
