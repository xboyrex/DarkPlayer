package dev.xboyrex.darkplayer.core.ui.audio

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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSegmentedControl
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSlider
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSwitch
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
            .padding(horizontal = 16.dp)
            .padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Glasmio Segmented Control: Equalizer | Dolby Atmos
        GlasmioSegmentedControl(
            items = listOf(
                0 to "Equalizer",
                1 to "Dolby Atmos",
            ),
            selected = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth(),
            height = 40.dp,
        )

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
    val eqEnabled = HardwareDolbyAndEqualizerEngine.equalizerEnabled
    val currentPreset = HardwareDolbyAndEqualizerEngine.selectedPresetName
    val bands = HardwareDolbyAndEqualizerEngine.bandGainsDb
    var reverbMenuExpanded by remember { mutableStateOf(false) }

    // 1. Header Row with Equalizer On/Off GlasmioSwitch & Reset
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = if (eqEnabled) "Active · $currentPreset preset" else "Off · Tap switch to enable 5-band EQ",
                style = MaterialTheme.typography.bodySmall,
                color = if (eqEnabled) accentBlue else Color(0xB2EBEBF5),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = { HardwareDolbyAndEqualizerEngine.resetEqualizerGains() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Equalizer",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp),
                )
            }
            GlasmioSwitch(
                checked = eqEnabled,
                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(it) },
                onTintColor = accentBlue,
            )
        }
    }

    // 2. Horizontal Preset Chips
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
                        if (selected) accentBlue.copy(alpha = 0.18f)
                        else Color(0xFF1C1C1E),
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) accentBlue else Color(0x2BFFFFFF),
                        shape = CircleShape,
                    )
                    .clickable {
                        if (!eqEnabled) {
                            HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(true)
                        }
                        HardwareDolbyAndEqualizerEngine.selectPreset(preset)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = preset.name,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) accentBlue else Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }

    // 3. Vertical 5-Band Sliders (60 Hz, 230 Hz, 910 Hz, 3600 Hz, 14000 Hz)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
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

    // 4. Reverb Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
            .clickable { reverbMenuExpanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Reverb",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = HardwareDolbyAndEqualizerEngine.reverbPreset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xB2EBEBF5),
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xB2EBEBF5),
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

    // 5. Bass Boost & Virtualizer Bars (using GlasmioSlider)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
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

    // 6. Smooth Audio Fade toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
            .clickable {
                HardwareDolbyAndEqualizerEngine.setAudioFade(!HardwareDolbyAndEqualizerEngine.audioFadeEnabled)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Smooth Play/Pause Audio Fade",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "Gently fades audio in and out when pausing or resuming",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xB2EBEBF5),
            )
        }
        GlasmioSwitch(
            checked = HardwareDolbyAndEqualizerEngine.audioFadeEnabled,
            onCheckedChange = { HardwareDolbyAndEqualizerEngine.setAudioFade(it) },
            onTintColor = accentGold,
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
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
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

                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(cx, trackTop),
                    end = Offset(cx, trackBottom),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                drawLine(
                    color = activeColor,
                    start = Offset(cx, thumbY),
                    end = Offset(cx, trackBottom),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )

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
            color = Color(0xB2EBEBF5),
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
            color = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.width(86.dp),
        )
        GlasmioSlider(
            value = percentage.toFloat(),
            onValueChange = { onPercentageChange(it.roundToInt()) },
            valueRange = 0f..100f,
            accentColor = accentColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$percentage%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp),
        )
    }
}

