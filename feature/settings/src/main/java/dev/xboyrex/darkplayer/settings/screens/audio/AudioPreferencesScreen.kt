package dev.xboyrex.darkplayer.settings.screens.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.PreferenceSwitch
import dev.xboyrex.darkplayer.core.ui.components.RadioTextButton
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.settings.composables.OptionsDialog
import dev.xboyrex.darkplayer.settings.utils.LocalesHelper
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaFluidSlider
import kotlin.math.roundToInt

@Composable
fun AudioPreferencesScreen(viewModel: AudioPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AudioPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * MX Player Pro Audio Settings + Studio Equalizer & Smooth Play/Pause Audio Fade
 * Implements Screenshot_20261110-155545_MX Player Pro.png (#14) and fixes Bug #3 (`153451`) & Bug #9 (`153649`).
 * - Properly applies `innerPadding` so top bar never overlaps the first row (Bug #9)
 * - Uses genuine `GlasmioSwitch` (`NextSwitch`) for Smooth Play/Pause Audio Fade, Fade on Start, and Fade on Seek (Bug #3, #9, #15)
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AudioPreferencesScreenContent(
    state: AudioPreferencesUiState,
    onAction: (AudioPreferencesUiEvent) -> Unit,
) {
    val languages = remember {
        listOf(
            element = Pair(
                first = "None",
                second = "",
            ),
        ) +
            LocalesHelper.getAvailableLocales()
    }
    val accent = LocalNovaAccentColor.current
    val presets = remember {
        mapOf(
            "Custom" to listOf(3, 0, 0, 0, -3),
            "Normal" to listOf(3, 0, 0, 0, 3),
            "Classical" to listOf(5, 3, -2, 4, 4),
            "Dance" to listOf(6, 0, 2, 4, 1),
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Rock" to listOf(5, 3, -1, 3, 5),
        )
    }
    val freqLabels = remember { listOf("60 Hz", "230 Hz", "910 Hz", "3600 Hz", "14000 Hz") }
    val reverbOptions = remember { listOf("None", "Small Room", "Medium Room", "Large Hall", "Plate") }
    val bands = state.preferences.equalizerBandGainsDb.let {
        if (it.size == 5) it else listOf(3, 0, 0, 0, -3)
    }

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.audio),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AudioPreferencesUiEvent.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            ListSectionTitle(text = "Nova Studio Audio Options")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = "Audio player",
                    description = "Use Nova Player as an audio player.",
                    icon = NextIcons.Audio,
                    isChecked = state.preferences.useAsAudioPlayer,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                it.copy(useAsAudioPlayer = !it.useAsAudioPlayer)
                            },
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.volume_boost),
                    description = "Audio volume can be boosted up to 200% if you use HW+ or SW decoder.",
                    icon = NextIcons.VolumeUp,
                    isChecked = state.preferences.enableVolumeBoost,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleVolumeBoost) },
                )
                PreferenceSwitch(
                    title = "System volume",
                    description = "Synchronize sound volume with the system media volume.",
                    icon = NextIcons.VolumeUp,
                    isChecked = state.preferences.syncSystemVolume,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                it.copy(syncSystemVolume = !it.syncSystemVolume)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.system_volume_panel),
                    description = stringResource(R.string.system_volume_panel_desc),
                    icon = NextIcons.Headset,
                    isChecked = state.preferences.showSystemVolumePanel,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleShowSystemVolumePanel) },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.pause_on_headset_disconnect),
                    description = stringResource(R.string.pause_on_headset_disconnect_desc),
                    icon = NextIcons.HeadsetOff,
                    isChecked = state.preferences.pauseOnHeadsetDisconnect,
                    onClick = { onAction(AudioPreferencesUiEvent.TogglePauseOnHeadsetDisconnect) },
                )
                PreferenceSwitch(
                    title = "Fade on start (Smooth Play/Pause Fade)",
                    description = "Gently fades audio in and out when starting, pausing, or resuming.",
                    icon = NextIcons.Audio,
                    isChecked = state.preferences.fadeOnStart && state.preferences.smoothPlayPauseAudioFade,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                val next = !(it.fadeOnStart && it.smoothPlayPauseAudioFade)
                                it.copy(
                                    fadeOnStart = next,
                                    smoothPlayPauseAudioFade = next,
                                )
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Fade on seek",
                    description = "Smoothly ramp volume after scrubbing to prevent clicks.",
                    icon = NextIcons.Audio,
                    isChecked = state.preferences.fadeOnSeek,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                it.copy(fadeOnSeek = !it.fadeOnSeek)
                            },
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_audio_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(
                        state.preferences.preferredAudioLanguage,
                    )
                        .takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.preferred_audio_lang_description),
                    icon = NextIcons.Language,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.ShowDialog(
                                value = AudioPreferenceDialog.AudioLanguageDialog,
                            ),
                        )
                    },
                )
                PreferenceSwitch(
                    title = "Prefer audio passthrough mode",
                    description = "Whenever possible, pass the source's audio signal directly to an external receiver.",
                    icon = NextIcons.Decoder,
                    isChecked = state.preferences.preferAudioPassthrough,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                it.copy(preferAudioPassthrough = !it.preferAudioPassthrough)
                            },
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.require_audio_focus),
                    description = stringResource(R.string.require_audio_focus_desc),
                    icon = NextIcons.Focus,
                    isChecked = state.preferences.requireAudioFocus,
                    onClick = { onAction(AudioPreferencesUiEvent.ToggleRequireAudioFocus) },
                )
                PreferenceSwitch(
                    title = "Audio equalizer",
                    description = "Enable 5-Band Studio Equalizer, Bass Boost & Dolby Atmos.",
                    icon = NextIcons.Audio,
                    isChecked = state.preferences.enableAudioEqualizer,
                    onClick = {
                        onAction(
                            AudioPreferencesUiEvent.UpdatePreferences {
                                it.copy(enableAudioEqualizer = !it.enableAudioEqualizer)
                            },
                        )
                    },
                    isLastItem = true,
                )
            }

            // Equalizer & Dolby Atmos Studio Card (Fixes Bug #3 & Bug #9!)
            ListSectionTitle(text = "EQUALIZER & DOLBY ATMOS STUDIO")
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.keys.forEach { presetName ->
                            val isSelected = state.preferences.equalizerPreset == presetName
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) accent else Color.White.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        val targetBands = presets[presetName] ?: bands
                                        onAction(
                                            AudioPreferencesUiEvent.UpdatePreferences {
                                                it.copy(
                                                    equalizerPreset = presetName,
                                                    equalizerBandGainsDb = targetBands,
                                                    enableAudioEqualizer = true,
                                                )
                                            },
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = presetName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    // 5-Band Sliders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.22f))
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
                                    text = "$gainDb dB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Slider(
                                    value = gainDb.toFloat(),
                                    onValueChange = { newVal ->
                                        val updated = bands.toMutableList().apply {
                                            this[idx] = newVal.roundToInt().coerceIn(-15, 15)
                                        }
                                        onAction(
                                            AudioPreferencesUiEvent.UpdatePreferences {
                                                it.copy(
                                                    equalizerPreset = "Custom",
                                                    equalizerBandGainsDb = updated,
                                                    enableAudioEqualizer = true,
                                                )
                                            },
                                        )
                                    },
                                    valueRange = -15f..15f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accent,
                                        activeTrackColor = accent,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Reverb Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .clickable {
                                val nextIdx = (reverbOptions.indexOf(state.preferences.reverbPreset) + 1) % reverbOptions.size
                                onAction(
                                    AudioPreferencesUiEvent.UpdatePreferences {
                                        it.copy(reverbPreset = reverbOptions[nextIdx])
                                    },
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Reverb", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${state.preferences.reverbPreset} ▾",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                        )
                    }

                    // Bass Boost & Virtualizer with GlasmioSlider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Bass Boost", style = MaterialTheme.typography.bodyMedium)
                            Text("${state.preferences.bassBoostPercent}%", color = accent, fontWeight = FontWeight.Bold)
                        }
                        NovaFluidSlider(
                            value = state.preferences.bassBoostPercent.toFloat(),
                            onValueChange = { v ->
                                onAction(
                                    AudioPreferencesUiEvent.UpdatePreferences {
                                        it.copy(bassBoostPercent = v.roundToInt().coerceIn(0, 100))
                                    },
                                )
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Virtualizer", style = MaterialTheme.typography.bodyMedium)
                            Text("${state.preferences.virtualizerPercent}%", color = accent, fontWeight = FontWeight.Bold)
                        }
                        NovaFluidSlider(
                            value = state.preferences.virtualizerPercent.toFloat(),
                            onValueChange = { v ->
                                onAction(
                                    AudioPreferencesUiEvent.UpdatePreferences {
                                        it.copy(virtualizerPercent = v.roundToInt().coerceIn(0, 100))
                                    },
                                )
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Smooth Play/Pause Audio Fade Card with GlasmioSwitch (Fixes Bug #3 & Bug #9!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .clickable {
                                onAction(
                                    AudioPreferencesUiEvent.UpdatePreferences {
                                        val next = !it.smoothPlayPauseAudioFade
                                        it.copy(
                                            smoothPlayPauseAudioFade = next,
                                            fadeOnStart = next,
                                        )
                                    },
                                )
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Smooth Play/Pause Audio Fade",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "Gently fades audio in and out when pausing or resuming",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        NextSwitch(
                            checked = state.preferences.smoothPlayPauseAudioFade,
                            onCheckedChange = { checked ->
                                onAction(
                                    AudioPreferencesUiEvent.UpdatePreferences {
                                        it.copy(
                                            smoothPlayPauseAudioFade = checked,
                                            fadeOnStart = checked,
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                AudioPreferenceDialog.AudioLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_audio_lang),
                        onDismissClick = {
                            onAction(AudioPreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == state.preferences.preferredAudioLanguage,
                                onClick = {
                                    onAction(AudioPreferencesUiEvent.UpdateAudioLanguage(it.second))
                                    onAction(AudioPreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AudioPreferencesScreenPreview() {
    NextPlayerTheme {
        AudioPreferencesScreenContent(
            state = AudioPreferencesUiState(),
            onAction = {},
        )
    }
}
