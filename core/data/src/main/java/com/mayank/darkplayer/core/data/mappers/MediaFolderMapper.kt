package com.mayank.darkplayer.core.data.mappers

import com.mayank.darkplayer.core.media.services.MediaFolder
import com.mayank.darkplayer.core.model.Folder

internal fun MediaFolder.toFolder() = Folder(
    name = name,
    path = path,
    dateModified = dateModified,
    totalSize = totalSize,
    totalDuration = totalDuration,
    videosCount = videosCount,
    foldersCount = foldersCount,
)
