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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
    width: Dp = 42.dp,
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
    // Linear 0..100% for normal range, smooth proportional extension for 100..400% boost
    val targetFillFraction = if (maxValue <= NORMAL_MAX_PERCENTAGE) {
        normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()
    } else if (normalizedValue <= NORMAL_MAX_PERCENTAGE) {
        (normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()) * 0.50f
    } else {
        0.50f + ((normalizedValue - NORMAL_MAX_PERCENTAGE).toFloat() / (maxValue - NORMAL_MAX_PERCENTAGE).coerceAtLeast(1).toFloat()) * 0.50f
    }

    val animatedFillFraction by animateFloatAsState(
        targetValue = targetFillFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
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
        animationSpec = tween(durationMillis = 120),
        label = "verticalProgressColor",
    )

    val pillScale by animateFloatAsState(
        targetValue = if (isBoostActive) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "verticalPillScale",
    )

    val capsuleShape = RoundedCornerShape(50)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .width(52.dp)
            .heightIn(min = 188.dp, max = 216.dp)
            .shadow(
                elevation = 16.dp,
                shape = capsuleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape = capsuleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1D26).copy(alpha = 0.94f),
                        Color(0xFF101218).copy(alpha = 0.96f),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (isBoostActive) animatedAccentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                shape = capsuleShape,
            )
            .padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Exact Numeric Percentage Readout
        Box(
            modifier = Modifier
                .width(width = width)
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "$normalizedValue%",
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = animatedAccentColor,
                    fontFamily = FontFamily.Monospace,
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
            enter = scaleIn(spring(dampingRatio = 0.65f, stiffness = 600f)) + fadeIn(tween(100)),
            exit = scaleOut(tween(90)) + fadeOut(tween(90)),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(animatedAccentColor.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 1.5.dp),
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

        // High-Precision Vertical Fill Track
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .width(width = 10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(width = 10.dp)
                    .fillMaxHeight(fraction = animatedFillFraction)
                    .clip(CircleShape)
                    .background(color = animatedAccentColor),
            )
        }

        // Bottom Indicator Icon inside subtle circle
        Box(
            modifier = Modifier
                .size(size = 28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = animatedAccentColor,
                modifier = Modifier.size(17.dp),
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
