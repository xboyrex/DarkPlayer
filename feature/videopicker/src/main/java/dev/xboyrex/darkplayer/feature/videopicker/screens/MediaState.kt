package dev.xboyrex.darkplayer.feature.videopicker.screens

import dev.xboyrex.darkplayer.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
