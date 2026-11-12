package dev.xboyrex.darkplayer.feature.player.extensions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import dev.xboyrex.darkplayer.core.model.VideoContentScale
import dev.xboyrex.darkplayer.core.ui.R

/**
 * 1:1 Original Source Pixel Scale (F15 / VP-12).
 * Maps each source video pixel to 1 destination surface pixel (`ScaleFactor(1f, 1f)` when
 * `srcSize` represents unscaled source dimensions), falling back to [ContentScale.Fit] if
 * source or destination dimensions are invalid.
 */
object NativePixelContentScale : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        if (srcSize.width <= 0f || srcSize.height <= 0f || dstSize.width <= 0f || dstSize.height <= 0f) {
            return ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
        }
        return ScaleFactor(scaleX = 1f, scaleY = 1f)
    }
}

fun VideoContentScale.nameRes(): Int = when (this) {
    VideoContentScale.BEST_FIT -> R.string.best_fit
    VideoContentScale.STRETCH -> R.string.stretch
    VideoContentScale.CROP -> R.string.crop
    VideoContentScale.HUNDRED_PERCENT -> R.string.hundred_percent
}

fun VideoContentScale.drawableRes(): Int = when (this) {
    VideoContentScale.BEST_FIT -> R.drawable.ic_fit_screen
    VideoContentScale.STRETCH -> R.drawable.ic_aspect_ratio
    VideoContentScale.CROP -> R.drawable.ic_crop_landscape
    VideoContentScale.HUNDRED_PERCENT -> R.drawable.ic_width_wide
}

fun VideoContentScale.toContentScale(): ContentScale = when (this) {
    VideoContentScale.BEST_FIT -> ContentScale.Fit
    VideoContentScale.STRETCH -> ContentScale.FillBounds
    VideoContentScale.CROP -> ContentScale.Crop
    VideoContentScale.HUNDRED_PERCENT -> NativePixelContentScale
}
