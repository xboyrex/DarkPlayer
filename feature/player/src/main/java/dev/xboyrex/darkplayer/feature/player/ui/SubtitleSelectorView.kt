package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.OptIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSlider
import dev.xboyrex.darkplayer.feature.player.extensions.getName
import dev.xboyrex.darkplayer.feature.player.state.SubtitleOptionsEvent
import dev.xboyrex.darkplayer.feature.player.state.rememberSubtitleOptionsState
import dev.xboyrex.darkplayer.feature.player.state.rememberTracksState
import kotlin.math.roundToLong

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.SubtitleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onSelectSubtitleClick: () -> Unit,
    onEvent: (SubtitleOptionsEvent) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val subtitleTracksState = rememberTracksState(
        player = player,
        C.TRACK_TYPE_TEXT,
    )
    val subtitleOptionsState = rememberSubtitleOptionsState(
        player = player,
        onEvent = onEvent,
    )
    var showSyncControls by remember { mutableStateOf(false) }
    val mxBlue = Color(0xFF38BDF8)

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Subtitle",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp)
                .selectableGroup(),
        ) {
            subtitleTracksState.tracks.forEachIndexed { index, track ->
                val isSelected = track.isSelected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            subtitleTracksState.switchTrack(if (isSelected) -1 else index)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = mxBlue,
                            uncheckedColor = Color.White.copy(alpha = 0.45f),
                            checkmarkColor = Color.Black,
                        ),
                    )
                    Text(
                        text = track.mediaTrackGroup.getName(
                            trackType = C.TRACK_TYPE_TEXT,
                            index = index,
                        ),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Disable option
            val noneSelected = subtitleTracksState.tracks.none { it.isSelected }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        subtitleTracksState.switchTrack(-1)
                        onDismiss()
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Checkbox(
                    checked = noneSelected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = mxBlue,
                        uncheckedColor = Color.White.copy(alpha = 0.45f),
                        checkmarkColor = Color.Black,
                    ),
                )
                Text(
                    text = stringResource(R.string.disable),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Open local subtitle (matching 155646_MX Player Pro.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectSubtitleClick()
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Open",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Subtitle Synchronization & Speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSyncControls = !showSyncControls }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Synchronization & Speed",
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Text(
                    text = "${subtitleOptionsState.delayMilliseconds} ms",
                    fontSize = 13.5.sp,
                    color = mxBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (showSyncControls) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds - 100L)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_remove),
                                contentDescription = "-100ms",
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = "Delay: ${subtitleOptionsState.delayMilliseconds} ms",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(
                            onClick = {
                                subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds + 100L)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "+100ms",
                                tint = Color.White,
                            )
                        }
                    }

                    Text(
                        text = "Subtitle Speed: ${String.format("%.2fx", subtitleOptionsState.speedMultiplier)}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                    GlasmioSlider(
                        value = subtitleOptionsState.speedMultiplier,
                        valueRange = 0.5f..2.0f,
                        accentColor = mxBlue,
                        onValueChange = {
                            val rounded = (it * 100f).roundToLong() / 100f
                            subtitleOptionsState.setSpeed(rounded)
                        },
                    )
                }
            }
        }
    }
}
