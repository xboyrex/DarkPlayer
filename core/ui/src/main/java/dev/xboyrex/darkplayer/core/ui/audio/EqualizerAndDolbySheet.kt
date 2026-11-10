package dev.xboyrex.darkplayer.core.ui.audio

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun EqualizerAndDolbyStudioContent(
    modifier: Modifier = Modifier,
    initialTab: Int = 0, // 0 = Equalizer, 1 = Dolby Atmos
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        HardwareDolbyAndEqualizerEngine.init(context)
    }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val accentBlue = Color(0xFF38BDF8)
    val accentGold = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Apple iOS Segmented Tab Switcher: Equalizer | Dolby Atmos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                0 to "Equalizer",
                1 to "Dolby Atmos",
            ).forEach { (tabIdx, label) ->
                val isSelected = selectedTab == tabIdx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.18f)
                            else Color.Transparent,
                        )
                        .border(
                            width = 0.8.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.30f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedTab = tabIdx }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (tabIdx == 0) Icons.Rounded.Equalizer else Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (isSelected) accentGold else Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.68f),
                        )
                    }
                }
            }
        }

        // Force Audio Booster & Low-Sound Dialogue Normalization Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161922))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Low-Sound Fix & Dialogue Pre-Gain",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "Boosts quiet 5.1/7.1 movie dialogue & forces EQ/Dolby on all codecs",
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.5.sp,
                    )
                }
                Switch(
                    checked = HardwareDolbyAndEqualizerEngine.lowSoundNormalizeEnabled,
                    onCheckedChange = { HardwareDolbyAndEqualizerEngine.setLowSoundNormalize(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accentGold,
                    ),
                )
            }
            val boostPct = ((HardwareDolbyAndEqualizerEngine.volumeBoostGainMb / 4000f) * 300f).roundToInt() + 100
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Master Audio Booster",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${boostPct}%",
                    color = accentGold,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Slider(
                value = HardwareDolbyAndEqualizerEngine.volumeBoostGainMb.toFloat(),
                onValueChange = { HardwareDolbyAndEqualizerEngine.setVolumeBoostMb(it.roundToInt()) },
                valueRange = 0f..4000f,
                colors = SliderDefaults.colors(
                    thumbColor = accentGold,
                    activeTrackColor = accentGold,
                ),
            )
        }

        if (selectedTab == 0) {
            EqualizerPanelContent(accentBlue = accentBlue, accentGold = accentGold)
        } else {
            DolbyAtmosPanelContent(accentGold = accentGold)
        }
    }
}

