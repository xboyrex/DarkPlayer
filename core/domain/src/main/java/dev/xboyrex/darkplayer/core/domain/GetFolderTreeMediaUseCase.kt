package dev.xboyrex.darkplayer.core.domain

import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.prettyName
import dev.xboyrex.darkplayer.core.data.repository.MediaRepository
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.Folder
import dev.xboyrex.darkplayer.core.model.Sort
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.model.isNew
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

/**
 * Produces the hierarchical (tree) view of media for a folder.
 *
 * Implements VP-18 single-pass folder tree index: each video contributes once to its parent
 * and ancestor folder accumulators in O(V * depth) rather than repeatedly filtering the full
 * descendant list for every child folder.
 */
@Factory
class GetFolderTreeMediaUseCase(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Named(DiQualifiers.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    private class FolderAccumulator(val path: String) {
        var dateModified: Long = 0L
        var totalSize: Long = 0L
        var totalDuration: Long = 0L
        var directVideosCount: Int = 0
        var newVideosCount: Int = 0
        val immediateChildren = LinkedHashSet<String>()
        val directVideos = ArrayList<Video>()
    }

    operator fun invoke(folderPath: String? = null): Flow<MediaHolder> = combine(
        mediaRepository.observeVideos(folderPath = folderPath),
        preferencesRepository.applicationPreferences,
        flow3 = newVideoClock(),
    ) { videos, preferences, nowMillis ->
        val excludedSet = preferences.excludeFolders.toHashSet()
        val included = videos.filterNot { it.parentPath in excludedSet }
        val sort = Sort(
            by = preferences.sortBy,
            order = preferences.sortOrder,
        )
        val thresholdDays = preferences.newVideoThresholdDays
        val treeIndex = buildFolderTreeIndex(
            videos = included,
            thresholdDays = thresholdDays,
            nowMillis = nowMillis,
        )

        if (folderPath != null) {
            mediaUnderIndexed(
                root = folderPath,
                treeIndex = treeIndex,
                excludedFolders = excludedSet,
                sort = sort,
            )
        } else {
            topLevelMediaIndexed(
                videos = included,
                treeIndex = treeIndex,
                excludedFolders = excludedSet,
                sort = sort,
            )
        }
    }.flowOn(context = defaultDispatcher)

    private fun buildFolderTreeIndex(
        videos: List<Video>,
        thresholdDays: Int,
        nowMillis: Long,
    ): Map<String, FolderAccumulator> {
        val index = HashMap<String, FolderAccumulator>(videos.size.coerceAtLeast(16))
        for (video in videos) {
            val isNewVideo = video.isNew(
                nowMillis = nowMillis,
                thresholdDays = thresholdDays,
            )
            val parentPath = video.parentPath
            val volumeRoot = volumeRootOf(video.path)

            var currentPath: String? = parentPath
            var childPath: String? = null
            while (!currentPath.isNullOrEmpty() && currentPath != "/") {
                val acc = index.getOrPut(currentPath) { FolderAccumulator(currentPath) }
                if (video.dateModified > acc.dateModified) {
                    acc.dateModified = video.dateModified
                }
                acc.totalSize += video.size
                acc.totalDuration += video.duration
                if (isNewVideo) {
                    acc.newVideosCount += 1
                }
                if (currentPath == parentPath) {
                    acc.directVideosCount += 1
                    acc.directVideos.add(video)
                }
                if (childPath != null) {
                    acc.immediateChildren.add(childPath)
                }
                if (currentPath == volumeRoot) break
                childPath = currentPath
                currentPath = File(currentPath).parent
            }
        }
        return index
    }

    private fun topLevelMediaIndexed(
        videos: List<Video>,
        treeIndex: Map<String, FolderAccumulator>,
        excludedFolders: Set<String>,
        sort: Sort,
    ): MediaHolder {
        val volumeRoots = videos.mapNotNull { volumeRootOf(it.path) }.distinct()
        if (volumeRoots.size <= 1) {
            val root = volumeRoots.firstOrNull()
                ?: return MediaHolder(
                    videos = emptyList(),
                    folders = emptyList(),
                )
            return mediaUnderIndexed(
                root = root,
                treeIndex = treeIndex,
                excludedFolders = excludedFolders,
                sort = sort,
            )
        }
        val folders = volumeRoots
            .filterNot { it in excludedFolders }
            .mapNotNull { volumeRoot ->
                treeIndex[volumeRoot]?.toFolder()
            }
        return MediaHolder(
            videos = emptyList(),
            folders = folders.sortedWith(sort.folderComparator()),
        )
    }

    private fun mediaUnderIndexed(
        root: String,
        treeIndex: Map<String, FolderAccumulator>,
        excludedFolders: Set<String>,
        sort: Sort,
    ): MediaHolder {
        val rootAcc = treeIndex[root]
            ?: return MediaHolder(
                videos = emptyList(),
                folders = emptyList(),
            )
        val folders = rootAcc.immediateChildren
            .filterNot { it in excludedFolders }
            .mapNotNull { childPath ->
                treeIndex[childPath]?.toFolder()
            }
        return MediaHolder(
            videos = rootAcc.directVideos.sortedWith(sort.videoComparator()),
            folders = folders.sortedWith(sort.folderComparator()),
        )
    }

    private fun FolderAccumulator.toFolder(): Folder {
        val file = File(path)
        return Folder(
            name = file.prettyName,
            path = path,
            parentPath = file.parent,
            dateModified = dateModified,
            totalSize = totalSize,
            totalDuration = totalDuration,
            videosCount = directVideosCount,
            foldersCount = immediateChildren.size,
            newVideosCount = newVideosCount,
        )
    }

    private fun volumeRootOf(path: String): String? {
        val parts = path.split('/').filter { it.isNotEmpty() }
        return when {
            parts.size >= 3 && parts[0] == "storage" && parts[1] == "emulated" ->
                "/storage/emulated/${parts[2]}"

            parts.size >= 2 && parts[0] == "storage" -> "/storage/${parts[1]}"

            else -> null
        }
    }
}
