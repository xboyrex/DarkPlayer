package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
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

/**
 * Authentic iPhone / iOS Vertical Volume & Brightness HUD Capsule.
 * Fills the entire capsule smoothly from bottom to top with crisp White (or warm Gold when boosted > 100%),
 * and uses dual-layer clipping (like GlasmioVolumeSlider) so the bottom icon and top percentage automatically
 * invert their contrast from White to Dark Charcoal as the fill crosses over them.
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 46.dp,
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
    val targetFillFraction = if (maxValue <= NORMAL_MAX_PERCENTAGE) {
        normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()
    } else if (normalizedValue <= NORMAL_MAX_PERCENTAGE) {
        (normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()) * 0.50f
    } else {
        0.50f + ((normalizedValue - NORMAL_MAX_PERCENTAGE).toFloat() / (maxValue - NORMAL_MAX_PERCENTAGE).coerceAtLeast(1).toFloat()) * 0.50f
    }

    val animatedFillFraction by animateFloatAsState(
        targetValue = targetFillFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 1200f),
        label = "iosVerticalFill",
    )

    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && normalizedValue > NORMAL_MAX_PERCENTAGE
    val fillColor by animateColorAsState(
        targetValue = if (isBoostActive) boostColor else Color.White,
        animationSpec = tween(durationMillis = 120),
        label = "iosFillColor",
    )

    val pillScale by animateFloatAsState(
        targetValue = if (isBoostActive) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
        label = "iosPillScale",
    )

    val capsuleShape = RoundedCornerShape(percent = 50)
    val trackColor = Color(0xFF1C1C1E).copy(alpha = 0.92f)
    val darkContrastColor = Color(0xFF111113)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .width(width = width)
            .height(168.dp)
            .shadow(
                elevation = 18.dp,
                shape = capsuleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape = capsuleShape)
            .drawBehind {
                // 1. iOS Solid Charcoal Capsule Track
                drawRect(color = trackColor)
                // 2. Full-Width Bottom-to-Top iOS Fill
                val fillHeight = size.height * animatedFillFraction
                val topY = (size.height - fillHeight).coerceIn(0f, size.height)
                drawRect(
                    color = fillColor,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, fillHeight),
                )
            },
    ) {
        // Layer 1 (Unfilled background contrast: White text & icon)
        Box(modifier = Modifier.matchParentSize()) {
            Text(
                text = "$normalizedValue",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(21.dp),
            )
        }

        // Layer 2 (Filled foreground contrast: Dark charcoal text & icon clipped to fill region)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val fillHeight = size.height * animatedFillFraction
                    val topY = (size.height - fillHeight).coerceIn(0f, size.height)
                    clipRect(
                        left = 0f,
                        top = topY,
                        right = size.width,
                        bottom = size.height,
                    ) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            Text(
                text = "$normalizedValue",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkContrastColor,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = darkContrastColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(21.dp),
            )
        }
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 65,
            maxValue = 100,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
