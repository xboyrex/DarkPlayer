package dev.xboyrex.darkplayer.feature.player.ui

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.model.MxQuickShortcut
import dev.xboyrex.darkplayer.core.model.PlayerControlStyle
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.VolumeBrightnessBarStyle
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaSegmentedControl
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaFluidSlider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * MX Player Pro 3-Dots More Menu Panel & Shortcuts Checklist
 * Exact implementation of Screenshot_20261110-155719, 155720, 155721_MX Player Pro.png (#24, #25, #26).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxScope.MxPlayerMorePanel(
    modifier: Modifier = Modifier,
    show: Boolean,
    playerPreferences: PlayerPreferences,
    onUpdatePreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
    onAspectRatioClick: () -> Unit,
    onLongPressSpeedClick: () -> Unit,
    onDisplaySettingsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onInformationClick: () -> Unit,
    onShareClick: () -> Unit,
    onCutAbRepeatClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onEqualizerClick: () -> Unit,
) {
    val accent = LocalNovaAccentColor.current
    val context = LocalContext.current
    val longPressSpeedText = remember(playerPreferences.longPressControlsSpeed) {
        "${"%.1f".format(playerPreferences.longPressControlsSpeed)}X"
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "",
        widthFraction = 0.45f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Row 1: Aspect Ratio | Speed FF (Long Press) [2.0X] | Display Settings | Playlists
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MxMoreGridButton(
                    label = "Aspect Ratio",
                    icon = Icons.Rounded.AspectRatio,
                    onClick = onAspectRatioClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Speed FF\n(Long Press)",
                    badgeText = longPressSpeedText,
                    showRedDot = true,
                    onClick = onLongPressSpeedClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Display\nSettings",
                    icon = Icons.Rounded.Tune,
                    onClick = onDisplaySettingsClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Playlists",
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    onClick = onPlaylistsClick,
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 2: Network Stream | Information | Share | Cut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MxMoreGridButton(
                    label = "Network\nStream",
                    icon = Icons.Rounded.Language,
                    onClick = {
                        Toast.makeText(context, "Network Stream active", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Information",
                    icon = Icons.Rounded.Info,
                    onClick = onInformationClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Share",
                    icon = Icons.Rounded.Share,
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Cut",
                    icon = Icons.Rounded.ContentCut,
                    onClick = onCutAbRepeatClick,
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 3: Bookmark | Chapter | Tutorial | More (Equalizer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MxMoreGridButton(
                    label = "Bookmark",
                    icon = Icons.Rounded.BookmarkBorder,
                    onClick = onBookmarkClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Chapter",
                    icon = Icons.Rounded.FormatListBulleted,
                    onClick = onChaptersClick,
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "Tutorial",
                    icon = Icons.Rounded.Lightbulb,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Swipe Left: Brightness • Swipe Right: Volume • Pinch: Zoom • Long Press: ${longPressSpeedText} Speed",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                )
                MxMoreGridButton(
                    label = "More",
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = onEqualizerClick,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            // Video Display Toggle Row matching MX #25 & #26 (155720, 155721)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Video Display",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                )
                NextSwitch(
                    checked = playerPreferences.showVideoDisplayInMenu,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showVideoDisplayInMenu = checked) }
                    },
                )
            }

            // Shortcuts Toggle Row matching MX #24, #25, #26 (155719, 155720, 155721)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Shortcuts",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                )
                NextSwitch(
                    checked = playerPreferences.showQuickShortcuts,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showQuickShortcuts = checked) }
                    },
                )
            }

            // Inline 2-Column Shortcuts Checklist matching MX #24 (155719_MX Player Pro.png)
            if (playerPreferences.showQuickShortcuts) {
                val pairs = listOf(
                    MxQuickShortcut.SCREEN_ROTATION to MxQuickShortcut.PLAYBACK_SPEED,
                    MxQuickShortcut.BACKGROUND_PLAY to MxQuickShortcut.LOOP,
                    MxQuickShortcut.MUTE to MxQuickShortcut.SHUFFLE,
                    MxQuickShortcut.EQUALIZER to MxQuickShortcut.SLEEP_TIMER,
                    MxQuickShortcut.AB_REPEAT to MxQuickShortcut.NIGHT_MODE,
                    MxQuickShortcut.CUSTOMISE_ITEMS to MxQuickShortcut.SCREENSHOT,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pairs.forEach { (leftItem, rightItem) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MxShortcutCheckboxCell(
                                shortcut = leftItem,
                                checked = leftItem.name in playerPreferences.enabledShortcuts,
                                accent = accent,
                                onToggle = {
                                    onUpdatePreferences { prefs ->
                                        val next = prefs.enabledShortcuts.toMutableList()
                                        if (leftItem.name in next) next.remove(leftItem.name) else next.add(leftItem.name)
                                        prefs.copy(enabledShortcuts = next)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            MxShortcutCheckboxCell(
                                shortcut = rightItem,
                                checked = rightItem.name in playerPreferences.enabledShortcuts,
                                accent = accent,
                                onToggle = {
                                    onUpdatePreferences { prefs ->
                                        val next = prefs.enabledShortcuts.toMutableList()
                                        if (rightItem.name in next) next.remove(rightItem.name) else next.add(rightItem.name)
                                        prefs.copy(enabledShortcuts = next)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MxMoreGridButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null,
    showRedDot: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (showRedDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D4F)),
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.White.copy(alpha = 0.90f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MxShortcutCheckboxCell(
    shortcut: MxQuickShortcut,
    checked: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = Color.White.copy(alpha = 0.6f),
            ),
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = shortcut.defaultTitle,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = Color.White,
            maxLines = 2,
        )
    }
}

/**
 * Dolby Atmos, 5-Band Studio Equalizer & Smooth Play/Pause Audio Fade Panel
 * Fixes Bug #3 (`153451`), Bug #9 (`153649`), and Bug #15 (`153857_Nova Player.png`).
 */
@Composable
fun BoxScope.MxEqualizerOverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    playerPreferences: PlayerPreferences,
    onUpdatePreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    val accent = LocalNovaAccentColor.current
    val presets = remember {
        mapOf(
            "Custom" to listOf(3, 0, 0, 0, -3),
            "Normal" to listOf(3, 0, 0, 0, 3),
            "Classical" to listOf(5, 3, -2, 4, 4),
            "Dance" to listOf(6, 0, 2, 4, 1),
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Rock" to listOf(5, 3, -1, 3, 5),
            "Cinema" to listOf(4, 2, 3, 2, 4),
        )
    }
    val freqLabels = remember { listOf("60 Hz", "230 Hz", "910 Hz", "3600 Hz", "14000 Hz") }
    val reverbOptions = remember { listOf("None", "Small Room", "Medium Room", "Large Hall", "Plate") }
    val bands = playerPreferences.equalizerBandGainsDb.let {
        if (it.size == 5) it else listOf(3, 0, 0, 0, -3)
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Dolby Atmos & Equalizer",
        widthFraction = 0.46f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Enable Equalizer & Dolby Atmos Master Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dolby Atmos & Spatial Audio",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = "3D surround stage & vocal dialogue clarity",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                }
                NextSwitch(
                    checked = playerPreferences.enableDolbyAtmos,
                    onCheckedChange = { enabled ->
                        onUpdatePreferences { it.copy(enableDolbyAtmos = enabled) }
                    },
                )
            }

            // Preset Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.keys.forEach { presetName ->
                    val isSelected = playerPreferences.equalizerPreset == presetName
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accent else Color.White.copy(alpha = 0.14f),
                                shape = CircleShape,
                            )
                            .clickable {
                                val targetBands = presets[presetName] ?: bands
                                onUpdatePreferences {
                                    it.copy(
                                        equalizerPreset = presetName,
                                        equalizerBandGainsDb = targetBands,
                                        enableAudioEqualizer = true,
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = presetName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accent else Color.White,
                        )
                    }
                }
            }

            // 5-Band Studio Equalizer Sliders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bands.forEachIndexed { idx, gainDb ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "${if (gainDb > 0) "+" else ""}$gainDb dB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Slider(
                            value = gainDb.toFloat(),
                            onValueChange = { newVal ->
                                val updated = bands.toMutableList().apply {
                                    this[idx] = newVal.roundToInt().coerceIn(-15, 15)
                                }
                                onUpdatePreferences {
                                    it.copy(
                                        equalizerPreset = "Custom",
                                        equalizerBandGainsDb = updated,
                                        enableAudioEqualizer = true,
                                    )
                                }
                            },
                            valueRange = -15f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                            ),
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = 270f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        Constraints(
                                            minWidth = constraints.minHeight,
                                            maxWidth = constraints.maxHeight,
                                            minHeight = constraints.minWidth,
                                            maxHeight = constraints.maxHeight,
                                        ),
                                    )
                                    layout(placeable.height, placeable.width) {
                                        placeable.place(
                                            -(placeable.width - placeable.height) / 2,
                                            (placeable.width - placeable.height) / 2,
                                        )
                                    }
                                }
                                .width(130.dp)
                                .height(36.dp),
                        )
                        Text(
                            text = freqLabels[idx],
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }

            // Reverb Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable {
                        val nextIdx = (reverbOptions.indexOf(playerPreferences.reverbPreset) + 1) % reverbOptions.size
                        onUpdatePreferences { it.copy(reverbPreset = reverbOptions[nextIdx]) }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Reverb",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                )
                Text(
                    text = "${playerPreferences.reverbPreset} ▾",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                )
            }

            // Bass Boost & Virtualizer with GlasmioSlider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Bass Boost", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("${playerPreferences.bassBoostPercent}%", color = accent, fontWeight = FontWeight.Bold)
                }
                NovaFluidSlider(
                    value = playerPreferences.bassBoostPercent.toFloat(),
                    onValueChange = { v ->
                        onUpdatePreferences { it.copy(bassBoostPercent = v.roundToInt().coerceIn(0, 100)) }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Virtualizer", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("${playerPreferences.virtualizerPercent}%", color = accent, fontWeight = FontWeight.Bold)
                }
                NovaFluidSlider(
                    value = playerPreferences.virtualizerPercent.toFloat(),
                    onValueChange = { v ->
                        onUpdatePreferences { it.copy(virtualizerPercent = v.roundToInt().coerceIn(0, 100)) }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Smooth Play/Pause Audio Fade Card (Fixes Bug #3, #9, #15 with genuine GlasmioSwitch!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = !playerPreferences.smoothPlayPauseAudioFade
                            onUpdatePreferences {
                                it.copy(
                                    smoothPlayPauseAudioFade = next,
                                    fadeOnStart = next,
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Smooth Play/Pause Audio Fade",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                        Text(
                            text = "Gently fades audio in and out when pausing or resuming",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                    NextSwitch(
                        checked = playerPreferences.smoothPlayPauseAudioFade,
                        onCheckedChange = { checked ->
                            onUpdatePreferences {
                                it.copy(
                                    smoothPlayPauseAudioFade = checked,
                                    fadeOnStart = checked,
                                )
                            }
                        },
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUpdatePreferences { it.copy(fadeOnSeek = !it.fadeOnSeek) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Smooth Fade on Seek",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                        Text(
                            text = "Prevents audio clicks while scrubbing timeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                    NextSwitch(
                        checked = playerPreferences.fadeOnSeek,
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(fadeOnSeek = checked) }
                        },
                    )
                }
            }
        }
    }
}

