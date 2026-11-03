package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.core.ui.audio.HardwareDolbyAndEqualizerEngine
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.feature.player.extensions.formatted
import dev.xboyrex.darkplayer.feature.player.state.BrightnessState
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import dev.xboyrex.darkplayer.feature.player.state.VerticalGesture
import dev.xboyrex.darkplayer.feature.player.state.VolumeAndBrightnessGestureState
import dev.xboyrex.darkplayer.feature.player.state.VolumeState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Normal plain text indicator for time seek (+00:10 [01:25]), crop/aspect ratio ("Best fit", "Crop", "Stretch", "100%"),
 * and pinch zoom percentage.
 */
@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.88f),
            offset = Offset(0f, 2f),
            blurRadius = 10f,
        ),
    ),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Shows the cumulative amount skipped by repeated D-pad left/right seeks while the controls are
 * hidden, along with the resulting position. Fades out shortly after the last seek.
 */
@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF212633),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}${abs(
                            n = offsetMs,
                        ).milliseconds.inWholeSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    DoubleTapIndicator(tapGestureState = tapGestureState)

    // 2x Hold Speed Solid Frosted Slate Capsule Pill
    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF222734))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = CircleShape,
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 8.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(
                        coreUiR.string.fast_playback_speed,
                        tapGestureState.longPressSpeed,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    gestureState: VolumeAndBrightnessGestureState,
    volumeState: VolumeState,
    brightnessState: BrightnessState,
) {
    val activeGesture = gestureState.activeGesture
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(
                systemBarsPadding.copy(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VerticalProgressView(
                value = volumeState.volumePercentage,
                maxValue = volumeState.maxVolumePercentage,
                isVolume = true,
                icon = painterResource(coreUiR.drawable.ic_volume),
                onValueChange = { newPct ->
                    gestureState.showGesturePillTemporarily(VerticalGesture.VOLUME)
                    volumeState.updateVolumePercentage(newPct)
                },
                onSecondaryClick = {
                    gestureState.showGesturePillTemporarily(VerticalGesture.VOLUME)
                    val nextBoost = if (volumeState.volumePercentage > 100) 100 else 200.coerceAtMost(volumeState.maxVolumePercentage)
                    volumeState.updateVolumePercentage(nextBoost)
                },
                onExpandClick = {
                    gestureState.openSoundAndVibrationPanel()
                },
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VerticalProgressView(
                value = brightnessState.brightnessPercentage,
                isVolume = false,
                icon = painterResource(coreUiR.drawable.ic_brightness),
                onValueChange = { newPct ->
                    gestureState.showGesturePillTemporarily(VerticalGesture.BRIGHTNESS)
                    brightnessState.updateBrightnessPercentage(newPct)
                },
                onSecondaryClick = {
                    gestureState.showGesturePillTemporarily(VerticalGesture.BRIGHTNESS)
                    HardwareDolbyAndEqualizerEngine.toggleNightMode()
                },
                onExpandClick = {
                    gestureState.openSoundAndVibrationPanel()
                },
            )
        }
    }
}
