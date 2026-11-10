package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import dev.xboyrex.darkplayer.core.model.DoubleTapGesture
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DoubleTapIndicator(modifier: Modifier = Modifier, tapGestureState: TapGestureState) {
    if (tapGestureState.seekMillis == 0L) return
    val isForward = tapGestureState.seekMillis > 0
    val seconds = abs(tapGestureState.seekMillis).milliseconds.inWholeSeconds
    val shape = if (isForward) RightSideOvalShape() else LeftSideOvalShape()

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
                        DoubleTapGesture.FAST_FORWARD_AND_REWIND -> 0.30f
                        DoubleTapGesture.BOTH -> 0.28f
                        DoubleTapGesture.NONE -> 0f
                    },
                )
                .clip(shape = shape)
                .background(
                    Brush.horizontalGradient(
                        colors = if (isForward) {
                            listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.22f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                        },
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = shape,
                )
                .indication(
                    tapGestureState.interactionSource,
                    indication = ripple(color = Color.White),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DoubleTapSeekTriangles(isForward = isForward)
                Text(
                    text = "${if (isForward) "+" else "-"}$seconds sec",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DoubleTapSeekTriangles(isForward: Boolean, modifier: Modifier = Modifier) {
    val animationDuration = 650L

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
            .size(20.dp)
            .graphicsLayer { this.alpha = alpha },
        tint = Color.White,
    )
}

@Preview
@Composable
private fun OvalShapePreview() {
    NextPlayerTheme {
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
