package dev.xboyrex.darkplayer.core.domain

import android.content.Context
import android.net.Uri
import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.getPath
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.MediaViewMode
import dev.xboyrex.darkplayer.core.model.Video
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class GetSortedPlaylistUseCase(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val context: Context,
    @Named(DiQualifiers.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(uri: Uri): List<Video> = withContext(context = defaultDispatcher) {
        val path = context.getPath(uri = uri) ?: return@withContext emptyList()
        val parent = File(path).parent ?: return@withContext emptyList()
        val preferences = preferencesRepository.applicationPreferences.first()

        // The playlist must match the order of the list the video was launched from.
        when (preferences.mediaViewMode) {
            MediaViewMode.FOLDER_TREE -> {
                // Tree mode: the folder and its subfolders.
                getSortedVideosUseCase(parent).first()
            }

            MediaViewMode.FOLDERS -> {
                // Folders mode: only videos directly in the same folder.
                getSortedVideosUseCase(parent).first().filter { it.parentPath == parent }
            }

            MediaViewMode.VIDEOS -> {
                // Videos mode shows a single global list across all storage; play in that order.
                getSortedVideosUseCase().first()
            }
        }
    }
}
