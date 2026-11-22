package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.feature.player.state.rememberPlaybackParametersState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val playbackSpeed = playbackParametersState.speed
    val skipSilenceEnabled = playbackParametersState.skipSilenceEnabled
    val onPlaybackSpeedChange: (Float) -> Unit = { playbackParametersState.setPlaybackSpeed(it) }
    val onSkipSilenceChange: (Boolean) -> Unit = { playbackParametersState.setIsSkipSilenceEnabled(it) }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val tonalCard = Color(0x16F4EFE8)
    val subtleText = Color(0xFFB3BCB4)
    val ivoryText = Color(0xFFF4EFE8)

    val presetsRow1 = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f)
    val presetsRow2 = listOf(1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    val presetsRow3 = listOf(3.5f, 4.0f)

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Compact borderless stepper header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1EF4EFE8))
                        .clickable {
                            val next = ((playbackSpeed - 0.05f).coerceIn(0.25f, 4.0f) * 20).roundToInt() / 20f
                            onPlaybackSpeedChange(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null, tint = ivoryText, modifier = Modifier.size(18.dp))
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.2f", playbackSpeed),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = ivoryText,
                    )
                    Text(
                        text = "x",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x1EF4EFE8))
                            .clickable {
                                val next = ((playbackSpeed + 0.05f).coerceIn(0.25f, 4.0f) * 20).roundToInt() / 20f
                                onPlaybackSpeedChange(next)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ivoryText, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (abs(playbackSpeed - 1.0f) < 0.01f) accent else Color(0x1EF4EFE8))
                            .clickable { onPlaybackSpeedChange(1.0f) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "1x",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (abs(playbackSpeed - 1.0f) < 0.01f) onAccent else ivoryText,
                        )
                    }
                }
            }

            // Borderless tonal slider card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Slider(
                    value = playbackSpeed.coerceIn(0.25f, 4.0f),
                    onValueChange = { raw ->
                        val snapped = (raw * 20f).roundToInt() / 20f
                        onPlaybackSpeedChange(snapped.coerceIn(0.25f, 4.0f))
                    },
                    valueRange = 0.25f..4.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color(0x2AF4EFE8),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("0.25x", "1.0x", "2.0x", "4.0x").forEach { mark ->
                        Text(
                            text = mark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtleText,
                        )
                    }
                }
            }

            // Borderless preset pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetsRow1.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetsRow2.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetsRow3.forEach { preset ->
                        SpeedPresetPill(
                            modifier = Modifier.weight(1f),
                            speed = preset,
                            selected = abs(playbackSpeed - preset) < 0.02f,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { onPlaybackSpeedChange(preset) },
                        )
                    }
                }
            }

            // Borderless Skip Silence row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tonalCard)
                    .clickable { onSkipSilenceChange(!skipSilenceEnabled) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (skipSilenceEnabled) accent.copy(alpha = 0.22f) else Color(0x1AF4EFE8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (skipSilenceEnabled) accent else subtleText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.skip_silence),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ivoryText,
                    )
                }
                Switch(
                    checked = skipSilenceEnabled,
                    onCheckedChange = onSkipSilenceChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onAccent,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color(0xFFE4E0D8),
                        uncheckedTrackColor = Color(0x2CF4EFE8),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SpeedPresetPill(
    modifier: Modifier = Modifier,
    speed: Float,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color(0x16F4EFE8),
        animationSpec = tween(140),
        label = "speed_pill_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) onAccent else Color(0xFFE8E4DC),
        animationSpec = tween(140),
        label = "speed_pill_fg",
    )
    val label = if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}.0x"
    } else {
        "${speed}x"
    }
    Box(
        modifier = modifier
            .calmPressScale(pressedScale = 0.92f)
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
        )
    }
}
