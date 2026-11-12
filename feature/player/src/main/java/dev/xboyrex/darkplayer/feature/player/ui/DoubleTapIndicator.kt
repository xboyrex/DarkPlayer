package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Minimal, ultra-clean animated +/-10s skip capsule badge ("10 sec wla too best minimal").
 */
@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    var lastNonZeroMillis by remember { mutableLongStateOf(10_000L) }
    if (tapGestureState.seekMillis != 0L) {
        lastNonZeroMillis = tapGestureState.seekMillis
    }
    val visible = tapGestureState.seekMillis != 0L
    val isForward = lastNonZeroMillis > 0L
    val seconds = abs(lastNonZeroMillis.milliseconds.inWholeSeconds)
    val accent = LocalNovaAccentColor.current

    val infiniteTransition = rememberInfiniteTransition(label = "skipSlide")
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skipChevronSlide",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(120)) + scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.86f, animationSpec = tween(150)),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0A0B10).copy(alpha = 0.80f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!isForward) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardDoubleArrowLeft,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .size(22.dp)
                                .offset(x = (-slideOffset).dp),
                        )
                    }
                    Text(
                        text = "${if (isForward) "+" else "-"}${seconds}s",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Color.White,
                    )
                    if (isForward) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .size(22.dp)
                                .offset(x = slideOffset.dp),
                        )
                    }
                }
            }
        }
    }
}
