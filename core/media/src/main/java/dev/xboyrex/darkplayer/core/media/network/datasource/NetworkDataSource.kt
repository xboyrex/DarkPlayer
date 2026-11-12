package dev.xboyrex.darkplayer.core.media.network.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import dev.xboyrex.darkplayer.core.media.network.NetworkClient
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking

/**
 * Reads a file on a saved network connection (SMB/FTP/SFTP/WebDAV) for the Media3 player.
 *
 * Implements VP-03 optimization:
 * - Skips remote [NetworkClient.fileSize] metadata round-trips when [DataSpec.length] is already supplied.
 * - Caches resolved remote file sizes per URI/path so repeated seek re-opens do not re-issue
 *   WebDAV PROPFIND/list or SMB metadata queries before opening the byte stream.
 */
@UnstableApi
class NetworkDataSource(private val sessions: NetworkSessions) : BaseDataSource(true) {

    private var uri: Uri? = null
    private var stream: InputStream? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    companion object {
        private const val MAX_CACHE_ENTRIES = 256
        private val fileSizeCache = ConcurrentHashMap<String, Long>()

        internal fun clearFileSizeCache() {
            fileSizeCache.clear()
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        try {
            runBlocking {
                val target = sessions.target(dataSpec.uri)
                val cacheKey = "${dataSpec.uri.scheme}://${dataSpec.uri.authority}/${target.filePath}"

                bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    dataSpec.length
                } else {
                    val cachedSize = fileSizeCache[cacheKey]
                    val fileSize = if (cachedSize != null && cachedSize >= 0L) {
                        cachedSize
                    } else {
                        target.client.fileSize(target.filePath).also { resolved ->
                            if (resolved >= 0L) {
                                if (fileSizeCache.size >= MAX_CACHE_ENTRIES) {
                                    fileSizeCache.clear()
                                }
                                fileSizeCache[cacheKey] = resolved
                            }
                        }
                    }
                    if (fileSize >= 0L) {
                        (fileSize - dataSpec.position).coerceAtLeast(minimumValue = 0L)
                    } else {
                        C.LENGTH_UNSET.toLong()
                    }
                }

                stream = target.client.openStream(
                    target.filePath,
                    dataSpec.position,
                )
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Failed to open ${dataSpec.uri.scheme} stream",
                e,
            )
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val readLength = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(
                length.toLong(),
                b = bytesRemaining,
            ).toInt()
        }
        val bytesRead = stream?.read(
            buffer,
            offset,
            readLength,
        ) ?: return C.RESULT_END_OF_INPUT
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            stream?.close()
        } finally {
            stream = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }
}
