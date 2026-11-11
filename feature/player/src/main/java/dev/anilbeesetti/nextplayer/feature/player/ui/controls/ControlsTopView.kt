package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import android.content.Context
import android.os.BatteryManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.model.MxQuickShortcut
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * MX Player Pro Top Bar + Collapsible Quick Shortcuts Row
 * Exact implementation of Screenshot_20261110-155632, 155634, 155640_MX Player Pro.png (#18, #19, #20).
 */
@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    showQuickShortcuts: Boolean = true,
    enabledShortcuts: List<String> = MxQuickShortcut.entries.map { it.name },
    showBatteryClock: Boolean = true,
    isNightModeActive: Boolean = false,
    isMuted: Boolean = false,
    isShuffleOn: Boolean = false,
    isLoopOn: Boolean = false,
    abRepeatLabel: String? = null,
    sleepTimerActive: Boolean = false,
    playbackSpeed: Float = 1.0f,
    onBackClick: () -> Unit = {},
    onDecoderClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onMoreMenuClick: () -> Unit = {},
    onShortcutClick: (MxQuickShortcut) -> Unit = {},
) {
    val firstControlFocusRequester = remember { FocusRequester() }
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() == 0.dp) 12.dp else 0.dp
    var shortcutsExpanded by rememberSaveable { mutableStateOf(false) }
    val accent = LocalNovaAccentColor.current
    val context = LocalContext.current

    val clockAndBattery by produceState(initialValue = "" to 100, context) {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
            value = fmt.format(Date()) to pct
            delay(15_000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = extraTopPadding, bottom = 8.dp),
    ) {
        // 1. Main Title Bar matching MX Player Pro (#18, #19, #20)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { firstControlFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineBreak = LineBreak.Heading,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showBatteryClock && clockAndBattery.first.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = clockAndBattery.first,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Icon(
                            imageVector = Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${clockAndBattery.second}%",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            // Right-side MX Player Pro Action Icons: Audio Track (♪), Subtitle (CC), Decoder (HW/HW+/SW), 3-Dots More (⋮)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onAudioClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_audio_track),
                        contentDescription = "Audio Track",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onSubtitleClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitle_track),
                        contentDescription = "Subtitles",
                        tint = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .semantics { contentDescription = decoderDescription }
                        .focusRequester(focusRequester = firstControlFocusRequester)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onDecoderClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onPlaylistClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_playlist),
                        contentDescription = "Playlist",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onMoreMenuClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                    )
                }
            }
        }

        // 2. MX Player Pro Collapsible Quick Shortcuts Row (#18 collapsed, #19 & #20 expanded)
        if (showQuickShortcuts && enabledShortcuts.isNotEmpty()) {
            val activeShortcuts = remember(enabledShortcuts) {
                MxQuickShortcut.entries.filter { it.name in enabledShortcuts }
                    .ifEmpty { MxQuickShortcut.entries }
            }

            AnimatedVisibility(
                visible = !shortcutsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val compactShortcuts = listOf(
                        MxQuickShortcut.EQUALIZER,
                        MxQuickShortcut.PLAYBACK_SPEED,
                        MxQuickShortcut.SCREENSHOT,
                        MxQuickShortcut.BACKGROUND_PLAY,
                        MxQuickShortcut.SCREEN_ROTATION,
                    ).filter { it in activeShortcuts }.ifEmpty { activeShortcuts.take(5) }

                    compactShortcuts.forEach { shortcut ->
                        MxCompactShortcutCircle(
                            shortcut = shortcut,
                            playbackSpeed = playbackSpeed,
                            accent = accent,
                            isActive = when (shortcut) {
                                MxQuickShortcut.NIGHT_MODE -> isNightModeActive
                                MxQuickShortcut.MUTE -> isMuted
                                MxQuickShortcut.SHUFFLE -> isShuffleOn
                                MxQuickShortcut.LOOP -> isLoopOn
                                MxQuickShortcut.AB_REPEAT -> abRepeatLabel != null
                                MxQuickShortcut.SLEEP_TIMER -> sleepTimerActive
                                else -> false
                            },
                            onClick = { onShortcutClick(shortcut) },
                        )
                    }

                    // Expand button '>' matching MX #18 (155632)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .clickable { shortcutsExpanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Expand Shortcuts",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = shortcutsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    activeShortcuts.forEach { shortcut ->
                        val isActive = when (shortcut) {
                            MxQuickShortcut.NIGHT_MODE -> isNightModeActive
                            MxQuickShortcut.MUTE -> isMuted
                            MxQuickShortcut.SHUFFLE -> isShuffleOn
                            MxQuickShortcut.LOOP -> isLoopOn
                            MxQuickShortcut.AB_REPEAT -> abRepeatLabel != null
                            MxQuickShortcut.SLEEP_TIMER -> sleepTimerActive
                            else -> false
                        }
                        MxExpandedShortcutItem(
                            shortcut = shortcut,
                            playbackSpeed = playbackSpeed,
                            abRepeatLabel = abRepeatLabel,
                            isActive = isActive,
                            accent = accent,
                            onClick = { onShortcutClick(shortcut) },
                        )
                    }

                    // Collapse button '<'
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable { shortcutsExpanded = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Collapse Shortcuts",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MxCompactShortcutCircle(
    shortcut: MxQuickShortcut,
    playbackSpeed: Float,
    accent: Color,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isActive) accent.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.52f))
            .border(
                width = 0.5.dp,
                color = if (isActive) accent else Color.White.copy(alpha = 0.18f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (shortcut == MxQuickShortcut.PLAYBACK_SPEED) {
            Text(
                text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else "%.1f".format(playbackSpeed)}X",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = shortcut.iconVector(),
                contentDescription = shortcut.defaultTitle,
                tint = if (isActive) accent else Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun MxExpandedShortcutItem(
    shortcut: MxQuickShortcut,
    playbackSpeed: Float,
    abRepeatLabel: String?,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isActive) accent.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.55f))
                .border(
                    width = 1.dp,
                    color = if (isActive) accent else Color.White.copy(alpha = 0.16f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (shortcut) {
                MxQuickShortcut.PLAYBACK_SPEED -> {
                    Text(
                        text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else "%.1f".format(playbackSpeed)}X",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                MxQuickShortcut.AB_REPEAT -> {
                    Text(
                        text = abRepeatLabel ?: "A⇄B",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) accent else Color.White,
                    )
                }
                else -> {
                    Icon(
                        imageVector = shortcut.iconVector(),
                        contentDescription = shortcut.defaultTitle,
                        tint = if (isActive) accent else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Text(
            text = shortcut.defaultTitle,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = if (isActive) accent else Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun MxQuickShortcut.iconVector(): ImageVector = when (this) {
    MxQuickShortcut.NIGHT_MODE -> Icons.Rounded.Bedtime
    MxQuickShortcut.CUSTOMISE_ITEMS -> Icons.Rounded.Edit
    MxQuickShortcut.SHUFFLE -> Icons.Rounded.Shuffle
    MxQuickShortcut.LOOP -> Icons.Rounded.Repeat
    MxQuickShortcut.MUTE -> Icons.Rounded.VolumeOff
    MxQuickShortcut.SLEEP_TIMER -> Icons.Rounded.Timer
    MxQuickShortcut.AB_REPEAT -> Icons.Rounded.RepeatOne
    MxQuickShortcut.EQUALIZER -> Icons.Rounded.Equalizer
    MxQuickShortcut.PLAYBACK_SPEED -> Icons.Rounded.Speed
    MxQuickShortcut.SCREENSHOT -> Icons.Rounded.CameraAlt
    MxQuickShortcut.BACKGROUND_PLAY -> Icons.Rounded.VolumeUp
    MxQuickShortcut.SCREEN_ROTATION -> Icons.Rounded.ScreenRotation
}

@Preview
@Composable
private fun ControlsTopViewPreview() {
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsTopView(
                title = "The.Core.2003.1080p.BluRay.Hindi.English.DD.5.1.x264.ESubs",
                videoDecoderMode = DecoderMode.HARDWARE,
                onBackClick = {},
            )
        }
    }
}
