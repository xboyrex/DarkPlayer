package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.glassOverlayFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.feature.player.extensions.formatted
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import dev.xboyrex.darkplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold,
    ),
) {
    val backdrop = LocalGlassBackdrop.current
    val pillShape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .then(
                    if (backdrop != null && GlassSettings.quality != GlassQuality.Off) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = pillShape,
                                tint = glassOverlayFill(0.48f),
                                blur = 18.dp,
                                refractionHeight = 10.dp,
                                refractionAmount = 16.dp,
                            ),
                        )
                    } else {
                        Modifier
                            .background(Color(0xFF080C14).copy(alpha = 0.62f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.26f),
                                        Color.White.copy(alpha = 0.08f),
                                    ),
                                ),
                                shape = pillShape,
                            )
                    },
                )
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = info,
                style = textStyle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Shows the cumulative amount skipped by repeated D-pad left/right seeks while the controls are
 * hidden, along with the resulting position. Fades out shortly after the last seek.
 */
@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    val backdrop = LocalGlassBackdrop.current
    val pillShape = RoundedCornerShape(20.dp)
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(tween(150, easing = Motion.Emphasized)) +
            scaleIn(spring(dampingRatio = 0.65f, stiffness = 500f), initialScale = 0.85f),
        exit = fadeOut(tween(180, easing = Motion.Emphasized)) +
            scaleOut(tween(180, easing = Motion.Emphasized), targetScale = 0.85f),
    ) {
        Row(
            modifier = Modifier
                .clip(pillShape)
                .then(
                    if (backdrop != null && GlassSettings.quality != GlassQuality.Off) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = pillShape,
                                tint = glassOverlayFill(0.48f),
                                blur = 18.dp,
                                refractionHeight = 10.dp,
                                refractionAmount = 16.dp,
                            ),
                        )
                    } else {
                        Modifier.background(Color.Black.copy(alpha = 0.62f))
                    },
                )
                .padding(
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
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    val backdrop = LocalGlassBackdrop.current
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(tween(160, easing = Motion.Emphasized)) +
            scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.8f),
        exit = fadeOut(tween(160, easing = Motion.Emphasized)) +
            scaleOut(tween(160, easing = Motion.Emphasized), targetScale = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .then(
                    if (backdrop != null && GlassSettings.quality != GlassQuality.Off) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = CircleShape,
                                tint = glassOverlayFill(0.48f),
                                blur = 18.dp,
                                refractionHeight = 10.dp,
                                refractionAmount = 16.dp,
                            ),
                        )
                    } else {
                        Modifier
                            .background(Color.Black.copy(alpha = 0.62f))
                            .border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = 0.24f),
                                shape = CircleShape,
                            )
                    },
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(
                    coreUiR.string.fast_playback_speed,
                    tapGestureState.longPressSpeed,
                ),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                ),
                color = Color.White,
            )
            Icon(
                imageVector = Icons.Rounded.FastForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
) {
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
            .padding(24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(tween(150, easing = Motion.Emphasized)) +
                scaleIn(spring(dampingRatio = 0.7f, stiffness = 500f), initialScale = 0.9f),
            exit = fadeOut(tween(180, easing = Motion.Emphasized)) +
                scaleOut(tween(180, easing = Motion.Emphasized), targetScale = 0.9f),
        ) {
            VerticalProgressView(
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(tween(150, easing = Motion.Emphasized)) +
                scaleIn(spring(dampingRatio = 0.7f, stiffness = 500f), initialScale = 0.9f),
            exit = fadeOut(tween(180, easing = Motion.Emphasized)) +
                scaleOut(tween(180, easing = Motion.Emphasized), targetScale = 0.9f),
        ) {
            VerticalProgressView(
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }
    }
}
