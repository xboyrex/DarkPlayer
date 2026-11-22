package com.mayank.darkplayer.feature.videopicker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mayank.darkplayer.core.model.MediaLayoutMode
import com.mayank.darkplayer.core.ui.R

@Composable
fun MediaLayoutMode.name(): String = when (this) {
    MediaLayoutMode.LIST -> stringResource(id = R.string.list)
    MediaLayoutMode.GRID -> stringResource(id = R.string.grid)
}
