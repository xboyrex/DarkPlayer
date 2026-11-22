package com.mayank.darkplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mayank.darkplayer.core.model.ThemeConfig
import com.mayank.darkplayer.core.ui.R

@Composable
fun ThemeConfig.name(): String {
    val stringRes = when (this) {
        ThemeConfig.SYSTEM -> R.string.system_default
        ThemeConfig.OFF -> R.string.off
        ThemeConfig.ON -> R.string.on
    }

    return stringResource(id = stringRes)
}
