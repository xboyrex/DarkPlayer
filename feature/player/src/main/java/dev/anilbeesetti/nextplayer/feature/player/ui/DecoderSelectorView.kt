package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

/**
 * Exact MX Player Pro "Select decoder" Dialog
 * Fixes Bug #12 (`153821_Nova Player.png`) and implements Screenshot_20261110-155649_MX Player Pro.png (#23):
 * Clean centered dark translucent dialog with radio buttons:
 * - HW decoder
 * - HW+ decoder
 * - SW decoder
 */
@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val accent = LocalNovaAccentColor.current
    val currentVideoMode = videoMode ?: DecoderMode.HARDWARE

    AnimatedVisibility(
        modifier = modifier.align(Alignment.Center),
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF10131A).copy(alpha = 0.92f),
            contentColor = Color.White,
            modifier = Modifier
                .widthIn(min = 290.dp, max = 360.dp)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Select decoder",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val mxOptions = listOf(
                    "HW decoder" to DecoderMode.HARDWARE,
                    "HW+ decoder" to DecoderMode.SYSTEM_SOFTWARE,
                    "SW decoder" to DecoderMode.FFMPEG_SOFTWARE,
                )

                mxOptions.forEach { (title, mode) ->
                    val isSelected = currentVideoMode == mode ||
                        (currentVideoMode == DecoderMode.AUTO && mode == DecoderMode.HARDWARE)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onVideoModeSelected(mode)
                                    onDismiss()
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accent,
                                unselectedColor = Color.White.copy(alpha = 0.7f),
                            ),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = Color.White.copy(alpha = 0.10f),
                )

                val useSwAudio = audioMode == DecoderMode.FFMPEG_SOFTWARE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = useSwAudio,
                            onClick = {
                                onAudioModeSelected(
                                    if (useSwAudio) DecoderMode.AUTO else DecoderMode.FFMPEG_SOFTWARE,
                                )
                            },
                            role = Role.Switch,
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Use SW audio decoder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    RadioButton(
                        selected = useSwAudio,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = accent,
                            unselectedColor = Color.White.copy(alpha = 0.6f),
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
