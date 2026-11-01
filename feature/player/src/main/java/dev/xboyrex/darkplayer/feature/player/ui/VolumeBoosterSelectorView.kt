package dev.xboyrex.darkplayer.feature.player.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.feature.player.service.AudioBoostProfile
import dev.xboyrex.darkplayer.feature.player.service.DolbyVolumeBoosterEngine
import dev.xboyrex.darkplayer.feature.player.state.VolumeState

@Composable
fun BoxScope.VolumeBoosterSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    volumeState: VolumeState?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentPct = volumeState?.volumePercentage ?: DolbyVolumeBoosterEngine.boostPercentage
    val presets = listOf(100, 200, 400, 600, 800, 1000)

    OverlayView(
        modifier = modifier,
        show = show,
        title = "1000% Dolby Audio Booster",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Live Boost Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = if (currentPct > 100) Color(0xFFFACC15).copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Master Output Gain",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.70f),
                    )
                    Text(
                        text = if (DolbyVolumeBoosterEngine.dolbyEnabled) {
                            "${DolbyVolumeBoosterEngine.activeProfile.label} · Anti-Clip Active"
                        } else {
                            "Standard Output"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFACC15),
                    )
                }
                Text(
                    text = "${currentPct}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentPct > 100) Color(0xFFFACC15) else Color.White,
                )
            }

            // 0% - 1000% Master Slider
            Slider(
                value = currentPct.toFloat(),
                onValueChange = { newVal ->
                    val target = newVal.toInt().coerceIn(0, 1000)
                    volumeState?.updateVolumePercentage(target)
                    DolbyVolumeBoosterEngine.setVolumePercentage(target)
                },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFACC15),
                    activeTrackColor = Color(0xFFFACC15),
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                ),
            )

            // Quick Boost Preset Pills (100% .. 1000%)
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    val isSelected = kotlin.math.abs(currentPct - preset) < 25
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFFACC15)
                                else Color.White.copy(alpha = 0.07f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFACC15)
                                else Color.White.copy(alpha = 0.14f),
                                shape = CircleShape,
                            )
                            .clickable {
                                volumeState?.updateVolumePercentage(preset)
                                DolbyVolumeBoosterEngine.setVolumePercentage(preset)
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (preset == 1000) "1000% MAX" else "${preset}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF090D16) else Color.White,
                        )
                    }
                }
            }

            // Intelligent Sound Profiles
            Text(
                text = "INTELLIGENT DOLBY PROFILES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AudioBoostProfile.entries.forEach { profile ->
                    val isSelected = DolbyVolumeBoosterEngine.activeProfile == profile && DolbyVolumeBoosterEngine.dolbyEnabled
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.14f)
                                else Color.White.copy(alpha = 0.04f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFACC15).copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = shape,
                            )
                            .clickable {
                                DolbyVolumeBoosterEngine.selectProfile(profile)
                                volumeState?.updateVolumePercentage(DolbyVolumeBoosterEngine.boostPercentage)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFFFACC15) else Color.White,
                            )
                            Text(
                                text = profile.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
            }

            // Deep Sub-Bass Harmonics Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sub-Bass Harmonic Punch",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Text(
                        text = "${DolbyVolumeBoosterEngine.bassStrength}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFACC15),
                    )
                }
                Slider(
                    value = DolbyVolumeBoosterEngine.bassStrength.toFloat(),
                    onValueChange = { DolbyVolumeBoosterEngine.setBassBoostStrength(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFACC15),
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                    ),
                )
            }

            // 3D Surround & Brickwall Limiter Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { DolbyVolumeBoosterEngine.setSurround(!DolbyVolumeBoosterEngine.surroundEnabled) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D Spatial Surround Stage",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Widens stereo imaging for headphones & speakers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                NextSwitch(
                    checked = DolbyVolumeBoosterEngine.surroundEnabled,
                    onCheckedChange = { DolbyVolumeBoosterEngine.setSurround(it) },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        DolbyVolumeBoosterEngine.setIntelligentLimiter(!DolbyVolumeBoosterEngine.intelligentLimiterEnabled)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Intelligent Anti-Clip Brickwall Limiter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Prevents distortion at 400%–1000% extreme boost",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                NextSwitch(
                    checked = DolbyVolumeBoosterEngine.intelligentLimiterEnabled,
                    onCheckedChange = { DolbyVolumeBoosterEngine.setIntelligentLimiter(it) },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "Using built-in Dark Player 1000% Dolby DSP Engine", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text(text = "Open System Hardware Equalizer")
            }
        }
    }
}