@Composable
private fun DolbyAtmosPanelContent(
    accentGold: Color,
) {
    val dolbyOn = HardwareDolbyAndEqualizerEngine.dolbyEnabled
    val currentProfile = HardwareDolbyAndEqualizerEngine.dolbyProfile
    val currentIeq = HardwareDolbyAndEqualizerEngine.dolbyIeq

    // 1. Master Dolby Atmos Card with GlasmioSwitch
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(
                width = 1.dp,
                color = if (dolbyOn) accentGold.copy(alpha = 0.55f) else Color(0x2BFFFFFF),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable { HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(!dolbyOn) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = "Use Dolby Atmos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = if (dolbyOn) {
                    "On (${currentProfile.title})"
                } else {
                    "Off · Tap to enable Dolby Atmos spatial processing"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (dolbyOn) accentGold else Color(0xB2EBEBF5),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = { HardwareDolbyAndEqualizerEngine.resetDolbyDefaults() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Dolby profiles",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp),
                )
            }
            GlasmioSwitch(
                checked = dolbyOn,
                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(it) },
                onTintColor = accentGold,
            )
        }
    }

    // 2. Choose a Profile
    Text(
        text = "CHOOSE A PROFILE",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xB2EBEBF5),
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
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) accentGold.copy(alpha = 0.16f)
                                else Color(0xFF1C1C1E),
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) accentGold else Color(0x2BFFFFFF),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable {
                                if (!dolbyOn) HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(true)
                                HardwareDolbyAndEqualizerEngine.setDolbyProfileType(profile)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                color = Color(0xB2EBEBF5),
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

    // 3. Intelligent Equalizer (GlasmioSegmentedControl)
    Text(
        text = "INTELLIGENT EQUALIZER",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xB2EBEBF5),
    )
    GlasmioSegmentedControl(
        items = DolbyIeqPreset.entries.map { it to it.title },
        selected = currentIeq,
        onSelected = { HardwareDolbyAndEqualizerEngine.setDolbyIeqPreset(it) },
        modifier = Modifier.fillMaxWidth(),
        height = 38.dp,
    )

    // 4. Dialogue Enhancer & Bass/Mid/Treble Tuning Card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Dialogue enhancer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Improves clarity of voices and speech (${HardwareDolbyAndEqualizerEngine.dialogueEnhancerAmount}/12)",
                    fontSize = 11.5.sp,
                    color = Color(0xB2EBEBF5),
                )
            }
            GlasmioSwitch(
                checked = HardwareDolbyAndEqualizerEngine.dialogueEnhancerEnabled,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDialogueEnhancer(enabled = it)
                },
                onTintColor = accentGold,
            )
        }
        if (HardwareDolbyAndEqualizerEngine.dialogueEnhancerEnabled) {
            GlasmioSlider(
                value = HardwareDolbyAndEqualizerEngine.dialogueEnhancerAmount.toFloat(),
                onValueChange = {
                    HardwareDolbyAndEqualizerEngine.setDialogueEnhancer(
                        enabled = true,
                        amount = it.roundToInt(),
                    )
                },
                valueRange = 1f..12f,
                accentColor = accentGold,
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
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Headphone surround virtualizer",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Creates virtual 3D surround sound on headphones",
                    fontSize = 11.5.sp,
                    color = Color(0xB2EBEBF5),
                )
            }
            GlasmioSwitch(
                checked = HardwareDolbyAndEqualizerEngine.headphoneVirtualizer,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(hpVirt = it)
                },
                onTintColor = accentGold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Speaker surround virtualizer",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Expands spatial soundstage on built-in speakers",
                    fontSize = 11.5.sp,
                    color = Color(0xB2EBEBF5),
                )
            }
            GlasmioSwitch(
                checked = HardwareDolbyAndEqualizerEngine.speakerVirtualizer,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(spkVirt = it)
                },
                onTintColor = accentGold,
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
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Volume leveler",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "Balances volume for a consistent listening experience",
                    fontSize = 11.5.sp,
                    color = Color(0xB2EBEBF5),
                )
            }
            GlasmioSwitch(
                checked = HardwareDolbyAndEqualizerEngine.volumeLevelerEnabled,
                onCheckedChange = {
                    HardwareDolbyAndEqualizerEngine.setDolbySurround(volLeveler = it)
                },
                onTintColor = accentGold,
            )
        }
    }
}
