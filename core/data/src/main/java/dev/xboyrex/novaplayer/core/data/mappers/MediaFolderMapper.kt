package dev.xboyrex.novaplayer.core.data.mappers

import dev.xboyrex.novaplayer.core.media.services.MediaFolder
import dev.xboyrex.novaplayer.core.model.Folder

internal fun MediaFolder.toFolder() = Folder(
    name = name,
    path = path,
    dateModified = dateModified,
    totalSize = totalSize,
    totalDuration = totalDuration,
    videosCount = videosCount,
    foldersCount = foldersCount,
)
