package dev.xboyrex.darkplayer.feature.videopicker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.xboyrex.darkplayer.core.model.MediaLayoutMode
import dev.xboyrex.darkplayer.core.ui.R

@Composable
fun MediaLayoutMode.name(): String = when (this) {
    MediaLayoutMode.LIST -> stringResource(id = R.string.list)
    MediaLayoutMode.GRID -> stringResource(id = R.string.grid)
}
