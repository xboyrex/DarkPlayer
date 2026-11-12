package dev.xboyrex.darkplayer.core.media.sync

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import coil3.ImageLoader
import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.getStorageVolumes
import dev.xboyrex.darkplayer.core.common.extensions.scanPaths
import dev.xboyrex.darkplayer.core.common.extensions.scanStorage
import dev.xboyrex.darkplayer.core.common.hasFullMediaAccess
import dev.xboyrex.darkplayer.core.database.converter.UriListConverter
import dev.xboyrex.darkplayer.core.database.dao.MediumStateDao
import dev.xboyrex.darkplayer.core.database.dao.PlaylistDao
import dev.xboyrex.darkplayer.core.media.services.MediaService
import dev.xboyrex.darkplayer.core.media.services.MediaVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class LocalMediaSynchronizer(
    private val mediumStateDao: MediumStateDao,
    private val playlistDao: PlaylistDao,
    private val imageLoader: ImageLoader,
    private val mediaService: MediaService,
    @Named(DiQualifiers.APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    private val context: Context,
) : MediaSynchronizer {

    private var mediaSyncingJob: Job? = null

    override suspend fun refresh(path: String?): Boolean = path?.let {
        context.scanPaths(paths = listOf(element = path))
    }
        ?: context.getStorageVolumes().all { context.scanStorage(it.path) }

    override fun startSync() {
        if (mediaSyncingJob?.isActive == true) return
        mediaSyncingJob = mediaService.observeVideos()
            .onEach { media -> updateMedia(media = media) }
            .catch {
                // A failed MediaStore query is not a complete snapshot. Leave stored state alone.
            }
            .launchIn(scope = applicationScope)
    }

    override fun stopSync() {
        mediaSyncingJob?.cancel()
    }

    private suspend fun updateMedia(media: List<MediaVideo>) = withContext(Dispatchers.Default) {
        // NP-02 Fix: Never run destructive playlist/state cleanup when the user has only granted
        // Android 14+ partial/selected-video access (READ_MEDIA_VISUAL_USER_SELECTED) or when
        // full library access is absent.
        if (!context.hasFullMediaAccess()) return@withContext
        if (media.isEmpty()) return@withContext

        val currentMediaUris = media.mapTo(destination = mutableSetOf()) { it.uri.toString() }

        playlistDao.removeMissingLocalItems(existingUris = currentMediaUris)

        val (wantedMediaStates, unwantedMediaStates) = mediumStateDao.getAll().first().partition {
            it.uriString in currentMediaUris ||
                !ContentResolver.SCHEME_CONTENT.equals(
                    it.uriString.toUri().scheme,
                    ignoreCase = true,
                )
        }

        mediumStateDao.delete(unwantedMediaStates.map { it.uriString })

        unwantedMediaStates.forEach { mediaState ->
            try {
                imageLoader.diskCache?.remove(mediaState.uriString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        launch {
            val currentMediaExternalSubs = wantedMediaStates.flatMap {
                UriListConverter.fromStringToList(it.externalSubs)
            }.toSet()

            unwantedMediaStates.onEach { mediaState ->
                for (sub in UriListConverter.fromStringToList(mediaState.externalSubs)) {
                    if (sub !in currentMediaExternalSubs) {
                        try {
                            context.contentResolver.releasePersistableUriPermission(
                                sub,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
