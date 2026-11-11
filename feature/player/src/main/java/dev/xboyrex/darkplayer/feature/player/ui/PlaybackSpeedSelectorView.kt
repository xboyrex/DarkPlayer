package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import dev.xboyrex.darkplayer.core.common.extensions.toString
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSlider
import dev.xboyrex.darkplayer.feature.player.state.rememberPlaybackParametersState
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val mxBlue = Color(0xFF38BDF8)

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Playback Speed",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val minValue = 0.25f
            val maxValue = 4.0f
            val stepSize = 0.05f

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = {
                        val newSpeed = (playbackParametersState.speed - stepSize).coerceAtLeast(minValue)
                        playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }

                Text(
                    text = "${playbackParametersState.speed.toString(decimalPlaces = 2)}X",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = mxBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(weight = 1f),
                )

                IconButton(
                    onClick = {
                        val newSpeed = (playbackParametersState.speed + stepSize).coerceAtMost(maxValue)
                        playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlasmioSlider(
                    value = playbackParametersState.speed.coerceIn(minValue, maxValue),
                    valueRange = minValue..maxValue,
                    accentColor = mxBlue,
                    onValueChange = {
                        val stepped = ((it / stepSize).roundToInt() * stepSize).coerceIn(minValue, maxValue)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playbackParametersState.setPlaybackSpeed(speed = stepped)
                    },
                    modifier = Modifier.weight(weight = 1f),
                )
                IconButton(onClick = { playbackParametersState.setPlaybackSpeed(speed = 1f) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reset),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            FlowRow(
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    0.25f, 0.5f, 0.75f, 1.0f,
                    1.25f, 1.5f, 1.75f, 2.0f,
                    2.5f, 3.0f, 3.5f, 4.0f,
                ).forEach { speed ->
                    val isSelectedSpeed = abs(playbackParametersState.speed - speed) < 0.03f
                    Box(
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(
                                if (isSelectedSpeed) mxBlue.copy(alpha = 0.24f)
                                else Color.White.copy(alpha = 0.07f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelectedSpeed) mxBlue else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { playbackParametersState.setPlaybackSpeed(speed = speed) }
                            .padding(horizontal = 8.dp, vertical = 9.dp)
                            .weight(weight = 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${speed}X",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedSpeed) mxBlue else Color.White,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        playbackParametersState.setIsSkipSilenceEnabled(!playbackParametersState.skipSilenceEnabled)
                    }
                    .padding(vertical = 8.dp),
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
