package com.mayank.darkplayer.feature.player

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.mayank.darkplayer.feature.player.extensions.toContentScale
import com.mayank.darkplayer.feature.player.state.CuesState
import com.mayank.darkplayer.feature.player.state.PictureInPictureState
import com.mayank.darkplayer.feature.player.state.SubtitlePositionState
import com.mayank.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import com.mayank.darkplayer.feature.player.ui.ShutterView
import com.mayank.darkplayer.feature.player.ui.SubtitleConfiguration
import com.mayank.darkplayer.feature.player.ui.SubtitleView

/**
 * Fixed PlayerContentFrame:
 * - Eliminates the double-density division bug on presentationState.videoSizeDp
 *   that caused Crop/Fit aspect ratio glitches.
 * - Clips zoomed/cropped video cleanly inside viewport bounds via clipToBounds().
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    pictureInPictureState: PictureInPictureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    subtitleConfiguration: SubtitleConfiguration,
    cuesState: CuesState,
    subtitlePositionState: SubtitlePositionState,
    videoVisible: Boolean = true,
    displayAspectRatio: Float? = null,
) {
    val presentationState = rememberPresentationState(player = player)
    val resolvedSourceSizeDp: Size? = presentationState.videoSizeDp?.let { size ->
        if (size.width <= 0f || size.height <= 0f) {
            null
        } else {
            val adjustedWidth = displayAspectRatio
                ?.takeIf { it > 0f }
                ?.let { ratio -> size.height * ratio }
                ?: size.width
            Size(width = adjustedWidth, height = size.height)
        }
    }

    val videoModifier = Modifier
        .resizeWithContentScale(
            contentScale = videoZoomAndContentScaleState.videoContentScale.toContentScale(),
            sourceSizeDp = resolvedSourceSizeDp,
        )
        .graphicsLayer {
            scaleX = videoZoomAndContentScaleState.zoom
            scaleY = videoZoomAndContentScaleState.zoom
            translationX = videoZoomAndContentScaleState.offset.x
            translationY = videoZoomAndContentScaleState.offset.y
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        if (videoVisible) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = videoModifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val rect = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt(),
                    )
                    pictureInPictureState.setVideoViewRect(rect)
                },
            )
        }

        AnimatedVisibility(
            visible = subtitleConfiguration.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
            )
        }

        SubtitleView(
            bitmapModifier = videoModifier,
            cuesState = cuesState,
            positionState = subtitlePositionState,
            isInPictureInPictureMode = pictureInPictureState.isInPictureInPictureMode,
            configuration = subtitleConfiguration,
        )

        if (videoVisible && presentationState.coverSurface) {
            ShutterView()
        }
    }
}
