package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.model.DoubleTapGesture
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.RollingText
import dev.xboyrex.darkplayer.core.ui.glass.glassOverlayFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.core.ui.glass.rememberReducedMotion
import dev.xboyrex.darkplayer.core.ui.theme.DarkPlayerTheme
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0
    val seconds = abs(tapGestureState.seekMillis.milliseconds.inWholeSeconds).toInt()
    val backdrop = LocalGlassBackdrop.current
    val reduced = rememberReducedMotion()

    val squishX = remember { Animatable(1f) }
    val squishY = remember { Animatable(1f) }
    val dash = remember { Animatable(0f) }
    val sparks = remember { Animatable(1f) }

    LaunchedEffect(tapGestureState.seekMillis) {
        if (reduced) return@LaunchedEffect
        launch { squishX.animateTo(1f, keyframes { durationMillis = 420; 1.14f at 110; 0.96f at 260 }) }
        launch { squishY.animateTo(1f, keyframes { durationMillis = 420; 0.88f at 110; 1.04f at 260 }) }
        launch { dash.animateTo(0f, keyframes { durationMillis = 360; 1f at 110 }) }
        launch {
            sparks.snapTo(0f)
            sparks.animateTo(1f, tween(520, easing = Motion.Emphasized))
        }
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "seekBurstScale",
    )
    val sparkColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (isForward) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    fraction = when (tapGestureState.doubleTapGesture) {
                        DoubleTapGesture.PLAY_PAUSE -> 0f
                        DoubleTapGesture.FAST_FORWARD_AND_REWIND -> 0.5f
                        DoubleTapGesture.BOTH -> 0.35f
                        DoubleTapGesture.NONE -> 0f
                    },
                )
                .clip(
                    shape = if (isForward) {
                        RightSideOvalShape()
                    } else {
                        LeftSideOvalShape()
                    },
                )
                .background(Color.White.copy(alpha = 0.14f))
                .indication(
                    tapGestureState.interactionSource,
                    indication = ripple(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale * squishX.value
                            scaleY = scale * squishY.value
                        }
                        .drawBehind {
                            val t = sparks.value
                            if (t >= 1f) return@drawBehind
                            val reach = size.width / 2f + 16.dp.toPx()
                            for (i in 0 until 8) {
                                val a = (i / 8f + 1f / 16f) * 2f * PI.toFloat()
                                val from = Offset(cos(a) * size.width * 0.36f, sin(a) * 12.dp.toPx())
                                val to = Offset(cos(a) * reach, sin(a) * reach * 0.62f)
                                drawCircle(
                                    color = (if (i % 2 == 0) sparkColor else Color.White).copy(alpha = 1f - t),
                                    radius = 3.5.dp.toPx() * (1f - 0.65f * t),
                                    center = center + from + (to - from) * t,
                                )
                            }
                        }
                        .height(46.dp)
                        .clip(CircleShape)
                        .then(
                            if (backdrop != null && GlassSettings.quality != GlassQuality.Off) {
                                Modifier.liquidGlass(
                                    backdrop = backdrop,
                                    spec = glassSpec(
                                        shape = CircleShape,
                                        tint = glassOverlayFill(0.46f),
                                        blur = 18.dp,
                                        refractionHeight = 10.dp,
                                        refractionAmount = 16.dp,
                                    ),
                                )
                            } else {
                                Modifier
                                    .background(Color.Black.copy(alpha = 0.48f))
                                    .border(
                                        width = 0.8.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.28f),
                                                Color.White.copy(alpha = 0.08f),
                                            ),
                                        ),
                                        shape = CircleShape,
                                    )
                            },
                        )
                        .padding(start = 14.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer {
                                translationX = (if (isForward) 1 else -1) * 6.dp.toPx() * dash.value
                            },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isForward) "+" else "−",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                    RollingText(
                        text = "${seconds}s",
                        color = Color.White,
                        up = true,
                        fontSize = 18.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                DoubleTapSeekTriangles(isForward = isForward)
            }
        }
    }
}

@Composable
private fun DoubleTapSeekTriangles(isForward: Boolean, modifier: Modifier = Modifier) {
    val animationDuration = 750L

    val alpha1 = remember { Animatable(initialValue = 0f) }
    val alpha2 = remember { Animatable(initialValue = 0f) }
    val alpha3 = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(key1 = animationDuration) {
        while (true) {
            alpha1.animateTo(
                targetValue = 1f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
            alpha2.animateTo(
                targetValue = 1f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
            alpha3.animateTo(
                targetValue = 1f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
            alpha1.animateTo(
                targetValue = 0f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
            alpha2.animateTo(
                targetValue = 0f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
            alpha3.animateTo(
                targetValue = 0f,
                animationSpec = tween((animationDuration / 5).toInt()),
            )
        }
    }

    val rotation = if (isForward) 0f else 180f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.rotate(degrees = rotation),
    ) {
        DoubleTapArrow(alpha1.value)
        DoubleTapArrow(alpha2.value)
        DoubleTapArrow(alpha3.value)
    }
}

private class RightSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(
                size.width,
                size.height,
            )
            lineTo(
                size.width,
                y = 0f,
            )
            lineTo(
                x = size.width * 0.1f,
                y = 0f,
            )
            cubicTo(
                x1 = size.width * 0.1f,
                y1 = 0f,
                x2 = -size.width * 0.1f,
                y2 = size.height / 2,
                x3 = size.width * 0.1f,
                size.height,
            )
            close()
        }
        return Outline.Generic(path = path)
    }
}

private class LeftSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(
                x = 0f,
                y = 0f,
            )
            lineTo(
                x = size.width * 0.9f,
                y = 0f,
            )
            cubicTo(
                x1 = size.width * 0.9f,
                y1 = 0f,
                x2 = size.width * 1.1f,
                y2 = size.height / 2,
                x3 = size.width * 0.9f,
                size.height,
            )
            lineTo(
                x = 0f,
                size.height,
            )
            close()
        }
        return Outline.Generic(path = path)
    }
}

@Composable
private fun DoubleTapArrow(alpha: Float) {
    Icon(
        painter = painterResource(R.drawable.ic_play),
        contentDescription = null,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { this.alpha = alpha },
        tint = Color.White,
    )
}

@Preview
@Composable
private fun OvalShapePreview() {
    DarkPlayerTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(weight = 1f)
                    .clip(shape = LeftSideOvalShape())
                    .background(Color.Blue),
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(weight = 1f)
                    .clip(shape = RightSideOvalShape())
                    .background(Color.Blue),
            ) {}
        }
    }
}
