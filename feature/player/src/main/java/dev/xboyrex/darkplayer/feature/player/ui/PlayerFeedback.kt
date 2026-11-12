package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.model.VolumeBrightnessBarStyle
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import dev.xboyrex.darkplayer.feature.player.extensions.formatted
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import dev.xboyrex.darkplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
    ),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.55f),
        ) {
            Text(
                text = info,
                style = textStyle,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}

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
            color = Color.Black.copy(alpha = 0.6f),
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
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    val accent = dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor.current
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(androidx.compose.animation.core.tween(140)) + scaleIn(
            initialScale = 0.84f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
            ),
        ),
        exit = fadeOut(androidx.compose.animation.core.tween(140)) + scaleOut(
            targetScale = 0.86f,
            animationSpec = androidx.compose.animation.core.tween(140),
        ),
    ) {
        val pulseTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "speedPulse")
        val arrowShift by pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(320),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "arrowShift",
        )
        Surface(
            shape = CircleShape,
            color = Color(0xFF0A0B10).copy(alpha = 0.84f),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 7.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${tapGestureState.longPressSpeed}x",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                )
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(15.dp)
                        .padding(start = arrowShift.dp.coerceAtLeast(0.dp)),
                )
            }
        }
    }
}

/**
 * Fixes Bug #16 (`153919_Nova Player.png`):
 * - Brightness indicator is placed on the LEFT (`Alignment.CenterStart`) matching left-side swipe
 * - Volume indicator is placed on the RIGHT (`Alignment.CenterEnd`) matching right-side swipe
 * - Triggers subtle haptic ticks on volume/brightness steps
 * - Supports user-selectable `VolumeBrightnessBarStyle` (`MX_PRO_VERTICAL`, `GLASMIO_LIQUID_CAPSULE`, `NEXT_VERTICAL_PILL`)
 */
@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
    barStyle: VolumeBrightnessBarStyle = VolumeBrightnessBarStyle.MX_PRO_VERTICAL,
    swapSides: Boolean = false,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    var lastVolumeStep by remember { mutableIntStateOf(volumePercentage / 5) }
    var lastBrightnessStep by remember { mutableIntStateOf(brightnessPercentage / 5) }

    LaunchedEffect(volumePercentage, activeGesture) {
        val step = volumePercentage / 5
        if (activeGesture == VerticalGesture.VOLUME && step != lastVolumeStep) {
            lastVolumeStep = step
            view.novaHaptic(8L, 85, hapticsEnabled)
        }
    }

    LaunchedEffect(brightnessPercentage, activeGesture) {
        val step = brightnessPercentage / 5
        if (activeGesture == VerticalGesture.BRIGHTNESS && step != lastBrightnessStep) {
            lastBrightnessStep = step
            view.novaHaptic(8L, 75, hapticsEnabled)
        }
    }

    val brightnessAlignment = if (swapSides) Alignment.CenterEnd else Alignment.CenterStart
    val volumeAlignment = if (swapSides) Alignment.CenterStart else Alignment.CenterEnd

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
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(brightnessAlignment),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
        ) {
            VerticalProgressView(
                value = brightnessPercentage,
                maxValue = 100,
                style = barStyle,
                isVolume = false,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(volumeAlignment),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
        ) {
            VerticalProgressView(
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                style = barStyle,
                isVolume = true,
                icon = painterResource(
                    if (volumePercentage <= 0) {
                        coreUiR.drawable.ic_mute
                    } else {
                        coreUiR.drawable.ic_volume
                    },
                ),
            )
        }
    }
}
