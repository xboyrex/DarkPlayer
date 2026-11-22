package com.mayank.darkplayer.core.data.mappers

import com.mayank.darkplayer.core.common.Utils
import com.mayank.darkplayer.core.database.entities.MediumStateEntity
import com.mayank.darkplayer.core.media.services.MediaVideo
import com.mayank.darkplayer.core.model.Video
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
