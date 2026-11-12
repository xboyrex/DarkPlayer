package dev.xboyrex.darkplayer.core.domain

import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.model.Folder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

/**
 * Use case for retrieving the most popular folders based on video play history.
 *
 * Implements VP-19 optimization: precomputes per-folder play count and latest play timestamp
 * in a single O(V) pass over [videos] instead of scanning [videos] repeatedly inside the
 * O(F log F) sort comparator.
 */
@Factory
class GetPopularFoldersUseCase(
    private val getSortedFoldersUseCase: GetSortedFoldersUseCase,
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    @Named(DiQualifiers.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    private class FolderPlayStats {
        var playedCount: Int = 0
        var latestPlayedTimeMs: Long = 0L
    }

    operator fun invoke(limit: Int = 5): Flow<List<Folder>> = combine(
        flow = getSortedFoldersUseCase(),
        flow2 = getSortedVideosUseCase(),
    ) { folders, videos ->
        if (folders.isEmpty()) return@combine emptyList()
        val statsByPath = HashMap<String, FolderPlayStats>(folders.size)
        for (video in videos) {
            val playedAt = video.lastPlayedAt?.time ?: continue
            val stats = statsByPath.getOrPut(video.parentPath) { FolderPlayStats() }
            stats.playedCount += 1
            if (playedAt > stats.latestPlayedTimeMs) {
                stats.latestPlayedTimeMs = playedAt
            }
        }
        folders.sortedWith(
            compareByDescending<Folder> { folder ->
                statsByPath[folder.path]?.playedCount ?: 0
            }.thenByDescending { folder ->
                statsByPath[folder.path]?.latestPlayedTimeMs ?: 0L
            }.thenByDescending { folder ->
                folder.videosCount
            },
        ).take(n = limit)
    }.flowOn(context = defaultDispatcher)
}
