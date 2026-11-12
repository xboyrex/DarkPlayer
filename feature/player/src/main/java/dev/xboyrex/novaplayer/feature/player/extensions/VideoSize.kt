package dev.xboyrex.novaplayer.feature.player.extensions

import androidx.media3.common.VideoSize

val VideoSize.isPortrait: Boolean
    get() = this.height > this.width