/**
 * MX Player Pro Display & UI Settings Panel (Implements MX #7 `155442_MX Player Pro.png` + Live Bar/UI Style Selector)
 */
@Composable
fun BoxScope.MxDisplaySettingsOverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    playerPreferences: PlayerPreferences,
    onUpdatePreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    val accent = LocalNovaAccentColor.current

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Display & UI Settings",
        widthFraction = 0.45f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Volume & Brightness Bar Design",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
            NovaSegmentedControl(
                items = listOf(
                    VolumeBrightnessBarStyle.MX_PRO_VERTICAL to "Nova Pro",
                    VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE to "Fluid Capsule",
                    VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL to "Minimal",
                ),
                selected = playerPreferences.volumeBrightnessBarStyle,
                onSelected = { style ->
                    onUpdatePreferences { it.copy(volumeBrightnessBarStyle = style) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Player Controls Design",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
            NovaSegmentedControl(
                items = listOf(
                    PlayerControlStyle.MX_PRO to "MX Pro",
                    PlayerControlStyle.LIQUID_GLASS to "Liquid Glass",
                    PlayerControlStyle.CLASSIC_NEXT to "Classic",
                ),
                selected = playerPreferences.playerControlStyle,
                onSelected = { style ->
                    onUpdatePreferences { it.copy(playerControlStyle = style) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            MxDisplayToggleRow(
                title = "Screen rotation button",
                checked = playerPreferences.showScreenRotationButton,
                onCheckedChange = { v -> onUpdatePreferences { it.copy(showScreenRotationButton = v) } },
            )
            MxDisplayToggleRow(
                title = "Display battery/clock in title bar",
                checked = playerPreferences.showBatteryClockInTitleBar,
                onCheckedChange = { v -> onUpdatePreferences { it.copy(showBatteryClockInTitleBar = v) } },
            )
            MxDisplayToggleRow(
                title = "Show Remaining Time",
                checked = playerPreferences.showRemainingTime,
                onCheckedChange = { v -> onUpdatePreferences { it.copy(showRemainingTime = v) } },
            )
            MxDisplayToggleRow(
                title = "Forward / Backward 10s buttons",
                checked = playerPreferences.showForwardBackwardButtons,
                onCheckedChange = { v -> onUpdatePreferences { it.copy(showForwardBackwardButtons = v) } },
            )
            MxDisplayToggleRow(
                title = "Swap Volume & Brightness Sides",
                checked = playerPreferences.swapVolumeBrightnessSides,
                onCheckedChange = { v -> onUpdatePreferences { it.copy(swapVolumeBrightnessSides = v) } },
            )
        }
    }
}

@Composable
private fun MxDisplayToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        NextSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * MX Player Pro Sleep Timer Panel
 */
@Composable
fun BoxScope.MxSleepTimerOverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalNovaAccentColor.current
    val options = listOf(
        0 to "Off",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes",
        90 to "90 minutes",
    )

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Sleep Timer",
        widthFraction = 0.36f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (mins, label) ->
                val selected = selectedMinutes == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable {
                            onSelectMinutes(mins)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (selected) accent else Color.White,
                    )
                    if (selected && mins > 0) {
                        Text("Active", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * MX Player Pro Media Information Panel
 */
@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MxMediaInformationOverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val mediaItem = player.currentMediaItem
    val videoFormat = player.videoFormat
    val audioFormat = player.audioFormat

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Properties & Information",
        widthFraction = 0.42f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MxInfoItem("Title", mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown")
            MxInfoItem("URI / Path", mediaItem?.mediaId ?: "Unknown")
            if (videoFormat != null) {
                MxInfoItem("Resolution", "${videoFormat.width} × ${videoFormat.height}")
                MxInfoItem("Video Codec", videoFormat.sampleMimeType ?: "Hardware")
                if (videoFormat.frameRate > 0) {
                    MxInfoItem("Frame Rate", "${"%.2f".format(videoFormat.frameRate)} fps")
                }
            }
            if (audioFormat != null) {
                MxInfoItem("Audio Codec", audioFormat.sampleMimeType ?: "Unknown")
                MxInfoItem("Channels / Sample Rate", "${audioFormat.channelCount} ch • ${audioFormat.sampleRate} Hz")
            }
        }
    }
}

@Composable
private fun MxInfoItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

/**
 * Captures the current video frame and saves a PNG screenshot to Pictures/NovaPlayer.
 */
@OptIn(UnstableApi::class)
fun captureVideoScreenshot(context: android.content.Context, player: Player) {
    when (
        val result = dev.xboyrex.darkplayer.feature.player.utils.VideoFrameCapture.captureAndSaveCurrentFrame(
            context = context,
            player = player,
        )
    ) {
        is dev.xboyrex.darkplayer.feature.player.utils.FrameSaveResult.Success -> {
            Toast.makeText(
                context,
                "Screenshot saved: ${result.fileName}",
                Toast.LENGTH_SHORT,
            ).show()
        }
        is dev.xboyrex.darkplayer.feature.player.utils.FrameSaveResult.Failure -> {
            Toast.makeText(
                context,
                "Screenshot failed: ${result.reason}",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

fun shareCurrentVideo(context: android.content.Context, player: Player) {
    val uriString = player.currentMediaItem?.mediaId ?: return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uriString))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
    }
}
