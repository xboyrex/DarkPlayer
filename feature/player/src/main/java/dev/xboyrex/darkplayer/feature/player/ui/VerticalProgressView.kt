package dev.xboyrex.darkplayer.feature.player.ui

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.state.VolumeState
import kotlin.math.roundToInt

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Sleek frosted slate-navy vertical pill matching 3114.png:
 * - Top speaker/brightness icon + live percentage/boost badge
 * - Smooth 5.5dp vertical slider groove (#0E1118) with crisp white/gold fill bar
 * - Secondary Bluetooth / Auto-brightness icon
 * - Subtle divider + bottom '•••' button that opens the 5-slider Sound & Vibration panel (3113.png).
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    icon: Painter,
    @IntRange(
        from = 0,
        to = 1000,
    ) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    isVolume: Boolean = true,
    boostColor: Color = Color(color = 0xFFFACC15),
    onValueChange: ((Int) -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
) {
    val normalizedValue = value.coerceIn(
        minimumValue = 0,
        maximumValue = maxValue,
    )
    // 0..100% fills 0..100% of the vertical track cleanly so brightness & normal volume look proportional;
    // when boosted above 100%, the track stays full and lights up with the boost accent color + badge.
    val rawTrackFraction = when {
        normalizedValue <= 0 -> 0f
        normalizedValue <= NORMAL_MAX_PERCENTAGE ->
            (normalizedValue.toFloat() / NORMAL_MAX_PERCENTAGE.toFloat()).coerceIn(0.06f, 1f)
        else -> 1f
    }
    val animatedFillFraction by animateFloatAsState(
        targetValue = rawTrackFraction,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 600f),
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
        animationSpec = tween(durationMillis = 160),
        label = "verticalProgressColor",
    )

    val pillShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .width(width)
            .height(192.dp)
            .shadow(
                elevation = 20.dp,
                shape = pillShape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape = pillShape)
            .background(Color(0xFF232834))
            .border(
                width = 1.dp,
                color = if (isBoostActive) {
                    animatedAccentColor.copy(alpha = 0.48f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                shape = pillShape,
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // 1. Top Speaker / Brightness Icon
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(enabled = onValueChange != null) {
                    if (normalizedValue > 0) {
                        onValueChange?.invoke(0)
                    } else {
                        onValueChange?.invoke(50)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = animatedAccentColor,
                modifier = Modifier.size(19.dp),
            )
        }

        // Numeric percentage + optional 2x/3x/4x boost badge
        Text(
            text = if (isBoostActive) "${normalizedValue}% (${boostTier}x)" else "$normalizedValue",
            fontSize = if (isBoostActive) 9.sp else 10.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = animatedAccentColor.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        // 2. Center Vertical Slider Groove (#0E1118) + Crisp White/Gold Fill Bar (matching 3114.png)
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .width(width = 18.dp)
                .pointerInput(maxValue, onValueChange) {
                    if (onValueChange == null) return@pointerInput
                    val effectiveMax = NORMAL_MAX_PERCENTAGE
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val h = size.height.coerceAtLeast(1).toFloat()
                        fun updateFromY(y: Float) {
                            val fraction = (1f - (y / h)).coerceIn(0f, 1f)
                            onValueChange((fraction * effectiveMax).roundToInt())
                        }
                        updateFromY(down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                updateFromY(change.position.y)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E1118)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(5.5.dp)
                        .fillMaxHeight(fraction = animatedFillFraction)
                        .clip(CircleShape)
                        .background(color = animatedAccentColor),
                )
            }
        }

        // 3. Secondary Icon (Bluetooth/Boost on Volume bar, Auto-Brightness on Brightness bar)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(enabled = onSecondaryClick != null) {
                    onSecondaryClick?.invoke()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isVolume) Icons.Rounded.Bluetooth else Icons.Rounded.BrightnessAuto,
                contentDescription = null,
                tint = if (isBoostActive) animatedAccentColor else Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(16.dp),
            )
        }

        // 4. Thin Horizontal Divider (matching 3114.png)
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.16f)),
        )

        // 5. Bottom '•••' More Options Button (opens 3113.png Sound & Vibration panel)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(enabled = onExpandClick != null) {
                    onExpandClick?.invoke()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Sound and vibration options",
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Expanded 5-channel "Sound and vibration" floating modal matching screenshot 3113.png:
 * - 5 vertical interactive sliders: Alarm, Notification, Ring/Vibration, Voice/Preamp, and Media/Bluetooth
 * - Each slider is wired to real Android AudioManager streams & DarkPlayer audio engine
 * - Bottom bar with "Done" pill button on left and Settings gear icon on right
 */
