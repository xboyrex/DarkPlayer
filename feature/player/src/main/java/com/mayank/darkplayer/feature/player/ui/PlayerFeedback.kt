package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R as coreUiR
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.feature.player.extensions.formatted
import com.mayank.darkplayer.feature.player.state.TapGestureState
import com.mayank.darkplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean floating text overlay for Crop / Best Fit / Stretch / Zoom / Seek readouts.
 */
@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(0f, 2f),
            blurRadius = 10f,
        ),
    ),
) {
    Text(
        text = info,
        style = textStyle,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(100)),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xCC000000),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}" +
                            "${abs(offsetMs).milliseconds.inWholeSeconds}s",
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
}

/**
 * Clean transparent-background 3X Speed Playing indicator with 3 smooth single chevrons.
 */
@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 28.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(100)),
        exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.92f, animationSpec = tween(90)),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "speed_pill_pulse")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "chevron_wave",
        )
        val speedStr = if (tapGestureState.longPressSpeed % 1f == 0f) {
            "${tapGestureState.longPressSpeed.toInt()}X"
        } else {
            "${tapGestureState.longPressSpeed}X"
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                for (i in 0..2) {
                    val dist = abs(phase - i.toFloat())
                    val chevronAlpha = (1f - (dist * 0.55f)).coerceIn(0.25f, 1f)
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(chevronAlpha),
                    )
                }
            }
            Text(
                text = "$speedStr Speed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.90f),
                        offset = Offset(0f, 2f),
                        blurRadius = 10f,
                    ),
                ),
                color = Color.White,
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
    useMaterialYouControls: Boolean,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
            .padding(24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(80)),
        ) {
            VerticalProgressView(
                useMaterialYouControls = useMaterialYouControls,
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }
    }
}
