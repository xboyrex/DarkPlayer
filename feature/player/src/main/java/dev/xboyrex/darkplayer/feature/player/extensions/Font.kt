package dev.xboyrex.darkplayer.feature.player.extensions

import android.graphics.Typeface
import dev.xboyrex.darkplayer.core.model.Font

fun Font.toTypeface(): Typeface = when (this) {
    Font.DEFAULT -> Typeface.DEFAULT
    Font.MONOSPACE -> Typeface.MONOSPACE
    Font.SANS_SERIF -> Typeface.SANS_SERIF
    Font.SERIF -> Typeface.SERIF
}