@Composable
private fun EqualizerPanelContent(
    accentBlue: Color,
    accentGold: Color,
) {
    val context = LocalContext.current
    val eqEnabled = HardwareDolbyAndEqualizerEngine.equalizerEnabled
    val currentPreset = HardwareDolbyAndEqualizerEngine.selectedPresetName
    val bands = HardwareDolbyAndEqualizerEngine.bandGainsDb
    var reverbMenuExpanded by remember { mutableStateOf(false) }

    // 1. Header Row with Equalizer On/Off Switch & Reset (matching 24072_c798dc.png)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = if (eqEnabled) "Active · $currentPreset preset" else "Off · Tap switch to enable 5-band EQ",
                style = MaterialTheme.typography.bodySmall,
                color = if (eqEnabled) accentBlue else Color.White.copy(alpha = 0.60f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(
                onClick = { HardwareDolbyAndEqualizerEngine.resetEqualizerGains() },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Equalizer",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = if (eqEnabled) "On" else "Off",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Switch(
                checked = eqEnabled,
                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentBlue,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.20f),
                ),
            )
        }
    }

    // 2. Horizontal Preset Chips (Custom, Normal, Classical, Dance, Flat, Folk, Heavy Metal, Hip Hop, Jazz, Pop, Rock...)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HardwareDolbyAndEqualizerEngine.PRESETS.forEach { preset ->
            val selected = currentPreset.equals(preset.name, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) accentBlue.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.04f),
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) accentBlue else Color.White.copy(alpha = 0.24f),
                        shape = CircleShape,
                    )
                    .clickable {
                        if (!eqEnabled) {
                            HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                        }
                        HardwareDolbyAndEqualizerEngine.selectPreset(preset)
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = preset.name,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) accentBlue else Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }

    // 3. Vertical 5-Band Sliders (60 Hz, 230 Hz, 910 Hz, 3600 Hz, 14000 Hz with dB readout on top)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HardwareDolbyAndEqualizerEngine.BAND_LABELS.forEachIndexed { index, freqLabel ->
            val gainDb = bands.getOrElse(index) { 0f }
            VerticalEqBandSlider(
                gainDb = gainDb,
                freqLabel = freqLabel,
                enabled = eqEnabled,
                accentColor = accentBlue,
                onGainChange = { newDb ->
                    if (!eqEnabled) {
                        HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                    }
                    HardwareDolbyAndEqualizerEngine.setBandGainDb(index, newDb)
                },
            )
        }
    }

    // 4. Reverb Row (matching 24072_c798dc.png)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable { reverbMenuExpanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Reverb",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.90f),
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = HardwareDolbyAndEqualizerEngine.reverbPreset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = reverbMenuExpanded,
                onDismissRequest = { reverbMenuExpanded = false },
            ) {
                ReverbPresetType.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.title) },
                        onClick = {
                            if (!eqEnabled && preset != ReverbPresetType.NONE) {
                                HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                            }
                            HardwareDolbyAndEqualizerEngine.setReverb(preset)
                            reverbMenuExpanded = false
                        },
                    )
                }
            }
        }
    }

    // 5. Bass Boost & Virtualizer Horizontal Bars (matching 24072_c798dc.png)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalBarEffectSlider(
            label = "Bass Boost",
            percentage = HardwareDolbyAndEqualizerEngine.bassBoostPercent,
            accentColor = accentBlue,
            onPercentageChange = {
                if (!eqEnabled && it > 0) {
                    HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                }
                HardwareDolbyAndEqualizerEngine.setBassBoost(it)
            },
        )
        HorizontalBarEffectSlider(
            label = "Virtualizer",
            percentage = HardwareDolbyAndEqualizerEngine.virtualizerPercent,
            accentColor = accentBlue,
            onPercentageChange = {
                if (!eqEnabled && it > 0) {
                    HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                }
                HardwareDolbyAndEqualizerEngine.setVirtualizer(it)
            },
        )
    }

    // 6. Smooth Audio Fade toggle (from RetroMusicPlayer AudioFader)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable {
                HardwareDolbyAndEqualizerEngine.setAudioFade(!HardwareDolbyAndEqualizerEngine.audioFadeEnabled)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Smooth Play/Pause Audio Fade",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "Gently fades audio in and out when pausing or resuming",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
        Switch(
            checked = HardwareDolbyAndEqualizerEngine.audioFadeEnabled,
            onCheckedChange = { HardwareDolbyAndEqualizerEngine.setAudioFade(it) },
            colors = SwitchDefaults.colors(checkedTrackColor = accentGold),
        )
    }
}

