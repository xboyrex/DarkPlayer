package dev.xboyrex.darkplayer.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.audio.EqualizerAndDolbyStudioContent
import dev.xboyrex.darkplayer.core.ui.audio.HardwareDolbyAndEqualizerEngine
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.display.ColoursAndDisplayEngineContent
import dev.xboyrex.darkplayer.core.ui.display.RealityDisplayEngine
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioIconButton
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSwitch
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.updater.UpdateOverlayDialog
import dev.xboyrex.darkplayer.core.ui.updater.rememberUpdateState

data class SettingsOutput(val navigateUp: () -> Unit, val openSetting: (Setting) -> Unit)

private sealed interface SettingsAction {
    data object NavigateUp : SettingsAction
    data class OpenSetting(val setting: Setting) : SettingsAction
}

private enum class YtxFloatingPanel {
    COLOURS_DISPLAY_ENGINE,
    AUDIO_DOLBY_STUDIO,
}

@Composable
fun SettingsScreen(output: SettingsOutput) {
    SettingsScreenContent { action ->
        when (action) {
            is SettingsAction.NavigateUp -> output.navigateUp()
            is SettingsAction.OpenSetting -> output.openSetting(action.setting)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(onAction: (SettingsAction) -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    val focusState = rememberRestorableFocusState()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val updateState = rememberUpdateState()
    val installedVersion = remember(context) { updateState.currentVersionName(context) }

    LaunchedEffect(context) {
        RealityDisplayEngine.init(context)
        HardwareDolbyAndEqualizerEngine.init(context)
        GlassSettings.init(context)
    }

    var activeFloatingPanel by rememberSaveable { mutableStateOf<YtxFloatingPanel?>(null) }
    var engineCardExpanded by rememberSaveable { mutableStateOf(true) }
    var playbackSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var interfaceSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var systemSectionExpanded by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = activeFloatingPanel != null) {
        activeFloatingPanel = null
    }

    val isOverlayOpen = activeFloatingPanel != null || updateState.isPopupActive
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 18.dp else 0.dp,
        animationSpec = tween(220),
        label = "ytxSettingsBlur",
    )

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = Motion.Emphasized),
        label = "settingsEnterAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
            containerColor = Color.Black,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusTopInset + 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlasmioIconButton(
                        onClick = { onAction(SettingsAction.NavigateUp) },
                        size = 44.dp,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(id = R.string.settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color.White,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .graphicsLayer { alpha = enterAlpha }
                    .verticalScroll(rememberScrollState())
                    .restorableFocusGroup(state = focusState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = navBottomInset + 36.dp),
            ) {
                // ── 1. Glasmio + YTX Expandable Nova Engine Hub Card ──
                YtxSettingsCard {
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (engineCardExpanded) 90f else 0f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                        label = "engineChevron",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { engineCardExpanded = !engineCardExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f))
                                .border(1.dp, c.primary.copy(alpha = 0.38f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Nova Player Studio & Glasmio Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "${RealityDisplayEngine.colourProfile.title} • ${RealityDisplayEngine.engineMode.title} • Dolby ${if (HardwareDolbyAndEqualizerEngine.dolbyEnabled) "ON" else "OFF"}",
                                fontSize = 12.sp,
                                color = Color(0xB2EBEBF5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = null,
                            tint = Color(0xB2EBEBF5),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation },
                        )
                    }

                    AnimatedVisibility(
                        visible = engineCardExpanded,
                        enter = fadeIn(tween(180)) + expandVertically(tween(260)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            HorizontalDivider(
                                color = Color(0x2BFFFFFF),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            YtxSwitchRow(
                                title = "Dolby Atmos & Dialogue Enhancer",
                                subtitle = "Force crystal-clear vocal boost & surround stage on all videos",
                                checked = HardwareDolbyAndEqualizerEngine.dolbyEnabled,
                                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setDolbyMasterEnabled(it) },
                            )
                            YtxSwitchRow(
                                title = "5-Band / 20-Band Studio Equalizer",
                                subtitle = "Active preset: ${HardwareDolbyAndEqualizerEngine.selectedPresetName}",
                                checked = HardwareDolbyAndEqualizerEngine.equalizerEnabled,
                                onCheckedChange = { HardwareDolbyAndEqualizerEngine.setEqualizerMasterEnabled(it) },
                            )
                            YtxSwitchRow(
                                title = "Glasmio Liquid Glass UI & Navigation",
                                subtitle = "iOS 27 refractive lens blur & spring pill physics enabled by default",
                                checked = GlassSettings.enabled,
                                onCheckedChange = { GlassSettings.updateEnabled(it) },
                            )
                        }
                    }
                }

                // ── 2. Display Engine & Audio Studio (Opens Glasmio Floating Blur Panels) ──
                YtxSectionHeader(
                    title = "DISPLAY ENGINE & AUDIO STUDIO",
                    expanded = true,
                    onClick = null,
                )
                YtxSettingsCard {
                    YtxSettingsRow(
                        icon = Icons.Rounded.ColorLens,
                        title = "Colours & Reality Display Engine",
                        subtitle = "${RealityDisplayEngine.colourProfile.title} • ${RealityDisplayEngine.engineMode.title} (Bravia / X-Reality)",
                        onClick = { activeFloatingPanel = YtxFloatingPanel.COLOURS_DISPLAY_ENGINE },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFF4A298).copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = RealityDisplayEngine.colourProfile.title,
                                    color = Color(0xFFF4A298),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                    )
                    YtxDivider()
                    YtxSettingsRow(
                        icon = Icons.Rounded.Equalizer,
                        title = "Dolby Atmos & Equalizer Studio",
                        subtitle = "Configure 20-band GEQ, Bass Boost, Virtualizer & Low-Sound Pre-Gain",
                        onClick = { activeFloatingPanel = YtxFloatingPanel.AUDIO_DOLBY_STUDIO },
                    )
                }

                // ── 3. Playback, Decoders & Gestures ──
                YtxSectionHeader(
                    title = "PLAYBACK, DECODERS & GESTURES",
                    expanded = playbackSectionExpanded,
                    onClick = { playbackSectionExpanded = !playbackSectionExpanded },
                )
                AnimatedVisibility(
                    visible = playbackSectionExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
                ) {
                    YtxSettingsCard {
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.PLAYER.name),
                            icon = Icons.Rounded.PlayCircleOutline,
                            title = stringResource(id = R.string.player_name),
                            subtitle = stringResource(id = R.string.player_description),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.PLAYER)) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.GESTURES.name),
                            icon = Icons.Rounded.Swipe,
                            title = stringResource(id = R.string.gestures_name),
                            subtitle = stringResource(id = R.string.gestures_description),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.GESTURES)) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.AUDIO.name),
                            icon = Icons.Rounded.VolumeUp,
                            title = stringResource(id = R.string.audio),
                            subtitle = stringResource(id = R.string.audio_desc),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.AUDIO)) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.SUBTITLE.name),
                            icon = Icons.Rounded.Subtitles,
                            title = stringResource(id = R.string.subtitle),
                            subtitle = stringResource(id = R.string.subtitle_desc),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.SUBTITLE)) },
                        )
                    }
                }

                // ── 4. Interface & Media Library ──
                YtxSectionHeader(
                    title = "INTERFACE & MEDIA LIBRARY",
                    expanded = interfaceSectionExpanded,
                    onClick = { interfaceSectionExpanded = !interfaceSectionExpanded },
                )
                AnimatedVisibility(
                    visible = interfaceSectionExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
                ) {
                    YtxSettingsCard {
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.APPEARANCE.name),
                            icon = Icons.Rounded.Palette,
                            title = stringResource(id = R.string.appearance_name),
                            subtitle = stringResource(id = R.string.appearance_description),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.APPEARANCE)) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.MEDIA_LIBRARY.name),
                            icon = Icons.Rounded.VideoLibrary,
                            title = stringResource(id = R.string.media_library),
                            subtitle = stringResource(id = R.string.media_library_description),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.MEDIA_LIBRARY)) },
                        )
                    }
                }

                // ── 5. General, Updates & About ──
                YtxSectionHeader(
                    title = "GENERAL & ABOUT NOVA PLAYER",
                    expanded = systemSectionExpanded,
                    onClick = { systemSectionExpanded = !systemSectionExpanded },
                )
                AnimatedVisibility(
                    visible = systemSectionExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
                ) {
                    YtxSettingsCard {
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.GENERAL.name),
                            icon = Icons.Rounded.Tune,
                            title = stringResource(id = R.string.general_name),
                            subtitle = stringResource(id = R.string.general_description),
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.GENERAL)) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            icon = Icons.Rounded.Info,
                            title = "Version",
                            subtitle = "Nova Player v$installedVersion",
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF30D158).copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF30D158)),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "Up to date",
                                            color = Color(0xFF30D158),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, "CHECK_UPDATES"),
                            icon = Icons.Rounded.FileDownload,
                            title = "Check for updates",
                            subtitle = "New GitHub releases install from inside Nova Player",
                            onClick = { updateState.check(context = context, manual = true) },
                        )
                        YtxDivider()
                        YtxSettingsRow(
                            modifier = Modifier.restorableFocusItem(focusState, SettingRow.ABOUT.name),
                            icon = Icons.Rounded.Info,
                            title = stringResource(id = R.string.about_name),
                            subtitle = "Nova Player architecture, open-source licenses & developer links",
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                        )
                    }
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
            )
        }

        // Glasmio + YTX Animated Floating Settings Modal Sheets
        AnimatedContent(
            targetState = activeFloatingPanel,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(130), targetScale = 0.92f) + fadeOut(tween(130)))
            },
            label = "ytxFloatingPanel",
        ) { panel ->
            when (panel) {
                YtxFloatingPanel.COLOURS_DISPLAY_ENGINE -> {
                    YtxFloatingModal(
                        title = "Colours",
                        subtitle = "Reality display engine • Bravia & X-Reality",
                        onDismiss = { activeFloatingPanel = null },
                    ) {
                        ColoursAndDisplayEngineContent(showPreviewBanner = true)
                    }
                }
                YtxFloatingPanel.AUDIO_DOLBY_STUDIO -> {
                    YtxFloatingModal(
                        title = "Dolby Atmos & Equalizer",
                        subtitle = "Media3 PCM DSP, Hardware DAP & Dialogue Pre-Gain",
                        onDismiss = { activeFloatingPanel = null },
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            EqualizerAndDolbyStudioContent()
                        }
                    }
                }
                null -> {}
            }
        }

        UpdateOverlayDialog(state = updateState)
    }
}

