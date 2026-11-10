package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.glass.VolumeHudStyle
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Multi-Style Vertical Volume & Brightness HUD supporting all 5 designs from 4313.jpg:
 * - Apple iOS (APPLE_IOS)
 * - Google Pixel M3 (GOOGLE_M3)
 * - Samsung One UI (SAMSUNG_ONEUI)
 * - Xiaomi HyperOS (XIAOMI_MIUI)
 * - OnePlus OxygenOS (ONEPLUS_OXYGEN)
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    icon: Painter,
    @IntRange(from = 0, to = 200) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = Color(0xFFFACC15),
    styleOverride: VolumeHudStyle? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        UiStylePreferences.init(context)
    }
    val style = styleOverride ?: UiStylePreferences.volumeHudStyle
    val safeMax = maxValue.coerceAtLeast(NORMAL_MAX_PERCENTAGE)
    val clampedValue = value.coerceIn(0, safeMax)
    val isBoosted = clampedValue > NORMAL_MAX_PERCENTAGE

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
        label = "hudFillFraction",
    )
    val animatedBoostFraction by animateFloatAsState(
        targetValue = boostTargetFraction,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 1400f),
        label = "hudBoostFraction",
    )

    when (style) {
        VolumeHudStyle.GOOGLE_M3 -> GooglePixelVolumeHud(
            modifier = modifier,
            icon = icon,
            value = clampedValue,
            fillFraction = animatedFillFraction,
            boostFraction = animatedBoostFraction,
            isBoosted = isBoosted,
            boostColor = boostColor,
        )
        VolumeHudStyle.SAMSUNG_ONEUI -> SamsungOneUiVolumeHud(
            modifier = modifier,
            icon = icon,
            value = clampedValue,
            fillFraction = animatedFillFraction,
            boostFraction = animatedBoostFraction,
            isBoosted = isBoosted,
            boostColor = boostColor,
        )
        VolumeHudStyle.XIAOMI_MIUI -> XiaomiHyperOsVolumeHud(
            modifier = modifier,
            icon = icon,
            value = clampedValue,
            fillFraction = animatedFillFraction,
            boostFraction = animatedBoostFraction,
            isBoosted = isBoosted,
            boostColor = boostColor,
        )
        VolumeHudStyle.ONEPLUS_OXYGEN -> OnePlusOxygenVolumeHud(
            modifier = modifier,
            icon = icon,
            value = clampedValue,
            fillFraction = animatedFillFraction,
            boostFraction = animatedBoostFraction,
            isBoosted = isBoosted,
            boostColor = boostColor,
        )
        VolumeHudStyle.APPLE_IOS -> AppleIosVolumeHud(
            modifier = modifier,
            width = width,
            icon = icon,
            value = clampedValue,
            fillFraction = animatedFillFraction,
            boostFraction = animatedBoostFraction,
            isBoosted = isBoosted,
            boostColor = boostColor,
        )
    }
}

@Composable
private fun AppleIosVolumeHud(
    modifier: Modifier,
    width: Dp,
    icon: Painter,
    value: Int,
    fillFraction: Float,
    boostFraction: Float,
    isBoosted: Boolean,
    boostColor: Color,
) {
    val pillWidth by animateDpAsState(
        targetValue = if (isBoosted) width + 2.dp else width,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 1200f),
        label = "iosHudWidth",
    )
    val capsuleShape = RoundedCornerShape(18.dp)
    val trackColor = Color(0xFF282644).copy(alpha = 0.88f)
    val fillWhite = Color(0xFFFAF6FF)
    val glyphOnDark = Color(0xFFF2F2F7)
    val glyphOnLight = Color(0xFF1D1B30)

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(168.dp)
            .shadow(16.dp, capsuleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(capsuleShape)
            .background(trackColor)
            .drawBehind {
                val fillHeight = size.height * fillFraction
                if (fillHeight > 0.5f) {
                    drawRect(
                        color = fillWhite,
                        topLeft = Offset(0f, size.height - fillHeight),
                        size = Size(size.width, fillHeight),
                    )
                }
                if (boostFraction > 0.005f) {
                    val boostHeight = size.height * boostFraction
                    drawRect(
                        color = boostColor,
                        topLeft = Offset(0f, size.height - boostHeight),
                        size = Size(size.width, boostHeight),
                    )
                }
            }
            .border(
                width = 0.8.dp,
                color = if (isBoosted) boostColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.18f),
                shape = capsuleShape,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (isBoosted) "${value}%" else "$value",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = glyphOnDark.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val activeFraction = maxOf(fillFraction, boostFraction)
                    val fillTop = size.height * (1f - activeFraction)
                    clipRect(0f, fillTop, size.width, size.height) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            Text(
                text = if (isBoosted) "${value}%" else "$value",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = glyphOnLight,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
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

/**
 * Google Pixel Material 3 style from 4313.jpg:
 * Outer light pill container with top circular speaker/brightness badge, inner rounded track + purple/accent pill thumb with icon,
 * bottom 3-dots, and separate circular mute/caption button below.
 */
@Composable
private fun GooglePixelVolumeHud(
    modifier: Modifier,
    icon: Painter,
    value: Int,
    fillFraction: Float,
    boostFraction: Float,
    isBoosted: Boolean,
    boostColor: Color,
) {
    val outerColor = Color(0xFFF5EFFB)
    val deepAccent = if (isBoosted) boostColor else Color(0xFF4F466E)
    val trackGray = Color(0xFFC8C2D6)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .width(54.dp)
                .height(228.dp)
                .shadow(18.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(outerColor)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top circular icon badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(deepAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (isBoosted) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Inner track with thick rounded pill thumb
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(136.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(trackGray),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val activeFrac = if (isBoosted) 1f else fillFraction.coerceAtLeast(0.22f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp * activeFrac)
                        .clip(RoundedCornerShape(999.dp))
                        .background(deepAccent),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = if (isBoosted) Color.Black else Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "$value",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBoosted) Color.Black else Color.White,
                        )
                    }
                }
            }

            // Three dots at bottom of pill
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = Color(0xFF4F466E),
                modifier = Modifier.size(20.dp),
            )
        }

        // Separate circular button below
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(outerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SubtitlesOff,
                contentDescription = null,
                tint = Color(0xFF4F466E),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Samsung One UI style from 4313.jpg:
 * Slim vertical capsule with 3 dots at top, two-tone grey/white fill, and bottom icon + value.
 */
@Composable
private fun SamsungOneUiVolumeHud(
    modifier: Modifier,
    icon: Painter,
    value: Int,
    fillFraction: Float,
    boostFraction: Float,
    isBoosted: Boolean,
    boostColor: Color,
) {
    val capsuleShape = RoundedCornerShape(999.dp)
    val trackGray = Color(0xFFA2A4B0).copy(alpha = 0.92f)
    val fillWhite = if (isBoosted) boostColor else Color(0xFFF5F6FA)

    Box(
        modifier = modifier
            .width(42.dp)
            .height(206.dp)
            .shadow(16.dp, capsuleShape)
            .clip(capsuleShape)
            .background(trackGray)
            .drawBehind {
                val fillH = size.height * fillFraction
                if (fillH > 0.5f) {
                    drawRect(
                        color = fillWhite,
                        topLeft = Offset(0f, size.height - fillH),
                        size = Size(size.width, fillH),
                    )
                }
            },
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreHoriz,
            contentDescription = null,
            tint = Color(0xFF1E1F26),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(18.dp),
        )
        Text(
            text = "$value",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F26),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color(0xFF1E1F26),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(20.dp),
        )
    }
}

