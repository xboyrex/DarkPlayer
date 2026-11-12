package dev.xboyrex.darkplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.xboyrex.darkplayer.core.model.Resume
import dev.xboyrex.darkplayer.core.ui.R

@Composable
fun Resume.name(): String {
    val stringRes = when (this) {
        Resume.YES -> R.string.yes
        Resume.NO -> R.string.no
    }

    return stringResource(id = stringRes)
}
