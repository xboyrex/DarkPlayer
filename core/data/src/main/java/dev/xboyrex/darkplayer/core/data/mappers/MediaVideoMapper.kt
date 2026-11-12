package dev.xboyrex.darkplayer.core.data.mappers

import dev.xboyrex.darkplayer.core.common.Utils
import dev.xboyrex.darkplayer.core.database.entities.MediumStateEntity
import dev.xboyrex.darkplayer.core.media.services.MediaVideo
import dev.xboyrex.darkplayer.core.model.Video
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
