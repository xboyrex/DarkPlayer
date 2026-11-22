package com.mayank.darkplayer.core.domain

import com.mayank.darkplayer.core.common.di.DiQualifiers
import com.mayank.darkplayer.core.data.repository.MediaRepository
import com.mayank.darkplayer.core.data.repository.PreferencesRepository
import com.mayank.darkplayer.core.model.Sort
import com.mayank.darkplayer.core.model.Video
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class GetSortedVideosUseCase(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Named(DiQualifiers.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(folderPath: String? = null): Flow<List<Video>> = combine(
        mediaRepository.observeVideos(folderPath = folderPath),
        preferencesRepository.applicationPreferences,
    ) { videoItems, preferences ->

        val nonExcludedVideos = videoItems.filterNot { it.parentPath in preferences.excludeFolders }

        val sort = Sort(
            by = preferences.sortBy,
            order = preferences.sortOrder,
        )
        nonExcludedVideos.sortedWith(sort.videoComparator())
    }.flowOn(context = defaultDispatcher)
}
