package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls
import dev.xboyrex.darkplayer.feature.player.model.DecoderTrackType
import dev.xboyrex.darkplayer.feature.player.model.selectableDecoderModes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
) {
    var selectedTrackType by rememberSaveable { mutableStateOf(DecoderTrackType.VIDEO) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Select decoder",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode ?: DecoderMode.AUTO
                    DecoderTrackType.AUDIO -> audioMode ?: DecoderMode.AUTO
                },
                onModeSelected = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> onVideoModeSelected
                    DecoderTrackType.AUDIO -> onAudioModeSelected
                },
            )
        }
    }
}

@Composable
private fun DecoderTrackTypeSelector(
    selectedTrackType: DecoderTrackType,
    onTrackTypeSelected: (DecoderTrackType) -> Unit,
) {
    val trackTypes = DecoderTrackType.entries
    val useSolidMode = LocalUseMaterialYouControls.current
    val containerShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(Color.White.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        trackTypes.forEach { trackType ->
            val isSelected = selectedTrackType == trackType
            val pillShape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(pillShape)
                    .background(
                        when {
                            isSelected && useSolidMode -> Color.White
                            isSelected -> Color.White.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        },
                    )
                    .clickable { onTrackTypeSelected(trackType) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        id = when (trackType) {
                            DecoderTrackType.VIDEO -> R.string.video
                            DecoderTrackType.AUDIO -> R.string.audio
                        },
                    ),
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected && useSolidMode -> Color(0xFF0A0E17)
                        isSelected -> Color.White
                        else -> Color.White.copy(alpha = 0.65f)
                    },
                )
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(selectedMode: DecoderMode?, onModeSelected: (DecoderMode) -> Unit) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        selectableDecoderModes.forEach { mode ->
            DecoderModeRow(
                mode = mode,
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun DecoderModeRow(mode: DecoderMode, selected: Boolean, onClick: () -> Unit) {
    val useSolidMode = LocalUseMaterialYouControls.current
    val rowShape = RoundedCornerShape(12.dp)
    val activeColor = if (useSolidMode) Color.White else Color(0xFFFACC15)
    val decoderTitle = when (mode) {
        DecoderMode.AUTO -> "HW+ decoder (Auto)"
        DecoderMode.HARDWARE -> "HW decoder"
        DecoderMode.SOFTWARE -> "SW+ decoder (System)"
        DecoderMode.FFMPEG -> "SW decoder (FFmpeg)"
    }
    val decoderSubtitle = when (mode) {
        DecoderMode.AUTO -> "Hybrid hardware + FFmpeg fallback (Recommended)"
        DecoderMode.HARDWARE -> "Hardware MediaCodec decoders only"
        DecoderMode.SOFTWARE -> "Android system software MediaCodec decoders"
        DecoderMode.FFMPEG -> "Bundled FFmpeg software decoder (10-bit / HEVC / DTS)"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = rowShape)
            .background(
                if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.scale(0.88f),
            colors = RadioButtonDefaults.colors(
                selectedColor = activeColor,
                unselectedColor = Color.White.copy(alpha = 0.48f),
            ),
        )
        Column {
            Text(
                text = decoderTitle,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
            )
            Text(
                text = decoderSubtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}
