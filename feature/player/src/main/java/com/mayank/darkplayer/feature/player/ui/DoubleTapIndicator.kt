package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean, transparent-background 10s skip indicator with zero oval clipping.
 */
@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0L
    val pulseScale = remember { Animatable(0.88f) }

    LaunchedEffect(tapGestureState.seekMillis) {
        pulseScale.snapTo(0.88f)
        pulseScale.animateTo(1.08f, animationSpec = tween(110))
        pulseScale.animateTo(1.0f, animationSpec = tween(110))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = 0.40f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DoubleTapSeekTriangles(isForward = isForward)
                Spacer(Modifier.height(4.dp))
                val secs = abs(tapGestureState.seekMillis.milliseconds.inWholeSeconds)
                Text(
                    text = "${if (isForward) "+" else "-"}${secs}s",
                    style = MaterialTheme.typography.titleLarge.copy(
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
}

@Composable
private fun DoubleTapSeekTriangles(isForward: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "skip_chevrons")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skip_wave",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0..2) {
            val idx = if (isForward) i else (2 - i)
            val dist = abs(phase - idx.toFloat())
            val alphaVal = (1f - dist * 0.55f).coerceIn(0.25f, 1f)
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(if (isForward) 0f else 180f)
                    .alpha(alphaVal),
            )
        }
    }
}
