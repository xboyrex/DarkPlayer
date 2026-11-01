package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.common.extensions.getFilenameFromUri
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.audio.HardwareDolbyAndEqualizerEngine
import dev.xboyrex.darkplayer.core.ui.audio.StereoModeType
import dev.xboyrex.darkplayer.feature.player.extensions.externalAudio
import dev.xboyrex.darkplayer.feature.player.extensions.externalAudioIndex
import dev.xboyrex.darkplayer.feature.player.extensions.getName
import dev.xboyrex.darkplayer.feature.player.state.rememberTracksState
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.AudioTrackSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    audioMode: DecoderMode? = null,
    onAudioModeSelected: (DecoderMode) -> Unit = {},
    onSelectAudioClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val audioTracksState = rememberTracksState(
        player = player,
        C.TRACK_TYPE_AUDIO,
    )
    val context = LocalContext.current
    var audioNames by remember { mutableStateOf(value = emptyList<String>()) }
    var stereoMenuExpanded by remember { mutableStateOf(false) }
    var showSyncControls by remember { mutableStateOf(false) }

    LaunchedEffect(
        key1 = player,
        audioTracksState.tracks,
    ) {
        val uris = player.currentMediaItem?.mediaMetadata?.externalAudio.orEmpty()
        audioNames =
            withContext(Dispatchers.IO) { uris.map { context.getFilenameFromUri(uri = it) } }
    }

    val isSwAudioDecoder = audioMode == DecoderMode.PREFER_SOFTWARE || audioMode == DecoderMode.SOFTWARE_ONLY
    val accentBlue = Color(0xFF38BDF8)

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Audio Track",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Track list inside iOS rounded card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                audioTracksState.tracks.forEachIndexed { index, track ->
                    RadioButtonRow(
                        selected = track.isSelected,
                        text = track.mediaTrackGroup.externalAudioIndex
                            ?.let { audioNames.getOrNull(index = it) }
                            ?: track.mediaTrackGroup.getName(
                                C.TRACK_TYPE_AUDIO,
                                index = index,
                            ),
                        onClick = {
                            audioTracksState.switchTrack(index = index)
                            onDismiss()
                        },
                    )
                }
                RadioButtonRow(
                    selected = audioTracksState.tracks.none { it.isSelected },
                    text = stringResource(R.string.disable),
                    onClick = {
                        audioTracksState.switchTrack(index = -1)
                        onDismiss()
                    },
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Use SW audio decoder checkbox row (matching 24074_c798dc.png)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val nextMode = if (isSwAudioDecoder) {
                                DecoderMode.PREFER_DEVICE
                            } else {
                                DecoderMode.PREFER_SOFTWARE
                            }
                            onAudioModeSelected(nextMode)
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(
                        checked = isSwAudioDecoder,
                        onCheckedChange = { checked ->
                            onAudioModeSelected(
                                if (checked) DecoderMode.PREFER_SOFTWARE else DecoderMode.PREFER_DEVICE,
                            )
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentBlue,
                            uncheckedColor = Color.White.copy(alpha = 0.55f),
                            checkmarkColor = Color(0xFF090D16),
                        ),
                    )
                    Text(
                        text = "Use SW audio decoder",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }

            // 2. Open, Stereo mode, and Synchronization iOS rows (matching 24074_c798dc.png)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
            ) {
                // Open external audio file
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectAudioClick()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Open",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(19.dp),
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Stereo mode selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { stereoMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Stereo mode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = HardwareDolbyAndEqualizerEngine.stereoMode.title,
                            fontSize = 12.sp,
                            color = accentBlue,
                        )
                    }
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.72f),
                        )
                        DropdownMenu(
                            expanded = stereoMenuExpanded,
                            onDismissRequest = { stereoMenuExpanded = false },
                        ) {
                            StereoModeType.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.title) },
                                    onClick = {
                                        HardwareDolbyAndEqualizerEngine.setStereoModeType(mode)
                                        stereoMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Synchronization (Audio Delay / Sync)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSyncControls = !showSyncControls }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Synchronization",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = "${HardwareDolbyAndEqualizerEngine.audioDelayMs} ms",
                                fontSize = 12.sp,
                                color = if (HardwareDolbyAndEqualizerEngine.audioDelayMs != 0) accentBlue else Color.White.copy(alpha = 0.65f),
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    if (showSyncControls) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    HardwareDolbyAndEqualizerEngine.setAudioSyncDelayMs(
                                        HardwareDolbyAndEqualizerEngine.audioDelayMs - 50,
                                    )
                                },
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-50 ms")
                            }
                            Text(
                                text = "${HardwareDolbyAndEqualizerEngine.audioDelayMs} ms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = {
                                        HardwareDolbyAndEqualizerEngine.setAudioSyncDelayMs(0)
                                    },
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset")
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        HardwareDolbyAndEqualizerEngine.setAudioSyncDelayMs(
                                            HardwareDolbyAndEqualizerEngine.audioDelayMs + 50,
                                        )
                                    },
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "+50 ms")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
