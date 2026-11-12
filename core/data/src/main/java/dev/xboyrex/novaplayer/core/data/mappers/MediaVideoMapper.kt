package dev.xboyrex.novaplayer.core.data.mappers

import dev.xboyrex.novaplayer.core.common.Utils
import dev.xboyrex.novaplayer.core.database.entities.MediumStateEntity
import dev.xboyrex.novaplayer.core.media.services.MediaVideo
import dev.xboyrex.novaplayer.core.model.Video
import java.util.Date

internal fun MediaVideo.toVideo(mediaState: MediumStateEntity? = null) = Video(
    id = id,
    uriString = uri.toString(),
    duration = duration,
    height = height,
    width = width,
    path = path,
    size = size,
    nameWithExtension = displayName,
    parentPath = parentPath,
    dateModified = dateModified,
    dateAdded = dateAdded,
    formattedDuration = Utils.formatDurationMillis(millis = duration),
    formattedFileSize = Utils.formatFileSize(size = size),
    playbackPosition = mediaState?.playbackPosition,
    lastPlayedAt = mediaState?.lastPlayedTime?.let { Date(it) },
)
