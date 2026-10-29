package dev.xboyrex.darkplayer.core.domain

import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.prettyName
import dev.xboyrex.darkplayer.core.data.repository.MediaRepository
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.Folder
import dev.xboyrex.darkplayer.core.model.Sort
import dev.xboyrex.darkplayer.core.model.isNew
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class GetSortedFoldersUseCase(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Named(DiQualifiers.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(folderPath: String? = null): Flow<List<Folder>> = combine(
        mediaRepository.observeVideos(folderPath = folderPath),
        preferencesRepository.applicationPreferences,
        flow3 = newVideoClock(),
    ) { videos, preferences, nowMillis ->
        val folders = videos
            .groupBy { it.parentPath }
            .map { (path, folderVideos) ->
                Folder(
                    name = File(path).prettyName,
                    path = path,
                    parentPath = File(path).parent,
                    dateModified = folderVideos.maxOfOrNull { it.dateModified } ?: 0L,
                    totalSize = folderVideos.sumOf { it.size },
                    totalDuration = folderVideos.sumOf { it.duration },
                    videosCount = folderVideos.size,
                    newVideosCount = folderVideos.count {
                        it.isNew(
                            nowMillis = nowMillis,
                            thresholdDays = preferences.newVideoThresholdDays,
                        )
                    },
                )
            }
            .filterNot { it.path in preferences.excludeFolders }

        val sort = Sort(
            by = preferences.sortBy,
            order = preferences.sortOrder,
        )
        folders.sortedWith(sort.folderComparator())
    }.flowOn(context = defaultDispatcher)
}
