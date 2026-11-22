package com.mayank.darkplayer.feature.more.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import com.mayank.darkplayer.core.ui.theme.DarkPlayerAppearanceDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerLanguageDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerStorageDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mayank.darkplayer.core.common.extensions.isTelevision
import com.mayank.darkplayer.core.media.services.MediaOperationsService
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.Video
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.base.DataState
import com.mayank.darkplayer.core.ui.components.BindTopLevelFab
import com.mayank.darkplayer.core.ui.components.LocalNavigationBottomPadding
import com.mayank.darkplayer.core.ui.components.NextTopAppBar
import com.mayank.darkplayer.core.ui.components.TopLevelFabKey
import com.mayank.darkplayer.core.ui.components.rememberRestorableFocusState
import com.mayank.darkplayer.core.ui.components.restorableFocusGroup
import com.mayank.darkplayer.core.ui.components.restorableFocusItem
import com.mayank.darkplayer.core.ui.components.thenIf
import com.mayank.darkplayer.core.ui.components.tvFocusRing
import com.mayank.darkplayer.core.ui.components.tvListFocus
import com.mayank.darkplayer.core.ui.designsystem.NextIcons
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.feature.more.components.HistoryEmptyState
import com.mayank.darkplayer.feature.videopicker.composables.VideoGridItem

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
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.more),
                fontWeight = FontWeight.Bold,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .clip(
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                    ),
                )
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .tvListFocus()
                    .padding(horizontal = 8.dp)
                    .padding(
                        top = 8.dp,
                        bottom =
                            scaffoldPadding.calculateBottomPadding() +
                                LocalNavigationBottomPadding.current + 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        onClick = { onAction(MoreAction.OpenVault) },
                    ) {
                        Icon(
                            imageVector = NextIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.vault))
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        onClick = { filePicker.launch(input = "video/*") },
                    ) {
                        Icon(
                            imageVector = NextIcons.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.pick_file))
                    }
                    if (MediaOperationsService.supportsTrash()) {
                        FilledTonalButton(
                            modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                            onClick = { onAction(MoreAction.OpenTrash) },
                        ) {
                            Icon(
                                imageVector = NextIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(R.string.trash))
                        }
                        Spacer(modifier = Modifier.weight(weight = 1f))
                    }
                }
                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
                MoreStudioCard(
                    onOpenAppearance = { showAppearanceDialog = true },
                    onOpenLanguage = { showLanguageDialog = true },
                    onOpenStorage = { showStorageDialog = true },
                    onOpenSettings = { onAction(MoreAction.OpenSettings) },
                )
            }
        }
        if (showAppearanceDialog) DarkPlayerAppearanceDialog(onDismiss = { showAppearanceDialog = false })
        if (showLanguageDialog) DarkPlayerLanguageDialog(onDismiss = { showLanguageDialog = false })
        if (showStorageDialog) DarkPlayerStorageDialog(onDismiss = { showStorageDialog = false })
    }
}

@Composable
private fun MoreStudioCard(
    onOpenAppearance: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Dark Player Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(22.dp)),
        ) {
            MoreStudioRow(
                icon = Icons.Rounded.Palette,
                title = "Appearance & Theme",
                subtitle = "${DarkPlayerUiPrefs.themeMode.label} • ${DarkPlayerUiPrefs.palette.label}",
                onClick = onOpenAppearance,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Translate,
                title = "Language",
                subtitle = "Change app language & translation",
                onClick = onOpenLanguage,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Storage,
                title = "Storage Usage",
                subtitle = "Device storage & clear Dark Player cache",
                onClick = onOpenStorage,
            )
            HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))
            MoreStudioRow(
                icon = Icons.Rounded.Settings,
                title = "All Settings",
                subtitle = "Player, gestures, decoders, subtitles & about",
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun MoreStudioRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
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
            // Keep the full header in the vertical focus path, including its empty title area.
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
                    // The screen owns initial focus; this region restores a video only when entered.
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
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
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
