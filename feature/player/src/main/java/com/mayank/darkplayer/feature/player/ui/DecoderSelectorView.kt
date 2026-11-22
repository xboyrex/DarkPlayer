package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.model.DecoderTrackType
import com.mayank.darkplayer.feature.player.model.descriptionRes
import com.mayank.darkplayer.feature.player.model.labelRes
import com.mayank.darkplayer.feature.player.model.selectableDecoderModes

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
        title = stringResource(R.string.select_decoders),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode
                    DecoderTrackType.AUDIO -> audioMode
                },
                trackType = selectedTrackType,
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
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x16F4EFE8))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DecoderTrackType.entries.forEach { trackType ->
            val selected = selectedTrackType == trackType
            val bg by animateColorAsState(if (selected) accent else Color.Transparent, tween(140), label = "dec_seg_bg")
            val fg by animateColorAsState(if (selected) onAccent else Color(0xFFD6DDD7), tween(140), label = "dec_seg_fg")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(selected = selected, onClick = { onTrackTypeSelected(trackType) }, role = Role.Tab)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = when (trackType) {
                        DecoderTrackType.VIDEO -> Icons.Rounded.Videocam
                        DecoderTrackType.AUDIO -> Icons.Rounded.Audiotrack
                    },
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        id = when (trackType) {
                            DecoderTrackType.VIDEO -> R.string.video
                            DecoderTrackType.AUDIO -> R.string.audio
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(
    selectedMode: DecoderMode?,
    trackType: DecoderTrackType,
    onModeSelected: (DecoderMode) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        selectableDecoderModes.filter {
            trackType == DecoderTrackType.VIDEO ||
                it != DecoderMode.HW_PLUS
        }.forEach { mode ->
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
    val accent = MaterialTheme.colorScheme.primary
    val icon = when (mode) {
        DecoderMode.AUTO -> Icons.Rounded.Tune
        DecoderMode.HARDWARE -> Icons.Rounded.Memory
        DecoderMode.HW_PLUS -> Icons.Rounded.Memory
        DecoderMode.FFMPEG -> Icons.Rounded.DeveloperBoard
    }
    val rowBg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.20f) else Color(0x14F4EFE8),
        animationSpec = tween(140),
        label = "dec_row_bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f)
            .clip(shape = RoundedCornerShape(18.dp))
            .background(rowBg)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) accent.copy(alpha = 0.25f) else Color(0x1AF4EFE8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else Color(0xFFD0D6D1),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) accent else Color(0xFFF4EFE8),
            )
            Text(
                text = stringResource(mode.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAB4AC),
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = Color(0xFF7D877F),
            ),
        )
    }
}
