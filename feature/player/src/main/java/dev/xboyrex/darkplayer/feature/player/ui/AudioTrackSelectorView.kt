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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    audioDecoderMode: DecoderMode? = null,
    onAudioDecoderModeSelected: (DecoderMode) -> Unit = {},
    onSelectAudioClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val audioTracksState = rememberTracksState(
        player = player,
        C.TRACK_TYPE_AUDIO,
    )
    val context = LocalContext.current
    var audioNames by remember { mutableStateOf(value = emptyList<String>()) }
    var stereoModeIndex by remember { mutableIntStateOf(0) }
    val stereoModes = listOf("Stereo", "Mono", "Left", "Right", "Dolby Surround")
    var showSyncControl by remember { mutableStateOf(false) }
    var syncDelayMs by remember { mutableIntStateOf(0) }
    val mxBlue = Color(0xFF38BDF8)
    val swAudioChecked = audioDecoderMode == DecoderMode.FFMPEG || audioDecoderMode == DecoderMode.SOFTWARE

    LaunchedEffect(
        key1 = player,
        audioTracksState.tracks,
    ) {
        val uris = player.currentMediaItem?.mediaMetadata?.externalAudio.orEmpty()
        audioNames =
            withContext(Dispatchers.IO) { uris.map { context.getFilenameFromUri(uri = it) } }
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Audio Track",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp)
                .selectableGroup(),
        ) {
            audioTracksState.tracks.forEachIndexed { index, track ->
                RadioButtonRow(
                    selected = track.isSelected,
                    text = track.mediaTrackGroup.externalAudioIndex
                        ?.let { audioNames.getOrNull(it) }
                        ?: track.mediaTrackGroup.getName(
                            trackType = C.TRACK_TYPE_AUDIO,
                            index = index,
                        ),
                    onClick = {
                        audioTracksState.switchTrack(index)
                        onDismiss()
                    },
                )
            }
            RadioButtonRow(
                selected = audioTracksState.tracks.none { it.isSelected },
                text = stringResource(R.string.disable),
                onClick = {
                    audioTracksState.switchTrack(-1)
                    onDismiss()
                },
            )

            // Use SW audio decoder checkbox (matching 155643_MX Player Pro.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAudioDecoderModeSelected(
                            if (swAudioChecked) DecoderMode.AUTO else DecoderMode.FFMPEG,
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Checkbox(
                    checked = swAudioChecked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = mxBlue,
                        uncheckedColor = Color.White.copy(alpha = 0.45f),
                        checkmarkColor = Color.Black,
                    ),
                )
                Text(
                    text = "Use SW audio decoder",
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Open external audio track row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectAudioClick()
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

            // Stereo mode row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        stereoModeIndex = (stereoModeIndex + 1) % stereoModes.size
                        val mode = stereoModes[stereoModeIndex]
                        if (mode == "Dolby Surround") {
                            HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(true)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Stereo mode",
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Text(
                    text = stereoModes[stereoModeIndex],
                    fontSize = 13.5.sp,
                    color = mxBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Synchronization row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSyncControl = !showSyncControl }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Synchronization",
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Text(
                    text = "${if (syncDelayMs >= 0) "+" else ""}${syncDelayMs / 1000f}s",
                    fontSize = 13.5.sp,
                    color = if (syncDelayMs != 0) mxBlue else Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (showSyncControl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { syncDelayMs -= 50 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_remove),
                            contentDescription = "-50ms",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "${syncDelayMs} ms",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { syncDelayMs += 50 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "+50ms",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
