package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
    icon: Painter,
    @IntRange(
        from = 0,
        to = 1000,
    ) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = Color(color = 0xFFFACC15),
) {
    val normalizedValue = value.coerceIn(
        minimumValue = 0,
        maximumValue = maxValue,
    )
    val targetFillFraction = if (normalizedValue <= NORMAL_MAX_PERCENTAGE) {
        (normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()) * if (maxValue > NORMAL_MAX_PERCENTAGE) 0.35f else 1f
    } else {
        0.35f + ((normalizedValue - NORMAL_MAX_PERCENTAGE).toFloat() / (maxValue - NORMAL_MAX_PERCENTAGE).coerceAtLeast(1).toFloat()) * 0.65f
    }
    val animatedFillFraction by animateFloatAsState(
        targetValue = targetFillFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "verticalProgressFill",
    )

    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && normalizedValue > NORMAL_MAX_PERCENTAGE
    val boostTier = when {
        !isBoostActive -> 1
        normalizedValue <= 200 -> 2
        normalizedValue <= 300 -> 3
        else -> 4
    }

    val targetAccentColor = when (boostTier) {
        1 -> Color.White
        2 -> boostColor
        3 -> Color(0xFFFB923C)
        else -> Color(0xFFF87171)
    }
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 180),
        label = "verticalProgressColor",
    )

    val pillScale by animateFloatAsState(
        targetValue = when {
            normalizedValue >= 350 -> 1.05f
            isBoostActive -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 480f),
        label = "verticalPillScale",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .heightIn(max = 216.dp)
            .clip(shape = CircleShape)
            .background(Color(0xFF121620).copy(alpha = 0.88f))
            .border(
                width = 0.8.dp,
                color = if (isBoostActive) animatedAccentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                shape = CircleShape,
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(width = width)
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "$normalizedValue",
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = animatedAccentColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = 12.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }

        AnimatedVisibility(
            visible = isBoostActive,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f)) + fadeIn(tween(120)),
            exit = scaleOut(tween(100)) + fadeOut(tween(100)),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(animatedAccentColor.copy(alpha = 0.20f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${boostTier}x",
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = animatedAccentColor,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .width(width = 8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(width = 8.dp)
                    .fillMaxHeight(fraction = animatedFillFraction)
                    .clip(CircleShape)
                    .background(color = animatedAccentColor),
            )
        }
        Box(
            modifier = Modifier.size(size = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = animatedAccentColor,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 300,
            maxValue = 400,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