@Composable
fun SoundAndVibrationPanel(
    visible: Boolean,
    volumeState: VolumeState?,
    player: Player?,
    onDismiss: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun readStreamFraction(streamType: Int): Float = runCatching {
        val max = audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(streamType).coerceIn(0, max)
        cur.toFloat() / max.toFloat()
    }.getOrDefault(0.7f)

    fun writeStreamFraction(streamType: Int, fraction: Float) {
        runCatching {
            val max = audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1)
            val target = (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
            audioManager.setStreamVolume(streamType, target, 0)
        }
    }

    var alarmFraction by remember(visible) {
        mutableFloatStateOf(readStreamFraction(AudioManager.STREAM_ALARM))
    }
    var notificationFraction by remember(visible) {
        mutableFloatStateOf(readStreamFraction(AudioManager.STREAM_NOTIFICATION))
    }
    var ringFraction by remember(visible) {
        mutableFloatStateOf(readStreamFraction(AudioManager.STREAM_RING))
    }
    var voiceFraction by remember(visible, player) {
        mutableFloatStateOf((player?.volume ?: 1f).coerceIn(0f, 1f))
    }
    val mediaFraction = remember(volumeState?.volumePercentage) {
        ((volumeState?.volumePercentage ?: 70).coerceIn(0, 100)) / 100f
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) +
            fadeIn(tween(160)),
        exit = scaleOut(tween(140), targetScale = 0.92f) + fadeOut(tween(130)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 332.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black,
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF232834))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { }
                    .padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Sound and vibration",
                    color = Color.White,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 5 Vertical Channel Sliders matching 3113.png
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoundChannelVerticalSlider(
                        fraction = alarmFraction,
                        icon = Icons.Rounded.AccessAlarm,
                        contentDescription = "Alarm volume",
                        onFractionChange = { f ->
                            alarmFraction = f
                            writeStreamFraction(AudioManager.STREAM_ALARM, f)
                        },
                    )
                    SoundChannelVerticalSlider(
                        fraction = notificationFraction,
                        icon = Icons.Rounded.NotificationsNone,
                        contentDescription = "Notification volume",
                        onFractionChange = { f ->
                            notificationFraction = f
                            writeStreamFraction(AudioManager.STREAM_NOTIFICATION, f)
                        },
                    )
                    SoundChannelVerticalSlider(
                        fraction = ringFraction,
                        icon = Icons.Rounded.Vibration,
                        contentDescription = "Ring and vibration volume",
                        onFractionChange = { f ->
                            ringFraction = f
                            writeStreamFraction(AudioManager.STREAM_RING, f)
                        },
                    )
                    SoundChannelVerticalSlider(
                        fraction = voiceFraction,
                        icon = Icons.Rounded.Call,
                        contentDescription = "Player voice & dialogue level",
                        onFractionChange = { f ->
                            voiceFraction = f
                            player?.volume = f.coerceIn(0f, 1f)
                            writeStreamFraction(AudioManager.STREAM_VOICE_CALL, f)
                        },
                    )
                    SoundChannelVerticalSlider(
                        fraction = mediaFraction,
                        icon = Icons.Rounded.Bluetooth,
                        contentDescription = "Media and Bluetooth volume",
                        onFractionChange = { f ->
                            val pct = (f * 100f).roundToInt().coerceIn(0, 100)
                            volumeState?.updateVolumePercentage(pct)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.14f),
                    thickness = 0.8.dp,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Row: "Done" pill on left, Settings Gear icon on right (matching 3113.png)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .pressable(onClick = onDismiss)
                            .padding(horizontal = 22.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .pressable {
                                onDismiss()
                                onOpenAudioSettings()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open Equalizer & Audio Settings",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundChannelVerticalSlider(
    fraction: Float,
    icon: ImageVector,
    contentDescription: String,
    onFractionChange: (Float) -> Unit,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = if (clamped <= 0f) 0f else clamped.coerceIn(0.06f, 1f),
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 620f),
        label = "channelFill",
    )

    Column(
        modifier = Modifier
            .width(42.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(28.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val h = size.height.coerceAtLeast(1).toFloat()
                        fun updateFromY(y: Float) {
                            val next = (1f - (y / h)).coerceIn(0f, 1f)
                            onFractionChange(next)
                        }
                        updateFromY(down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                updateFromY(change.position.y)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E1118)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(5.5.dp)
                        .fillMaxHeight(fraction = animatedFill)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(21.dp)
                .clickable {
                    onFractionChange(if (clamped > 0.05f) 0f else 0.75f)
                },
        )
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 75,
            maxValue = 200,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
