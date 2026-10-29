package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
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
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 30.dp,
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
    val fillFraction = if (normalizedValue <= NORMAL_MAX_PERCENTAGE) {
        (normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()) * if (maxValue > NORMAL_MAX_PERCENTAGE) 0.5f else 1f
    } else {
        0.5f + ((normalizedValue - NORMAL_MAX_PERCENTAGE).toFloat() / (maxValue - NORMAL_MAX_PERCENTAGE).coerceAtLeast(1).toFloat()) * 0.5f
    }
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && value > NORMAL_MAX_PERCENTAGE

    Column(
        modifier = modifier
            .heightIn(max = 240.dp)
            .clip(shape = CircleShape)
            .background(Color(0xFF080C14).copy(alpha = 0.58f))
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
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(size = width),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = if (isBoostActive) "${normalizedValue}%" else normalizedValue.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isBoostActive) Color(0xFFFACC15) else Color.White,
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
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(width = 12.dp)
                    .fillMaxHeight(fraction = fillFraction)
                    .clip(CircleShape)
                    .background(
                        color = if (isBoostActive) {
                            boostColor
                        } else {
                            Color.White
                        },
                    ),
            )
        }
        Box(
            modifier = Modifier.size(size = width),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (isBoostActive) Color(0xFFFACC15) else Color.White,
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