@Composable
private fun YtxSectionHeader(
    title: String,
    expanded: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xB2EBEBF5),
            letterSpacing = 0.6.sp,
        )
        if (onClick != null) {
            val rot by animateFloatAsState(
                targetValue = if (expanded) 90f else 0f,
                animationSpec = tween(180),
                label = "secHeaderRot",
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
                tint = Color(0xB2EBEBF5),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = rot },
            )
        }
    }
}

@Composable
private fun YtxSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), RoundedCornerShape(26.dp)),
        content = content,
    )
}

@Composable
private fun YtxDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Color(0x2BFFFFFF),
        thickness = 0.6.dp,
    )
}

@Composable
private fun YtxSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E))
                .border(0.6.dp, Color(0x2BFFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.5.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = Color(0xB2EBEBF5),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
                tint = Color(0x4DEBEBF5),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun YtxSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = Color.White,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = Color(0xB2EBEBF5),
            )
        }
        GlasmioSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun YtxFloatingModal(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 14.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(Color(0xFF141416).copy(alpha = 0.96f))
                .border(0.8.dp, Color(0x2BFFFFFF), RoundedCornerShape(38.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .padding(top = 18.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xB2EBEBF5),
                    )
                }
                GlasmioIconButton(
                    onClick = onDismiss,
                    size = 36.dp,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0x2BFFFFFF), thickness = 0.6.dp)
            content()
        }
    }
}

enum class Setting {
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    GESTURES,
    AUDIO,
    SUBTITLE,
    GENERAL,
    ABOUT,
}

private enum class SettingRow(
    val setting: Setting,
) {
    PLAYER(Setting.PLAYER),
    GESTURES(Setting.GESTURES),
    AUDIO(Setting.AUDIO),
    SUBTITLE(Setting.SUBTITLE),
    APPEARANCE(Setting.APPEARANCE),
    MEDIA_LIBRARY(Setting.MEDIA_LIBRARY),
    GENERAL(Setting.GENERAL),
    ABOUT(Setting.ABOUT),
}
