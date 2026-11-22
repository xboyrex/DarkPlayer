package com.mayank.darkplayer.feature.videopicker.screens

import com.mayank.darkplayer.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
