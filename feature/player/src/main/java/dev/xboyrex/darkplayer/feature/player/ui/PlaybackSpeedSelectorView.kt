package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.common.extensions.round
import dev.xboyrex.darkplayer.core.common.extensions.toString
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.feature.player.state.rememberPlaybackParametersState
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaFluidSlider

/**
 * Redesigned MX Player Pro / Glasmio Liquid Playback Speed Panel
 * Fixes Bug #13 (`153833_Nova Player.png`):
 * Replaces the cluttered 15-button grid and yellow slider with a sleek MX/Glasmio speed control,
 * clean 8 preset chips, and GlasmioSwitch for Skip silence.
 */
@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val accent = LocalNovaAccentColor.current

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Playback Speed",
        widthFraction = 0.40f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val minValue = 0.25f
            val maxValue = 4.0f
            val stepSize = 0.05f

            // Current speed display with - / + step buttons and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            val newSpeed = (playbackParametersState.speed - stepSize)
                                .round(2)
                                .coerceAtLeast(minValue)
                            playbackParametersState.setPlaybackSpeed(newSpeed)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = "Decrease Speed",
                        tint = Color.White,
                    )
                }

                Text(
                    text = "${playbackParametersState.speed.toString(decimalPlaces = 2)}x",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    ),
                    textAlign = TextAlign.Center,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                val newSpeed = (playbackParametersState.speed + stepSize)
                                    .round(2)
                                    .coerceAtMost(maxValue)
                                playbackParametersState.setPlaybackSpeed(newSpeed)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Increase Speed",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { playbackParametersState.setPlaybackSpeed(1.0f) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reset),
                            contentDescription = "Reset Speed",
                            tint = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // Glasmio Liquid Slider for smooth continuous speed control
            NovaFluidSlider(
                value = playbackParametersState.speed.coerceIn(minValue, maxValue),
                valueRange = minValue..maxValue,
                onValueChange = {
                    val stepped = ((it / 0.05f).toInt() * 0.05f).round(2).coerceIn(minValue, maxValue)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playbackParametersState.setPlaybackSpeed(speed = stepped)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Clean 4x2 MX Speed Presets
            FlowRow(
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(
                    0.5f,
                    0.75f,
                    1.0f,
                    1.25f,
                    1.5f,
                    2.0f,
                    3.0f,
                    4.0f,
                ).forEach { speed ->
                    val isSelected = kotlin.math.abs(playbackParametersState.speed - speed) < 0.02f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accent else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { playbackParametersState.setPlaybackSpeed(speed = speed) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${speed}x",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accent else Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(10.dp))
                    .clickable {
                        playbackParametersState.setIsSkipSilenceEnabled(
                            !playbackParametersState.skipSilenceEnabled,
                        )
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.skip_silence),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.weight(weight = 1f),
                )
                NextSwitch(
                    checked = playbackParametersState.skipSilenceEnabled,
                    onCheckedChange = {
                        playbackParametersState.setIsSkipSilenceEnabled(enabled = it)
                    },
                )
            }
        }
    }
}
