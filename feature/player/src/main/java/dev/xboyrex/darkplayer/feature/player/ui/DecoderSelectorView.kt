package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

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
    val mxBlue = Color(0xFF38BDF8)
    val currentVideo = videoMode ?: DecoderMode.HARDWARE
    val swAudioEnabled = audioMode == DecoderMode.FFMPEG || audioMode == DecoderMode.SOFTWARE

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 48.dp, end = 24.dp),
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0D14).copy(alpha = 0.88f))
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Select decoder",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val decoderItems = listOf(
                Triple(DecoderMode.HARDWARE, "HW decoder", "Hardware MediaCodec decoder"),
                Triple(DecoderMode.SOFTWARE, "HW+ decoder", "Hybrid Hardware + System decoder"),
                Triple(DecoderMode.FFMPEG, "SW decoder", "Bundled FFmpeg software decoder"),
            )

            decoderItems.forEach { (mode, title, _) ->
                val isSelected = currentVideo == mode ||
                    (mode == DecoderMode.HARDWARE && currentVideo == DecoderMode.AUTO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                onVideoModeSelected(mode)
                                onDismiss()
                            },
                        )
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = mxBlue,
                            unselectedColor = Color.White.copy(alpha = 0.48f),
                        ),
                    )
                    Text(
                        text = title,
                        fontSize = 15.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.White,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(0.6.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAudioModeSelected(
                            if (swAudioEnabled) DecoderMode.AUTO else DecoderMode.FFMPEG,
                        )
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Checkbox(
                    checked = swAudioEnabled,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = mxBlue,
                        uncheckedColor = Color.White.copy(alpha = 0.48f),
                        checkmarkColor = Color.Black,
                    ),
                )
                Text(
                    text = "Use SW audio decoder",
                    fontSize = 14.5.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}
