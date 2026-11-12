package dev.xboyrex.darkplayer.core.media.services

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.VIDEO_COLLECTION_URI
import dev.xboyrex.darkplayer.core.common.extensions.prettyName
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * [MediaService] implementation that queries the Android MediaStore for video files.
 *
 * Implements VP-01 snapshot coalescing: concurrent subscribers observing the full media library
 * within the same MediaStore change generation share a single [ContentResolver.query] result
 * instead of triggering N redundant full-library scans across Home, Recent, Popular Folders,
 * Search, and Synchronizer flows.
 */
@Single
class MediaStoreMediaService(
    private val context: Context,
    @Named(DiQualifiers.APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) : MediaService {

    companion object {
        private const val OBSERVER_DEBOUNCE_MS = 250L
        private const val SNAPSHOT_TTL_MS = 1_500L

        private val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_ADDED,
        )
    }

    private data class SnapshotEntry(
        val generation: Long,
        val timestampMs: Long,
        val videos: List<MediaVideo>,
    )

    private val changeGeneration = AtomicLong(1L)
    private val snapshotMutex = Mutex()
    @Volatile
    private var cachedRootSnapshot: SnapshotEntry? = null

    fun invalidateSnapshotCache() {
        changeGeneration.incrementAndGet()
        cachedRootSnapshot = null
    }

    @OptIn(FlowPreview::class)
    private val mediaChanges: Flow<Long> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                val gen = changeGeneration.incrementAndGet()
                cachedRootSnapshot = null
                trySend(element = gen)
            }
        }
        context.contentResolver.registerContentObserver(
            VIDEO_COLLECTION_URI,
            true,
            observer,
        )
        trySend(element = changeGeneration.incrementAndGet())
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
        .debounce(OBSERVER_DEBOUNCE_MS.milliseconds)
        .shareIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1,
        )

    override fun observeFolders(folderPath: String?): Flow<List<MediaFolder>> = observeMedia {
        fetchFoldersFromSnapshotOrQuery(folderPath = folderPath)
    }

    override fun observeVideos(folderPath: String?): Flow<List<MediaVideo>> = observeMedia {
        fetchVideosFromSnapshotOrQuery(folderPath = folderPath)
    }

    override fun observeTrashVideos(): Flow<List<MediaVideo>> = observeMedia {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) queryTrashVideos() else emptyList()
    }

    private fun <T> observeMedia(query: suspend () -> List<T>): Flow<List<T>> = mediaChanges
        .map {
            try {
                query()
            } catch (e: IllegalArgumentException) {
                if (e.message?.startsWith(prefix = "Volume ") == true &&
                    e.message?.endsWith(suffix = " not found") == true
                ) {
                    emptyList()
                } else {
                    throw e
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    private suspend fun fetchVideosFromSnapshotOrQuery(folderPath: String?): List<MediaVideo> =
        withContext(Dispatchers.IO) {
            if (folderPath != null) {
                return@withContext queryVideos(folderPath = folderPath)
            }
            val currentGen = changeGeneration.get()
            val now = SystemClock.elapsedRealtime()
            cachedRootSnapshot?.let { cached ->
                if (cached.generation == currentGen && (now - cached.timestampMs) <= SNAPSHOT_TTL_MS && cached.videos.isNotEmpty()) {
                    return@withContext cached.videos
                }
            }
            snapshotMutex.withLock {
                val recheckNow = SystemClock.elapsedRealtime()
                cachedRootSnapshot?.let { cached ->
                    if (cached.generation == changeGeneration.get() &&
                        (recheckNow - cached.timestampMs) <= SNAPSHOT_TTL_MS &&
                        cached.videos.isNotEmpty()
                    ) {
                        return@withLock cached.videos
                    }
                }
                val queried = queryVideos(folderPath = null)
                if (queried.isNotEmpty()) {
                    cachedRootSnapshot = SnapshotEntry(
                        generation = changeGeneration.get(),
                        timestampMs = SystemClock.elapsedRealtime(),
                        videos = queried,
                    )
                }
                queried
            }
        }

    private suspend fun fetchFoldersFromSnapshotOrQuery(folderPath: String?): List<MediaFolder> =
        withContext(Dispatchers.IO) {
            val videos = fetchVideosFromSnapshotOrQuery(folderPath = folderPath)
            groupVideosIntoFolders(videos)
        }

    override suspend fun fetchFolders(folderPath: String?): List<MediaFolder> =
        withContext(Dispatchers.IO) {
            val videos = fetchVideos(folderPath = folderPath)
            groupVideosIntoFolders(videos)
        }

    private fun groupVideosIntoFolders(videos: List<MediaVideo>): List<MediaFolder> {
        val videosByFolder = videos.groupBy { File(it.path).parentFile }
        return videosByFolder.mapNotNull { (folder, folderVideos) ->
            folder?.let {
                MediaFolder(
                    path = it.path,
                    name = it.prettyName,
                    dateModified = folderVideos.maxOfOrNull { video -> video.dateModified } ?: 0L,
                    totalSize = folderVideos.sumOf { video -> video.size },
                    totalDuration = folderVideos.sumOf { video -> video.duration },
                    videosCount = folderVideos.size,
                    foldersCount = 0,
                )
            }
        }
    }

    override suspend fun fetchVideos(folderPath: String?): List<MediaVideo> =
        withContext(Dispatchers.IO) { queryVideos(folderPath = folderPath) }

    override suspend fun findVideo(uri: Uri): MediaVideo? = withContext(Dispatchers.IO) {
        cachedRootSnapshot?.videos?.firstOrNull { it.uri == uri }?.let { return@withContext it }
        return@withContext try {
            context.contentResolver.query(
                uri,
                VIDEO_PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@withContext null
                cursor.toMediaVideo()
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun findFolder(path: String): MediaFolder? = withContext(Dispatchers.IO) {
        val folderFile = File(path)
        if (!folderFile.exists()) return@withContext null

        val allVideos = fetchVideos(folderPath = path)
        if (allVideos.isEmpty()) return@withContext null

        val directVideos = allVideos.filter { File(it.path).parent == path }
        val directSubfolders = allVideos
            .mapNotNull { video ->
                File(video.path).parentFile?.walkUp()?.firstOrNull { it.parent == path }
            }
            .distinctBy { it.path }

        return@withContext MediaFolder(
            path = folderFile.path,
            name = folderFile.prettyName,
            dateModified = allVideos.maxOfOrNull { it.dateModified } ?: folderFile.lastModified(),
            totalSize = allVideos.sumOf { it.size },
            totalDuration = allVideos.sumOf { it.duration },
            videosCount = directVideos.size,
            foldersCount = directSubfolders.size,
        )
    }

    private fun File.walkUp() = generateSequence(seed = parentFile) { it.parentFile }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun queryTrashVideos(): List<MediaVideo> {
        val queryArgs = Bundle().apply {
            putInt(
                MediaStore.QUERY_ARG_MATCH_TRASHED,
                MediaStore.MATCH_ONLY,
            )
            putStringArray(
                android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Video.Media.DATE_MODIFIED),
            )
            putInt(
                android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }
        return queryVideoCursor {
            context.contentResolver.query(
                VIDEO_COLLECTION_URI,
                VIDEO_PROJECTION,
                queryArgs,
                null,
            )
        }
    }

    private fun queryVideos(folderPath: String?): List<MediaVideo> {
        val selection = if (folderPath == null) {
            null
        } else {
            "${MediaStore.Video.Media.DATA} LIKE ? ESCAPE '\\'"
        }
        val selectionArgs = if (folderPath == null) {
            null
        } else {
            arrayOf("${folderPath.escapeLike()}/%")
        }
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        return queryVideoCursor {
            context.contentResolver.query(
                VIDEO_COLLECTION_URI,
                VIDEO_PROJECTION,
                selection,
                selectionArgs,
                sortOrder,
            )
        }
    }

    private fun queryVideoCursor(query: () -> Cursor?): List<MediaVideo> {
        val mediaVideos = mutableListOf<MediaVideo>()
        val cursor = checkNotNull(value = query()) {
            "MediaStore returned no cursor for the video query"
        }
        cursor.use {
            while (cursor.moveToNext()) {
                val video = cursor.toMediaVideo() ?: continue
                mediaVideos.add(element = video)
            }
        }
        return mediaVideos
    }

    private fun String.escapeLike(): String = replace(
        oldValue = "\\",
        newValue = "\\\\",
    ).replace(
        oldValue = "%",
        newValue = "\\%",
    ).replace(
        oldValue = "_",
        newValue = "\\_",
    )

    private fun Cursor.toMediaVideo(): MediaVideo? {
        val dataIndex =
            getColumnIndex(MediaStore.Video.Media.DATA).takeIf { it >= 0 } ?: return null
        val idIndex = getColumnIndex(MediaStore.Video.Media._ID).takeIf { it >= 0 } ?: return null
        val displayNameIndex =
            getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME).takeIf { it >= 0 } ?: return null
        val durationIndex =
            getColumnIndex(MediaStore.Video.Media.DURATION).takeIf { it >= 0 } ?: return null
        val widthIndex =
            getColumnIndex(MediaStore.Video.Media.WIDTH).takeIf { it >= 0 } ?: return null
        val heightIndex =
            getColumnIndex(MediaStore.Video.Media.HEIGHT).takeIf { it >= 0 } ?: return null
        val sizeIndex =
            getColumnIndex(MediaStore.Video.Media.SIZE).takeIf { it >= 0 } ?: return null
        val dateModifiedIndex =
            getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED).takeIf { it >= 0 } ?: return null
        val dateAddedIndex =
            getColumnIndex(MediaStore.Video.Media.DATE_ADDED).takeIf { it >= 0 } ?: return null

        val path = getString(dataIndex)?.takeIf { it.isNotBlank() } ?: return null
        val file = File(path)
        val size = getLong(sizeIndex)
        if (size <= 0L && !file.exists()) return null
        val id = getLong(idIndex)
        return MediaVideo(
            id = id,
            path = path,
            title = file.name,
            parentPath = file.parent ?: "/",
            uri = ContentUris.withAppendedId(
                VIDEO_COLLECTION_URI,
                id,
            ),
            displayName = getString(displayNameIndex) ?: file.name,
            duration = getLong(durationIndex),
            width = getInt(widthIndex),
            height = getInt(heightIndex),
            size = size,
            dateModified = getLong(dateModifiedIndex),
            dateAdded = getLong(dateAddedIndex),
        )
    }
}
