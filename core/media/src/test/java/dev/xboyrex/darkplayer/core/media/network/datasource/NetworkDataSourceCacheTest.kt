package dev.xboyrex.darkplayer.core.media.network.datasource

import android.net.Uri
import androidx.media3.datasource.DataSpec
import dev.xboyrex.darkplayer.core.media.network.NetworkClient
import dev.xboyrex.darkplayer.core.media.network.NetworkClientFactory
import dev.xboyrex.darkplayer.core.media.network.NetworkConnectionResolver
import dev.xboyrex.darkplayer.core.model.NetworkConnection
import dev.xboyrex.darkplayer.core.model.NetworkFile
import dev.xboyrex.darkplayer.core.model.NetworkProtocol
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class NetworkDataSourceCacheTest {

    @Before
    fun setUp() {
        NetworkDataSource.clearFileSizeCache()
    }

    @Test
    fun `skips remote fileSize call when DataSpec length is known and caches unknown length across seeks`() {
        val fileSizeCalls = AtomicInteger(0)
        val payload = "0123456789abcdef".toByteArray()
        val fakeClient = object : NetworkClient {
            override val rootPath: String = "/"
            override suspend fun connect(): Result<Unit> = Result.success(Unit)
            override suspend fun disconnect() = Unit
            override fun isConnected(): Boolean = true
            override suspend fun listFiles(path: String): Result<List<NetworkFile>> =
                Result.success(emptyList())
            override suspend fun fileSize(path: String): Long {
                fileSizeCalls.incrementAndGet()
                return payload.size.toLong()
            }
            override suspend fun openStream(path: String, offset: Long): InputStream =
                ByteArrayInputStream(payload.drop(offset.toInt()).toByteArray())
        }
        val sessions = NetworkSessions(
            NetworkConnectionResolver {
                NetworkConnection(
                    id = 1L,
                    name = "Test",
                    protocol = NetworkProtocol.WEBDAV,
                    host = "127.0.0.1",
                )
            },
            NetworkClientFactory { fakeClient },
        )
        val source = NetworkDataSource(sessions)
        val uri = Uri.parse("webdav://127.0.0.1/Movies/sample.mp4?cid=1")

        // 1. Known DataSpec.length -> 0 fileSize() calls
        val knownSpec = DataSpec.Builder()
            .setUri(uri)
            .setPosition(0)
            .setLength(8)
            .build()
        assertEquals(8L, source.open(knownSpec))
        source.close()
        assertEquals(0, fileSizeCalls.get())

        // 2. Unset DataSpec.length -> 1 fileSize() call, then cached on subsequent seek open
        source.open(DataSpec.Builder().setUri(uri).setPosition(0).build())
        source.close()
        assertEquals(1, fileSizeCalls.get())

        val remaining = source.open(DataSpec.Builder().setUri(uri).setPosition(6).build())
        source.close()
        assertEquals(10L, remaining)
        assertEquals(1, fileSizeCalls.get())
    }
}