/**
 * Xiaomi HyperOS / MIUI style from 4313.jpg:
 * Wide rounded pill with top dots, white rising fill, bottom music/sun icon, and two stacked oval buttons below.
 */
@Composable
private fun XiaomiHyperOsVolumeHud(
    modifier: Modifier,
    icon: Painter,
    value: Int,
    fillFraction: Float,
    boostFraction: Float,
    isBoosted: Boolean,
    boostColor: Color,
) {
    val barShape = RoundedCornerShape(20.dp)
    val trackSlate = Color(0xFF544B7E).copy(alpha = 0.94f)
    val fillColor = if (isBoosted) boostColor else Color(0xFFFAF8FF)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(194.dp)
                .shadow(16.dp, barShape)
                .clip(barShape)
                .background(trackSlate)
                .drawBehind {
                    val fillH = size.height * fillFraction
                    if (fillH > 0.5f) {
                        drawRect(
                            color = fillColor,
                            topLeft = Offset(0f, size.height - fillH),
                            size = Size(size.width, fillH),
                        )
                    }
                },
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(18.dp),
            )
            Text(
                text = if (isBoosted) "${value}%" else "$value",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (fillFraction > 0.82f) Color(0xFF3E3566) else Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (fillFraction > 0.16f) Color(0xFF4B4275) else Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(22.dp),
            )
        }

        // Bottom pill 1 (Notifications / Audio Boost)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(34.dp)
                .shadow(10.dp, RoundedCornerShape(17.dp))
                .clip(RoundedCornerShape(17.dp))
                .background(trackSlate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }

        // Bottom pill 2 (Night mode moon)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(34.dp)
                .shadow(10.dp, RoundedCornerShape(17.dp))
                .clip(RoundedCornerShape(17.dp))
                .background(trackSlate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/**
 * OnePlus OxygenOS style from 4313.jpg:
 * Tall rounded vertical bar with smoky slate upper track, pure white rising fill, bottom icon,
 * and two white oval pills below.
 */
@Composable
private fun OnePlusOxygenVolumeHud(
    modifier: Modifier,
    icon: Painter,
    value: Int,
    fillFraction: Float,
    boostFraction: Float,
    isBoosted: Boolean,
    boostColor: Color,
) {
    val barShape = RoundedCornerShape(18.dp)
    val trackSlate = Color(0xFF6B6586).copy(alpha = 0.94f)
    val fillColor = if (isBoosted) boostColor else Color.White

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(214.dp)
                .shadow(16.dp, barShape)
                .clip(barShape)
                .background(trackSlate)
                .drawBehind {
                    val fillH = size.height * fillFraction
                    if (fillH > 0.5f) {
                        drawRect(
                            color = fillColor,
                            topLeft = Offset(0f, size.height - fillH),
                            size = Size(size.width, fillH),
                        )
                    }
                },
        ) {
            Text(
                text = if (isBoosted) "${value}%" else "$value",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (fillFraction > 0.85f) Color(0xFF2B2838) else Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp),
            )
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (fillFraction > 0.15f) Color(0xFF3B384A) else Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(22.dp),
            )
        }

        // White oval pill 1 (...)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(34.dp)
                .shadow(10.dp, RoundedCornerShape(17.dp))
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(20.dp),
            )
        }

        // White oval pill 2 (Subtitles/Caption)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(34.dp)
                .shadow(10.dp, RoundedCornerShape(17.dp))
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    NextPlayerTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VolumeHudStyle.entries.forEach { style ->
                VerticalProgressView(
                    value = 65,
                    icon = painterResource(R.drawable.ic_volume),
                    styleOverride = style,
                )
            }
        }
    }
}
