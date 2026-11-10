package dev.xboyrex.darkplayer.feature.player.ui.controls

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.MxQuickShortcut
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.model.labelRes
import dev.xboyrex.darkplayer.feature.player.utils.VideoFrameCapture
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    player: Player? = null,
    onBackClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onMoreDrawerClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onPlayInBackgroundClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstControlFocusRequester = remember { FocusRequester() }
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() < 12.dp) 14.dp else 6.dp

    var shortcutsExpanded by rememberSaveable { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf((player?.volume ?: 1f) == 0f) }
    var abPointAMs by remember { mutableLongStateOf(-1L) }
    var abPointBMs by remember { mutableLongStateOf(-1L) }

    // A-B Repeat live loop engine
    LaunchedEffect(player, abPointAMs, abPointBMs) {
        if (player != null && abPointAMs >= 0L && abPointBMs > abPointAMs) {
            while (true) {
                if (player.currentPosition >= abPointBMs) {
                    player.seekTo(abPointAMs)
                }
                delay(120L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.76f),
                        Color.Black.copy(alpha = 0.42f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = extraTopPadding, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Top Header Bar (<-, Title, Audio Track, Subtitle, HW, 3-dot More menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { firstControlFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineBreak = LineBreak.Heading,
                    fontSize = 15.5.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PlayerButton(onClick = onAudioClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_audio_track),
                        contentDescription = null,
                    )
                }
                PlayerButton(onClick = onSubtitleClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitle_track),
                        contentDescription = null,
                    )
                }
                PlayerButton(
                    modifier = Modifier
                        .semantics { contentDescription = decoderDescription }
                        .focusRequester(focusRequester = firstControlFocusRequester),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    onClick = onDecoderClick,
                ) {
                    Text(
                        text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                PlayerButton(onClick = onPlaylistClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_playlist),
                        contentDescription = null,
                    )
                }
                PlayerButton(onClick = onMoreDrawerClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                    )
                }
            }
        }

        // Row 2: MX Player Pro Expandable Quick Shortcuts Ribbon (155632, 155634, 155640)
        if (UiStylePreferences.shortcutsBarVisible) {
            val enabledSet = UiStylePreferences.enabledShortcuts
            val speedText = player?.playbackParameters?.speed?.let { sp ->
                if (sp == sp.toInt().toFloat()) "${sp.toInt()}X" else "${String.format("%.1f", sp)}X"
            } ?: "1X"

            AnimatedContent(
                targetState = shortcutsExpanded,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mxShortcutsRibbon",
            ) { expanded ->
                if (!expanded) {
                    // Collapsed compact pill row (matching 155632_MX Player Pro.png)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MxCompactCircleShortcut(
                            onClick = onEqualizerClick,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_equalizer),
                                contentDescription = "Equalizer",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        MxCompactCircleShortcut(
                            onClick = onPlaybackSpeedClick,
                        ) {
                            Text(
                                text = speedText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                            )
                        }
                        MxCompactCircleShortcut(
                            showRedDot = true,
                            onClick = {
                                VideoFrameCapture.captureCurrentFrame(
                                    context = context,
                                    player = player,
                                    scope = scope,
                                )
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_screenshot),
                                contentDescription = "Screenshot",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        MxCompactCircleShortcut(
                            onClick = onPlayInBackgroundClick,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_headset),
                                contentDescription = "Background Play",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        MxCompactCircleShortcut(
                            onClick = {
                                val act = context as? Activity
                                if (act != null) {
                                    val isLand = act.resources.configuration.orientation ==
                                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                    act.requestedOrientation = if (isLand) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_screen_rotation),
                                contentDescription = "Rotate",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        // Expand chevron button (>)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.54f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable { shortcutsExpanded = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Expand shortcuts",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    // Full Expanded Labeled MX Player Quick Shortcuts Bar (matching 155634 & 155640)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (enabledSet.contains(MxQuickShortcut.NIGHT_MODE.id)) {
                            MxLabeledShortcutItem(
                                label = "Night Mode",
                                active = UiStylePreferences.nightModeEnabled,
                                onClick = { UiStylePreferences.toggleNightMode() },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = "Night Mode",
                                    tint = if (UiStylePreferences.nightModeEnabled) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.CUSTOMISE.id)) {
                            MxLabeledShortcutItem(
                                label = "Customise\nItems",
                                onClick = onMoreDrawerClick,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Customise Items",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.SHUFFLE.id)) {
                            val shuffleOn = player?.shuffleModeEnabled == true
                            MxLabeledShortcutItem(
                                label = "Shuffle",
                                active = shuffleOn,
                                onClick = {
                                    player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (shuffleOn) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle,
                                    ),
                                    contentDescription = "Shuffle",
                                    tint = if (shuffleOn) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.LOOP.id)) {
                            val repeatMode = player?.repeatMode ?: Player.REPEAT_MODE_OFF
                            MxLabeledShortcutItem(
                                label = "Loop",
                                active = repeatMode != Player.REPEAT_MODE_OFF,
                                onClick = {
                                    player?.let {
                                        it.repeatMode = when (it.repeatMode) {
                                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                            else -> Player.REPEAT_MODE_OFF
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ONE -> R.drawable.ic_loop_one
                                            Player.REPEAT_MODE_ALL -> R.drawable.ic_loop_all
                                            else -> R.drawable.ic_loop_off
                                        },
                                    ),
                                    contentDescription = "Loop",
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.MUTE.id)) {
                            MxLabeledShortcutItem(
                                label = "Mute",
                                active = isMuted,
                                onClick = {
                                    player?.let {
                                        if (isMuted) {
                                            it.volume = 1f
                                            isMuted = false
                                        } else {
                                            it.volume = 0f
                                            isMuted = true
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = if (isMuted) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.SLEEP_TIMER.id)) {
                            MxLabeledShortcutItem(
                                label = "Sleep Timer",
                                onClick = onSleepTimerClick,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = "Sleep Timer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.AB_REPEAT.id)) {
                            val abActive = abPointAMs >= 0L
                            val abLabel = when {
                                abPointAMs >= 0L && abPointBMs > abPointAMs -> "A-B Active"
                                abPointAMs >= 0L -> "Set B..."
                                else -> "A - B\nRepeat"
                            }
                            MxLabeledShortcutItem(
                                label = abLabel,
                                active = abActive,
                                onClick = {
                                    val pos = player?.currentPosition ?: 0L
                                    when {
                                        abPointAMs < 0L -> {
                                            abPointAMs = pos
                                            Toast.makeText(
                                                context,
                                                "Point A set at ${getStringForTime(pos)}. Tap again to set Point B.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        abPointBMs < 0L && pos > abPointAMs -> {
                                            abPointBMs = pos
                                            Toast.makeText(
                                                context,
                                                "Looping A (${getStringForTime(abPointAMs)}) → B (${getStringForTime(abPointBMs)})",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        else -> {
                                            abPointAMs = -1L
                                            abPointBMs = -1L
                                            Toast.makeText(context, "A-B Repeat cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = "A-B Repeat",
                                    tint = if (abActive) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.EQUALIZER.id)) {
                            MxLabeledShortcutItem(
                                label = "Equalizer",
                                onClick = onEqualizerClick,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_equalizer),
                                    contentDescription = "Equalizer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.SPEED.id)) {
                            MxLabeledShortcutItem(
                                label = "Speed",
                                onClick = onPlaybackSpeedClick,
                            ) {
                                Text(
                                    text = speedText,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.SCREENSHOT.id)) {
                            MxLabeledShortcutItem(
                                label = "Screenshot",
                                showRedDot = true,
                                onClick = {
                                    VideoFrameCapture.captureCurrentFrame(
                                        context = context,
                                        player = player,
                                        scope = scope,
                                    )
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_screenshot),
                                    contentDescription = "Screenshot",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.BACKGROUND_PLAY.id)) {
                            MxLabeledShortcutItem(
                                label = "Background\nPlay",
                                onClick = onPlayInBackgroundClick,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_headset),
                                    contentDescription = "Background Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (enabledSet.contains(MxQuickShortcut.ROTATION.id)) {
                            MxLabeledShortcutItem(
                                label = "Screen\nRotation",
                                onClick = {
                                    val act = context as? Activity
                                    if (act != null) {
                                        val isLand = act.resources.configuration.orientation ==
                                            android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                        act.requestedOrientation = if (isLand) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_screen_rotation),
                                    contentDescription = "Screen Rotation",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        // Collapse chevron (<)
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.54f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable { shortcutsExpanded = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Collapse shortcuts",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MxCompactCircleShortcut(
    showRedDot: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.52f))
                .border(0.6.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        if (showRedDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
            )
        }
    }
}

@Composable
private fun MxLabeledShortcutItem(
    label: String,
    active: Boolean = false,
    showRedDot: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 52.dp, max = 66.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Color(0xFF38BDF8).copy(alpha = 0.24f)
                        else Color.Black.copy(alpha = 0.52f),
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (active) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.20f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
            if (showRedDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-1).dp, y = 1.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                )
            }
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 10.5.sp,
            lineHeight = 12.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
                onDecoderClick = {},
                onAudioClick = {},
                onSubtitleClick = {},
                onPlaylistClick = {},
            )
        }
    }
}