@Composable
private fun VerticalEqBandSlider(
    gainDb: Float,
    freqLabel: String,
    enabled: Boolean,
    accentColor: Color,
    onGainChange: (Float) -> Unit,
) {
    val activeColor = if (enabled) accentColor else accentColor.copy(alpha = 0.45f)
    val dbText = "${gainDb.roundToInt()} dB"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(56.dp),
    ) {
        Text(
            text = dbText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(148.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val fraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                            val newDb = -15f + fraction * 30f
                            onGainChange(newDb)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val fraction = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                            val newDb = -15f + fraction * 30f
                            onGainChange(newDb)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val trackTop = 10.dp.toPx()
                val trackBottom = size.height - 10.dp.toPx()
                val trackHeight = (trackBottom - trackTop).coerceAtLeast(1f)
                val normalized = ((gainDb + 15f) / 30f).coerceIn(0f, 1f)
                val thumbY = trackBottom - normalized * trackHeight

                // Inactive vertical line
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(cx, trackTop),
                    end = Offset(cx, trackBottom),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Active vertical line from bottom to thumb
                drawLine(
                    color = activeColor,
                    start = Offset(cx, thumbY),
                    end = Offset(cx, trackBottom),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Thumb circle (matching 24072_c798dc.png)
                drawCircle(
                    color = activeColor,
                    radius = 8.5.dp.toPx(),
                    center = Offset(cx, thumbY),
                )
            }
        }

        Text(
            text = freqLabel,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.70f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun HorizontalBarEffectSlider(
    label: String,
    percentage: Int,
    accentColor: Color,
    onPercentageChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.width(86.dp),
        )
        Slider(
            value = percentage.toFloat(),
            onValueChange = { onPercentageChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            ),
        )
        Text(
            text = "$percentage%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp),
        )
    }
}

@Composable
private fun DolbyAtmosPanelContent(
    accentGold: Color,
) {
    val context = LocalContext.current
    val dolbyOn = HardwareDolbyAndEqualizerEngine.dolbyEnabled
    val currentProfile = HardwareDolbyAndEqualizerEngine.dolbyProfile
    val currentIeq = HardwareDolbyAndEqualizerEngine.dolbyIeq

    // 1. Master Dolby Atmos Card (ported from hardware_dolby-16 LunarisDolby)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentGold.copy(alpha = if (dolbyOn) 0.22f else 0.08f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = if (dolbyOn) accentGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(!dolbyOn) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Use Dolby Atmos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = if (dolbyOn) {
                    "On (${currentProfile.title}) · ${if (HardwareDolbyAndEqualizerEngine.isHardwareDapSupported) "Hardware DAP HAL" else "Multi-Band DSP"}"
                } else {
                    "Off · Tap to enable Dolby Atmos spatial processing"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (dolbyOn) accentGold else Color.White.copy(alpha = 0.65f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = { HardwareDolbyAndEqualizerEngine.resetDolbyDefaults() },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Dolby profiles",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(19.dp),
                )
            }
            Switch(
                checked = dolbyOn,
                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF090D16),
                    checkedTrackColor = accentGold,
                ),
            )
        }
    }

    // 2. Choose a Profile (Dynamic, Movie/Video, Music, Game, Work, Casual, Mood)
    Text(
        text = "CHOOSE A PROFILE",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.70f),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DolbyProfileType.entries.chunked(2).forEach { rowProfiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowProfiles.forEach { profile ->
                    val selected = currentProfile == profile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) accentGold.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.05f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) accentGold else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                if (!dolbyOn) HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(true)
                                HardwareDolbyAndEqualizerEngine.setDolbyProfileType(profile)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column {
                            Text(
                                text = profile.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accentGold else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = profile.subtitle,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowProfiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // 3. Intelligent Equalizer (IEQ: Off, Detailed, Balanced, Warm)
    Text(
        text = "INTELLIGENT EQUALIZER",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.70f),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DolbyIeqPreset.entries.forEach { ieq ->
            val selected = currentIeq == ieq
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) accentGold
                        else Color.White.copy(alpha = 0.06f),
                    )
                    .clickable {
                        HardwareDolbyAndEqualizerEngine.setDolbyIeqPreset(ieq)
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ieq.title,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color(0xFF090D16) else Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }

    // 4. Dialogue Enhancer & Bass/Mid/Treble Tuning Card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dialogue enhancer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Improves clarity of voices and speech (${HardwareDolbyAndEqualizerEngine.dialogueEnhancerAmount}/12)",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Switch(
                checked = HardwareDolbyAndEqualizerEngine.dialogueEnhancerEnabled,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDialogueEnhancer(enabled = it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = accentGold),
            )
        }
        if (HardwareDolbyAndEqualizerEngine.dialogueEnhancerEnabled) {
            Slider(
                value = HardwareDolbyAndEqualizerEngine.dialogueEnhancerAmount.toFloat(),
                onValueChange = {
                    HardwareDolbyAndEqualizerEngine.setDialogueEnhancer(
                        enabled = true,
                        amount = it.roundToInt(),
                    )
                },
                valueRange = 1f..12f,
                colors = SliderDefaults.colors(
                    thumbColor = accentGold,
                    activeTrackColor = accentGold,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                ),
            )
        }

        HorizontalBarEffectSlider(
            label = "Bass level",
            percentage = HardwareDolbyAndEqualizerEngine.bassLevel,
            accentColor = accentGold,
            onPercentageChange = {
                HardwareDolbyAndEqualizerEngine.setDolbyBass(
                    enabled = it > 0,
                    level = it,
                )
            },
        )
        HorizontalBarEffectSlider(
            label = "Mid level",
            percentage = HardwareDolbyAndEqualizerEngine.midLevel,
            accentColor = accentGold,
            onPercentageChange = {
                HardwareDolbyAndEqualizerEngine.setDolbyMidTreble(mid = it)
            },
        )
        HorizontalBarEffectSlider(
            label = "Treble level",
            percentage = HardwareDolbyAndEqualizerEngine.trebleLevel,
            accentColor = accentGold,
            onPercentageChange = {
                HardwareDolbyAndEqualizerEngine.setDolbyMidTreble(treble = it)
            },
        )
    }

    // 5. Surround Virtualizer, Stereo Widening & Volume Leveler
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Headphone surround virtualizer",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Creates virtual 3D surround sound on headphones",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Switch(
                checked = HardwareDolbyAndEqualizerEngine.headphoneVirtualizer,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(hpVirt = it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = accentGold),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Speaker surround virtualizer",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Expands spatial soundstage on built-in speakers",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Switch(
                checked = HardwareDolbyAndEqualizerEngine.speakerVirtualizer,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(spkVirt = it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = accentGold),
            )
        }

        HorizontalBarEffectSlider(
            label = "Stereo wide",
            percentage = HardwareDolbyAndEqualizerEngine.stereoWidening,
            accentColor = accentGold,
            onPercentageChange = {
                HardwareDolbyAndEqualizerEngine.setDolbySurround(widening = it)
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Volume leveler",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Balances volume for a consistent listening experience",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Switch(
                checked = HardwareDolbyAndEqualizerEngine.volumeLevelerEnabled,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(volLeveler = it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = accentGold),
            )
        }
    }
}
