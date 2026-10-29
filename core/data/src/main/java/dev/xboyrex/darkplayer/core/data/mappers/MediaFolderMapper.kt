package dev.xboyrex.darkplayer.core.data.mappers

import dev.xboyrex.darkplayer.core.media.services.MediaFolder
import dev.xboyrex.darkplayer.core.model.Folder

internal fun MediaFolder.toFolder() = Folder(
    name = name,
    path = path,
    dateModified = dateModified,
    totalSize = totalSize,
    totalDuration = totalDuration,
    videosCount = videosCount,
    foldersCount = foldersCount,
)
