package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.RollingText
import dev.xboyrex.darkplayer.core.ui.glass.glassOverlayFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.core.ui.glass.rememberReducedMotion
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0
    val seconds = abs(tapGestureState.seekMillis.milliseconds.inWholeSeconds).toInt()
    val backdrop = LocalGlassBackdrop.current
    val reduced = rememberReducedMotion()
    val c = MaterialTheme.colorScheme
    val accent = c.primary

    val popScale = remember { Animatable(0.86f) }
    val ringRipple = remember { Animatable(0f) }
    val arcSweep = remember { Animatable(0f) }
    val iconSpin = remember { Animatable(0f) }
    val chevronWave = remember { Animatable(0f) }

    LaunchedEffect(tapGestureState.tapCount, tapGestureState.seekMillis) {
        if (reduced) {
            popScale.snapTo(1f)
            return@LaunchedEffect
        }
        launch {
            popScale.snapTo(0.88f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.46f, stiffness = 620f),
            )
        }
        launch {
            ringRipple.snapTo(0f)
            ringRipple.animateTo(1f, tween(540, easing = Motion.Emphasized))
        }
        launch {
            arcSweep.snapTo(0f)
            arcSweep.animateTo(1f, tween(420, easing = Motion.Emphasized))
        }
        launch {
            iconSpin.snapTo(if (isForward) 48f else -48f)
            iconSpin.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 500f))
        }
        launch {
            chevronWave.snapTo(0f)
            chevronWave.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 480
                    0.5f at 160
                    1f at 480
                },
            )
        }
    }

    val entryAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(140),
        label = "skipHudAlpha",
    )

    val useLiquidGlass = backdrop != null && GlassSettings.quality != GlassQuality.Off
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = entryAlpha
                    scaleX = popScale.value
                    scaleY = popScale.value
                }
                .drawBehind {
                    val r = ringRipple.value
                    if (r < 1f) {
                        val expand1 = 14.dp.toPx() * r
                        drawRoundRect(
                            color = accent.copy(alpha = (1f - r) * 0.55f),
                            topLeft = Offset(-expand1, -expand1),
                            size = Size(size.width + expand1 * 2f, size.height + expand1 * 2f),
                            cornerRadius = CornerRadius(24.dp.toPx() + expand1),
                            style = Stroke(width = 2.2.dp.toPx() * (1f - r * 0.5f)),
                        )
                        val expand2 = 24.dp.toPx() * r
                        drawRoundRect(
                            color = Color.White.copy(alpha = (1f - r) * 0.28f),
                            topLeft = Offset(-expand2, -expand2),
                            size = Size(size.width + expand2 * 2f, size.height + expand2 * 2f),
                            cornerRadius = CornerRadius(24.dp.toPx() + expand2),
                            style = Stroke(width = 1.2.dp.toPx() * (1f - r * 0.5f)),
                        )
                    }
                }
                .clip(cardShape)
                .then(
                    if (useLiquidGlass) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = cardShape,
                                tint = glassOverlayFill(0.52f),
                                blur = 22.dp,
                                refractionHeight = 12.dp,
                                refractionAmount = 20.dp,
                            ),
                        )
                    } else {
                        Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1F2433).copy(alpha = 0.95f),
                                        Color(0xFF11141D).copy(alpha = 0.97f),
                                    ),
                                ),
                                shape = cardShape,
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.72f),
                                        Color.White.copy(alpha = 0.20f),
                                    ),
                                ),
                                shape = cardShape,
                            )
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!isForward) {
                    CascadingChevrons(isForward = false, progress = chevronWave.value, accent = accent)
                }

                // Cyber-Arc Icon Badge
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 2.4.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            style = Stroke(width = strokeW),
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = (if (isForward) 360f else -360f) * arcSweep.value,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    rotationZ = iconSpin.value
                                },
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isForward) "+" else "−",
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                        )
                        RollingText(
                            text = "${seconds}s",
                            color = Color.White,
                            up = true,
                            fontSize = 19.sp,
                        )
                    }
                    Text(
                        text = if (isForward) "FAST FORWARD" else "REWIND",
                        color = Color.White.copy(alpha = 0.68f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp,
                    )
                }

                if (isForward) {
                    CascadingChevrons(isForward = true, progress = chevronWave.value, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun CascadingChevrons(
    isForward: Boolean,
    progress: Float,
    accent: Color,
) {
    val dir = if (isForward) 1f else -1f
    val shift = (progress - 0.5f) * 6f * dir
    val alphaWave = 0.55f + 0.45f * (1f - abs(progress * 2f - 1f))
    Icon(
        imageVector = if (isForward) {
            Icons.Rounded.KeyboardDoubleArrowRight
        } else {
            Icons.Rounded.KeyboardDoubleArrowLeft
        },
        contentDescription = null,
        tint = accent.copy(alpha = alphaWave),
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                translationX = shift.dp.toPx()
            },
    )
}
