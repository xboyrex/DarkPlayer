package dev.xboyrex.novaplayer.feature.videopicker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.xboyrex.novaplayer.core.model.MediaLayoutMode
import dev.xboyrex.novaplayer.core.ui.R

@Composable
fun MediaLayoutMode.name(): String = when (this) {
    MediaLayoutMode.LIST -> stringResource(id = R.string.list)
    MediaLayoutMode.GRID -> stringResource(id = R.string.grid)
}
