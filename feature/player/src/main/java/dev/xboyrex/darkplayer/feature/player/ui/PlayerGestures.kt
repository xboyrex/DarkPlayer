package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.xboyrex.darkplayer.feature.player.extensions.detectCustomHorizontalDragGestures
import dev.xboyrex.darkplayer.feature.player.extensions.detectCustomTransformGestures
import dev.xboyrex.darkplayer.feature.player.extensions.detectCustomVerticalDragGestures
import dev.xboyrex.darkplayer.feature.player.state.ControlsVisibilityState
import dev.xboyrex.darkplayer.feature.player.state.PictureInPictureState
import dev.xboyrex.darkplayer.feature.player.state.SeekGestureState
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import dev.xboyrex.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.xboyrex.darkplayer.feature.player.state.VolumeAndBrightnessGestureState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    pictureInPictureState: PictureInPictureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
) {
    // Keep the system's swipe-to-reveal areas reserved even while the bars are hidden.
    val gestureInsets = WindowInsets.systemGestures.union(WindowInsets.systemBarsIgnoringVisibility)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(pictureInPictureState.isInPictureInPictureMode) {
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectTapGestures(
                        onTap = {
                            if (tapGestureState.seekMillis != 0L) return@detectTapGestures
                            if (volumeAndBrightnessGestureState.isDragging ||
                                seekGestureState.isSeeking
                            ) {
                                return@detectTapGestures
                            }
                            controlsVisibilityState.toggleControlsVisibility()
                        },
                        onDoubleTap = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleDoubleTap(
                                offset = it,
                                size = size,
                            )
                        },
                        onPress = {
                            tryAwaitRelease()
                            tapGestureState.handleOnLongPressRelease()
                        },
                        onLongPress = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleLongPress(offset = it)
                        },
                    )
                }
                .pointerInput(
                    seekGestureState,
                    volumeAndBrightnessGestureState,
                    gestureInsets,
                    density,
                    layoutDirection,
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomVerticalDragGestures(
                        shouldStartGesture = {
                            !seekGestureState.isSeeking &&
                                gestureInsets.isPlayerGestureStart(
                                    it,
                                    size,
                                    density,
                                    layoutDirection,
                                )
                        },
                        onDragStart = {
                            if (!seekGestureState.isSeeking) {
                                volumeAndBrightnessGestureState.onDragStart(
                                    offset = it,
                                    size = size,
                                )
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!seekGestureState.isSeeking) {
                                volumeAndBrightnessGestureState.onDrag(change, dragAmount)
                            }
                        },
                        onDragCancel = volumeAndBrightnessGestureState::onDragEnd,
                        onDragEnd = volumeAndBrightnessGestureState::onDragEnd,
                    )
                }
                .pointerInput(
                    seekGestureState,
                    volumeAndBrightnessGestureState,
                    gestureInsets,
                    density,
                    layoutDirection,
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomHorizontalDragGestures(
                        shouldStartGesture = {
                            !volumeAndBrightnessGestureState.isDragging &&
                                volumeAndBrightnessGestureState.activeGesture == null &&
                                gestureInsets.isPlayerGestureStart(
                                    it,
                                    size,
                                    density,
                                    layoutDirection,
                                )
                        },
                        onDragStart = { offset ->
                            if (!volumeAndBrightnessGestureState.isDragging &&
                                volumeAndBrightnessGestureState.activeGesture == null
                            ) {
                                seekGestureState.onDragStart(offset)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!volumeAndBrightnessGestureState.isDragging) {
                                seekGestureState.onDrag(change, dragAmount)
                            }
                        },
                        onDragCancel = seekGestureState::onDragEnd,
                        onDragEnd = seekGestureState::onDragEnd,
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomTransformGestures(
                        onGesture = { _, panChange, zoomChange, _ ->
                            if (tapGestureState.isLongPressGestureInAction ||
                                seekGestureState.isSeeking ||
                                volumeAndBrightnessGestureState.isDragging
                            ) {
                                return@detectCustomTransformGestures
                            }
                            videoZoomAndContentScaleState.onZoomPanGesture(
                                constraints = this@BoxWithConstraints.constraints,
                                panChange = panChange,
                                zoomChange = zoomChange,
                            )
                        },
                        onGestureEnd = { videoZoomAndContentScaleState.onZoomPanGestureEnd() },
                    )
                },
        )
    }
}

internal fun WindowInsets.isPlayerGestureStart(
    position: Offset,
    size: IntSize,
    density: Density,
    layoutDirection: LayoutDirection,
): Boolean = position.x >= getLeft(
    density,
    layoutDirection,
) &&
    position.x < size.width - getRight(
        density,
        layoutDirection,
    ) &&
    position.y >= getTop(density) &&
    position.y < size.height - getBottom(density)
