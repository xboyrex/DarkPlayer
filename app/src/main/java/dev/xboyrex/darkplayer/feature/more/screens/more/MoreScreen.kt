package dev.xboyrex.darkplayer.feature.more.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SavedSearch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.media.services.MediaOperationsService
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.MxAppThemeMode
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.base.DataState
import dev.xboyrex.darkplayer.core.ui.components.BindTopLevelFab
import dev.xboyrex.darkplayer.core.ui.components.CancelButton
import dev.xboyrex.darkplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.darkplayer.core.ui.components.NextDialog
import dev.xboyrex.darkplayer.core.ui.components.NextOutlinedTextField
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabKey
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoGradientBrush
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaUiSwitcherPopup
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import dev.xboyrex.darkplayer.feature.more.components.HistoryEmptyState
import dev.xboyrex.darkplayer.feature.videopicker.composables.VideoGridItem

@Composable
fun MoreScreen(viewModel: MoreViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }
    val accent = LocalNovaAccentColor.current
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    var showNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }
    var showUiSwitcherPopup by rememberSaveable { mutableStateOf(false) }
    var showStatusSaverDialog by rememberSaveable { mutableStateOf(false) }
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "Nova Hub",
                fontWeight = FontWeight.Bold,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .verticalScroll(state = rememberScrollState())
                .tvListFocus()
                .padding(horizontal = 14.dp)
                .padding(
                    top = 8.dp,
                    bottom =
                        scaffoldPadding.calculateBottomPadding() +
                            LocalNavigationBottomPadding.current + 110.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Top Circular Brand Card + Quick UI Switcher Popup Trigger
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        view.novaHaptic(10L, 125, hapticsEnabled)
                        showUiSwitcherPopup = true
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nova_launcher),
                        contentDescription = "Nova Player Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nova Interface Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Tap to open compact popup for Theme, Dock, Player & HUD",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NovaLogoGradientBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Open UI Switcher",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // 2. Quick Tools 3x2 Grid Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        NovaHubGridItem(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "Open Video",
                            accent = accent,
                            onClick = { filePicker.launch("video/*") },
                            modifier = Modifier.weight(1f),
                        )
                        NovaHubGridItem(
                            icon = Icons.Rounded.FolderSpecial,
                            label = "Private Vault",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenVault) },
                            modifier = Modifier.weight(1f),
                        )
                        NovaHubGridItem(
                            icon = Icons.Rounded.Add,
                            label = "Watch History",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenHistory) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        NovaHubGridItem(
                            icon = Icons.Rounded.Language,
                            label = "Network\nStream",
                            accent = accent,
                            onClick = { showNetworkStreamDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                        NovaHubGridItem(
                            icon = Icons.Rounded.Computer,
                            label = "Preferences",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenSettings) },
                            modifier = Modifier.weight(1f),
                        )
                        NovaHubGridItem(
                            icon = Icons.Rounded.FolderShared,
                            label = "Recycle Bin",
                            accent = accent,
                            onClick = {
                                if (MediaOperationsService.supportsTrash()) {
                                    onAction(MoreAction.OpenTrash)
                                } else {
                                    filePicker.launch("video/*")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 3. Status Clips Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showStatusSaverDialog = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SavedSearch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Saved & Recent Clips",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 4. Interface Studio + Settings Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NovaHubMenuRow(
                        icon = Icons.Rounded.Palette,
                        title = "Theme & Interface Switcher",
                        onClick = { showUiSwitcherPopup = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    )
                    NovaHubMenuRow(
                        icon = Icons.Rounded.Settings,
                        title = "Settings",
                        onClick = { onAction(MoreAction.OpenSettings) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    )
                    NovaHubMenuRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Gestures & Shortcuts Guide",
                        onClick = { showHelpDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    )
                    NovaHubMenuRow(
                        icon = Icons.Rounded.Gavel,
                        title = "Open Source Licenses",
                        onClick = { showLegalDialog = true },
                    )
                }
            }

            // 5. Watch History Section
            HistorySection(
                history = state.history,
                preferences = state.preferences,
                onMoreClick = { onAction(MoreAction.OpenHistory) },
                onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
            )
        }
    }

    if (showUiSwitcherPopup) {
        NovaUiSwitcherPopup(
            currentThemeMode = state.preferences.mxAppThemeMode,
            useDynamicDeviceAccent = state.preferences.useDynamicColors,
            currentNavBarStyle = state.preferences.navBarStyle,
            currentSwitchStyle = state.preferences.switchStyle,
            currentHudStyle = state.playerPreferences.volumeBrightnessBarStyle,
            currentPlayerStyle = state.playerPreferences.playerControlStyle,
            onSelectThemeMode = { mode ->
                onAction(
                    MoreAction.UpdateAppPreferences {
                        it.copy(
                            mxAppThemeMode = mode,
                            themeConfig = if (mode == MxAppThemeMode.LIGHT_THEME) ThemeConfig.OFF else ThemeConfig.ON,
                            useHighContrastDarkTheme = mode != MxAppThemeMode.LIGHT_THEME,
                        )
                    },
                )
            },
            onSelectDynamicAccent = { useDynamic ->
                onAction(
                    MoreAction.UpdateAppPreferences {
                        it.copy(
                            useDynamicColors = useDynamic,
                            mxClassicThemeHex = "#7C3AED",
                        )
                    },
                )
            },
            onSelectNavBarStyle = { navStyle ->
                onAction(MoreAction.UpdateAppPreferences { it.copy(navBarStyle = navStyle) })
            },
            onSelectSwitchStyle = { swStyle ->
                onAction(MoreAction.UpdateAppPreferences { it.copy(switchStyle = swStyle) })
            },
            onSelectHudStyle = { hudStyle ->
                onAction(MoreAction.UpdatePlayerPreferences { it.copy(volumeBrightnessBarStyle = hudStyle) })
            },
            onSelectPlayerStyle = { pStyle ->
                onAction(MoreAction.UpdatePlayerPreferences { it.copy(playerControlStyle = pStyle) })
            },
            onDismiss = { showUiSwitcherPopup = false },
        )
    }

    if (showNetworkStreamDialog) {
        var streamUrl by rememberSaveable { mutableStateOf("") }
        NextDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            title = { Text("Network Stream") },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter network stream URL (HTTPS, HTTP, M3U8, DASH, RTSP):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NextOutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("https://example.com/live.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = streamUrl.isNotBlank(),
                    onClick = {
                        val trimmed = streamUrl.trim()
                        if (trimmed.isNotEmpty()) {
                            showNetworkStreamDialog = false
                            onAction(MoreAction.PlayVideo(trimmed))
                        }
                    },
                ) {
                    Text("Play")
                }
            },
            dismissButton = {
                CancelButton(onClick = { showNetworkStreamDialog = false })
            },
        )
    }

    if (showStatusSaverDialog) {
        NextDialog(
            onDismissRequest = { showStatusSaverDialog = false },
            title = { Text("Recent Media Clips") },
            content = {
                val recentVideos = state.history.result.orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recentVideos.isEmpty()) {
                        Text(
                            text = "No recent clips played yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        recentVideos.take(5).forEach { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showStatusSaverDialog = false
                                        onAction(MoreAction.PlayVideo(video.uriString))
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = NextIcons.Video,
                                    contentDescription = null,
                                    tint = accent,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = video.displayName,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusSaverDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {},
        )
    }

    if (showLegalDialog) {
        NextDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text("Open Source Notices") },
            content = {
                Text(
                    text = "Nova Player is built with Kotlin, Jetpack Compose, AndroidX Media3, and FFmpeg hardware/software decoding under GPLv3 / Apache 2.0 open-source licenses.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) { Text("OK") }
            },
            dismissButton = {},
        )
    }

    if (showHelpDialog) {
        NextDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Nova Player Gestures") },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Double-Tap Left / Right: Instant -10s / +10s Skip (tap repeatedly for 20s, 30s...)")
                    Text("• Horizontal Drag: Axis-locked frame-accurate video scrubbing")
                    Text("• Left Edge Vertical Swipe: Screen Brightness (0–100%)")
                    Text("• Right Edge Vertical Swipe: Audio Volume (0–200% Boost)")
                    Text("• Long Press Video: 2.0x Fast-Forward Speed")
                    Text("• Pinch Two Fingers: Video Zoom & Pan (50%–400%)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got It") }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun NovaHubGridItem(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                view.novaHaptic(10L, 120, hapticsEnabled)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun NovaHubMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accent = LocalNovaAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.novaHaptic(10L, 120, hapticsEnabled)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistorySection(
    history: DataState<List<Video>>,
    preferences: ApplicationPreferences,
    onMoreClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
) {
    val videos = history.result.orEmpty().take(n = 10)
    val isTv = LocalContext.current.isTelevision
    val historyButtonFocusRequester = remember { FocusRequester() }
    val historyFocusState = rememberRestorableFocusState()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().thenIf(condition = isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp).weight(weight = 1f),
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .focusRequester(focusRequester = historyButtonFocusRequester)
                    .focusProperties {
                        if (isTv && !preferences.isHistoryPaused && videos.isNotEmpty()) {
                            down = historyFocusState.requester
                        }
                    }
                    .tvFocusRing(),
            ) {
                Icon(
                    imageVector = NextIcons.ArrowForward,
                    contentDescription = stringResource(R.string.history),
                )
            }
        }
        when {
            preferences.isHistoryPaused -> HistoryEmptyState(
                isHistoryOff = true,
                modifier = Modifier.fillMaxWidth(),
            )

            history is DataState.Success && videos.isEmpty() -> HistoryEmptyState(
                isHistoryOff = false,
                modifier = Modifier.fillMaxWidth(),
            )

            else -> {
                LazyRow(
                    modifier = Modifier
                        .focusProperties {
                            onExit = {
                                if (isTv && requestedFocusDirection == FocusDirection.Up) {
                                    historyButtonFocusRequester.requestFocus()
                                }
                            }
                        }
                        .restorableFocusGroup(
                            state = historyFocusState,
                            ready = false,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = videos,
                        key = { it.uriString },
                    ) { video ->
                        VideoGridItem(
                            video = video,
                            isRecentlyPlayedVideo = false,
                            textStyle = MaterialTheme.typography.bodySmall,
                            preferences = preferences,
                            modifier = Modifier
                                .width(140.dp)
                                .restorableFocusItem(
                                    state = historyFocusState,
                                    video.uriString,
                                ),
                            onClick = { onVideoClick(video) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MoreScreenContentPreview() {
    MoreScreenContent(
        state = MoreUiState(
            history = DataState.Success(value = emptyList()),
        ),
        onAction = {},
    )
}
