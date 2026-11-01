package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.glassOverlayFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.core.ui.theme.DarkPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    icon: Painter,
    @IntRange(
        from = 0,
        to = 200,
    ) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = MaterialTheme.colorScheme.error,
) {
    val backdrop = LocalGlassBackdrop.current
    val normalizedValue = value.coerceIn(
        minimumValue = 0,
        maximumValue = maxValue,
    )
    val rawFillFraction = (normalizedValue.toFloat() / maxValue.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val animatedFillFraction by animateFloatAsState(
        targetValue = rawFillFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "verticalProgressFill",
    )
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && normalizedValue > NORMAL_MAX_PERCENTAGE
    val activeFillColor by animateColorAsState(
        targetValue = if (isBoostActive) boostColor else Color.White,
        label = "verticalProgressColor",
    )

    Column(
        modifier = modifier
            .heightIn(max = 240.dp)
            .clip(shape = CircleShape)
            .then(
                if (backdrop != null && GlassSettings.quality != GlassQuality.Off) {
                    Modifier.liquidGlass(
                        backdrop = backdrop,
                        spec = glassSpec(
                            shape = CircleShape,
                            tint = glassOverlayFill(0.48f),
                            blur = 18.dp,
                            refractionHeight = 10.dp,
                            refractionAmount = 16.dp,
                        ),
                    )
                } else {
                    Modifier
                        .background(Color(0xFF080C14).copy(alpha = 0.62f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.26f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = CircleShape,
                        )
                },
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(size = width),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = normalizedValue.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                ),
            )
        }
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .width(width = 12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(width = 12.dp)
                    .fillMaxHeight(fraction = animatedFillFraction)
                    .clip(CircleShape)
                    .background(color = activeFillColor),
            )
        }
        Box(
            modifier = Modifier.size(size = width),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    DarkPlayerTheme {
        VerticalProgressView(
            value = 50,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
