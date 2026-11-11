package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.model.VolumeBrightnessBarStyle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import kotlin.math.roundToInt

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Multi-Style Vertical Progress Indicator for Volume & Brightness in Nova Player.
 * Supports:
 * 1. `MX_PRO_VERTICAL` — MX Player Pro style vertical bar with 0–15 / 16–30 (200% boost) level display and icon (Fixes Bug #16)
 * 2. `GLASMIO_LIQUID_CAPSULE` — Glasmio iOS 27 Liquid Glass vertical capsule with dual-clipped icon
 * 3. `NEXT_VERTICAL_PILL` — Classic Material vertical pill
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
    icon: Painter,
    @IntRange(
        from = 0,
        to = 200,
    ) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    style: VolumeBrightnessBarStyle = VolumeBrightnessBarStyle.MX_PRO_VERTICAL,
    isVolume: Boolean = false,
    boostColor: Color = Color(color = 0xFFFF8C28),
) {
    val normalizedValue = value.coerceIn(
        minimumValue = 0,
        maximumValue = maxValue.coerceAtLeast(1),
    )
    val rawFraction = (normalizedValue.toFloat() / maxValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "verticalBarFill",
    )
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && normalizedValue > NORMAL_MAX_PERCENTAGE
    val accent = LocalNovaAccentColor.current

    when (style) {
        VolumeBrightnessBarStyle.MX_PRO_VERTICAL -> {
            // MX Player Pro displays 0..15 for standard range and 16..30 when Volume Boost (up to 200%) is active
            val mxStepValue = if (isVolume) {
                ((normalizedValue / 100f) * 15f).roundToInt().coerceIn(0, 30)
            } else {
                ((normalizedValue / 100f) * 15f).roundToInt().coerceIn(0, 15)
            }
            Column(
                modifier = modifier
                    .height(210.dp)
                    .width(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF10131C).copy(alpha = 0.84f))
                    .border(
                        width = 1.dp,
                        color = if (isBoostActive) boostColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(25.dp),
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (isBoostActive) boostColor else Color.White,
                    modifier = Modifier.size(20.dp),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(fraction = animatedFraction)
                            .clip(CircleShape)
                            .background(
                                brush = if (isBoostActive) {
                                    Brush.verticalGradient(
                                        listOf(boostColor, accent),
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(accent, accent.copy(alpha = 0.85f)),
                                    )
                                },
                            ),
                    )
                }

                Text(
                    text = if (isVolume) "$mxStepValue" else "$normalizedValue%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBoostActive) boostColor else Color.White,
                    maxLines = 1,
                )
            }
        }

        VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE -> {
            // Glasmio iOS 27 Liquid Glass Vertical Capsule with clipped dual-contrast icon & label
            Box(
                modifier = modifier
                    .height(210.dp)
                    .width(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1E26).copy(alpha = 0.68f))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // White / Boost liquid fill from bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val fillTop = size.height * (1f - animatedFraction)
                            clipRect(top = fillTop, bottom = size.height) {
                                drawRect(if (isBoostActive) boostColor else Color.White)
                            }
                        },
                )

                // Unfilled layer content (white text & icon)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$normalizedValue%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Filled clipped layer content (dark text & icon for contrast on white fill)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val fillTop = size.height * (1f - animatedFraction)
                            clipRect(top = fillTop, bottom = size.height) {
                                this@drawWithContent.drawContent()
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "$normalizedValue%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL -> {
            Column(
                modifier = modifier
                    .heightIn(max = 220.dp)
                    .clip(shape = CircleShape)
                    .background(Color(0xFF14161E).copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        .width(width = width)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .width(width = width)
                            .fillMaxHeight(fraction = animatedFraction)
                            .background(
                                color = if (isBoostActive) {
                                    boostColor
                                } else {
                                    accent
                                },
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
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
            isVolume = true,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
