package dev.xboyrex.novaplayer.feature.videopicker.screens

import dev.xboyrex.novaplayer.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
