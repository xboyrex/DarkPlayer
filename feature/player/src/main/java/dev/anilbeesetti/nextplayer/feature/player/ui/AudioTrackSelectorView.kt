package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.common.extensions.getFilenameFromUri
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.feature.player.extensions.externalAudio
import dev.anilbeesetti.nextplayer.feature.player.extensions.externalAudioIndex
import dev.anilbeesetti.nextplayer.feature.player.extensions.getName
import dev.anilbeesetti.nextplayer.feature.player.state.rememberTracksState
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MX Player Pro "Audio Track" Right-Side Panel
 * Exact implementation of Screenshot_20261110-155643_MX Player Pro.png (#21 in mx_screenshots):
 * - Radio list of audio tracks + "Disable"
 * - "Use SW audio decoder" checkbox
 * - "Open" (external audio file)
 * - "Stereo mode" (Stereo / Mono / Reverse)
 * - "Synchronization" (Audio delay +/- ms)
 */
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
    val accent = LocalNovaAccentColor.current
    var audioNames by remember { mutableStateOf(value = emptyList<String>()) }
    var stereoModeIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSyncControl by rememberSaveable { mutableStateOf(false) }
    var audioDelayMs by rememberSaveable { mutableLongStateOf(0L) }

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
        widthFraction = 0.42f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 22.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            audioTracksState.tracks.forEachIndexed { index, track ->
                val trackLabel = track.mediaTrackGroup.externalAudioIndex
                    ?.let { audioNames.getOrNull(index = it) }
                    ?: track.mediaTrackGroup.getName(
                        C.TRACK_TYPE_AUDIO,
                        index = index,
                    )
                MxAudioRadioRow(
                    selected = track.isSelected,
                    text = trackLabel,
                    accent = accent,
                    onClick = {
                        audioTracksState.switchTrack(index = index)
                        onDismiss()
                    },
                )
            }
            MxAudioRadioRow(
                selected = audioTracksState.tracks.none { it.isSelected },
                text = stringResource(R.string.disable),
                accent = accent,
                onClick = {
                    audioTracksState.switchTrack(index = -1)
                    onDismiss()
                },
            )

            // "Use SW audio decoder" checkbox matching MX #21 (155643)
            val useSwAudio = audioDecoderMode == DecoderMode.FFMPEG_SOFTWARE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onAudioDecoderModeSelected(
                            if (useSwAudio) DecoderMode.AUTO else DecoderMode.FFMPEG_SOFTWARE,
                        )
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = useSwAudio,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = accent,
                        uncheckedColor = Color.White.copy(alpha = 0.6f),
                    ),
                )
                Text(
                    text = "Use SW audio decoder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            Spacer(modifier = Modifier.height(6.dp))

            // "Open" row
            Text(
                text = "Open",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onSelectAudioClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
            )

            // "Stereo mode" row
            val stereoLabels = listOf("Stereo", "Mono", "Reverse Stereo")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { stereoModeIndex = (stereoModeIndex + 1) % stereoLabels.size }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stereo mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = stereoLabels[stereoModeIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                )
            }

            // "Synchronization" row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSyncControl = !showSyncControl }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Synchronization",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = "${if (audioDelayMs >= 0) "+" else ""}${audioDelayMs / 1000.0} s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                )
            }

            if (showSyncControl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "- 0.05s",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { audioDelayMs -= 50L }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Text(
                        text = "Reset",
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { audioDelayMs = 0L }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Text(
                        text = "+ 0.05s",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { audioDelayMs += 50L }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MxAudioRadioRow(
    selected: Boolean,
    text: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = Color.White.copy(alpha = 0.65f),
            ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}
