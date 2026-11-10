package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Authentic 1:1 iPhone / iOS 18 / iOS 27 Vertical Volume & Brightness HUD Capsule.
 *
 * - Single sleek continuous capsule pill (`46dp` wide × `168dp` tall) with zero extra outer boxes.
 * - Smooth spring-animated bottom-to-top white fill (`0%..100%`), plus warm iOS amber boost fill (`100%..200%`)
 *   when NextPlayer's native LoudnessEnhancer volume boost is active.
 * - Dual-layer contrast clipping (`clipRect`) on both the bottom icon and top percentage label so
 *   the glyph and text automatically invert from crisp white (`#F2F2F7`) on the dark track to deep
 *   charcoal (`#111114`) as the white fill rises over them.
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 46.dp,
    icon: Painter,
    @IntRange(
        from = 0,
        to = 200,
    ) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = Color(color = 0xFFFACC15),
) {
    val safeMax = maxValue.coerceAtLeast(NORMAL_MAX_PERCENTAGE)
    val clampedValue = value.coerceIn(0, safeMax)
    val isBoosted = clampedValue > NORMAL_MAX_PERCENTAGE

    // Normal fill rises from 0% to 100%; boost fill rises from 100% to 200%
    val normalTargetFraction = (clampedValue.coerceAtMost(NORMAL_MAX_PERCENTAGE).toFloat() / NORMAL_MAX_PERCENTAGE.toFloat())
        .coerceIn(0f, 1f)
    val boostTargetFraction = if (isBoosted && safeMax > NORMAL_MAX_PERCENTAGE) {
        ((clampedValue - NORMAL_MAX_PERCENTAGE).toFloat() / (safeMax - NORMAL_MAX_PERCENTAGE).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedFillFraction by animateFloatAsState(
        targetValue = normalTargetFraction,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 1400f),
        label = "iosHudFillFraction",
    )
    val animatedBoostFraction by animateFloatAsState(
        targetValue = boostTargetFraction,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 1400f),
        label = "iosHudBoostFraction",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (isBoosted) width + 2.dp else width,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 1200f),
        label = "iosHudWidth",
    )

    val capsuleShape = RoundedCornerShape(percent = 50)
    val trackColor = Color(0xFF1C1C1E).copy(alpha = 0.88f)
    val fillWhite = Color(0xFFFFFFFF)
    val glyphOnDark = Color(0xFFF2F2F7)
    val glyphOnLight = Color(0xFF111114)

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(168.dp)
            .shadow(
                elevation = 16.dp,
                shape = capsuleShape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.55f),
            )
            .clip(capsuleShape)
            .background(trackColor)
            .drawBehind {
                val fillHeight = size.height * animatedFillFraction
                if (fillHeight > 0.5f) {
                    drawRect(
                        color = fillWhite,
                        topLeft = Offset(0f, size.height - fillHeight),
                        size = Size(size.width, fillHeight),
                    )
                }
                if (animatedBoostFraction > 0.005f) {
                    val boostHeight = size.height * animatedBoostFraction
                    drawRect(
                        color = boostColor,
                        topLeft = Offset(0f, size.height - boostHeight),
                        size = Size(size.width, boostHeight),
                    )
                }
            }
            .border(
                width = 0.75.dp,
                color = if (isBoosted) boostColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.16f),
                shape = capsuleShape,
            ),
    ) {
        // Layer 1: Unfilled dark track layer (white icon at bottom + subtle percentage at top)
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "$clampedValue",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = glyphOnDark.copy(alpha = 0.82f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 13.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = glyphOnDark,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(21.dp),
            )
        }

        // Layer 2: Clipped to the rising white/boost fill (dark charcoal icon + percentage)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val activeFraction = maxOf(animatedFillFraction, animatedBoostFraction)
                    val fillTop = size.height * (1f - activeFraction)
                    clipRect(
                        left = 0f,
                        top = fillTop,
                        right = size.width,
                        bottom = size.height,
                    ) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            Text(
                text = "$clampedValue",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = glyphOnLight,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 13.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = glyphOnLight,
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
            value = 50,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}

@Preview
@Composable
private fun VerticalProgressBoostPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 150,
            maxValue = 200,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
