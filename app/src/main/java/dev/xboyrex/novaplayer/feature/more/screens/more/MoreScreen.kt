package dev.xboyrex.novaplayer.feature.more.screens.more

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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.novaplayer.core.common.extensions.isTelevision
import dev.xboyrex.novaplayer.core.media.services.MediaOperationsService
import dev.xboyrex.novaplayer.core.model.ApplicationPreferences
import dev.xboyrex.novaplayer.core.model.Video
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.base.DataState
import dev.xboyrex.novaplayer.core.ui.components.BindTopLevelFab
import dev.xboyrex.novaplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.novaplayer.core.ui.components.NovaTopAppBar
import dev.xboyrex.novaplayer.core.ui.components.TopLevelFabKey
import dev.xboyrex.novaplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.novaplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.novaplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.novaplayer.core.ui.components.thenIf
import dev.xboyrex.novaplayer.core.ui.components.tvFocusRing
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyAboutDialog
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyController
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyPresetsData
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyStudioDialog
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyVectorLogo
import dev.xboyrex.novaplayer.core.ui.extensions.copy
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import dev.xboyrex.novaplayer.feature.more.components.HistoryEmptyState
import dev.xboyrex.novaplayer.feature.videopicker.composables.VideoGridItem

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
        NovaIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }
    var showDolbyStudio by rememberSaveable { mutableStateOf(false) }
    var showDolbyAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NovaTopAppBar(
                title = stringResource(R.string.more),
                fontWeight = FontWeight.Bold,
                actions = {
                    IconButton(onClick = { showDolbyAbout = true }) {
                        Icon(
                            imageVector = NovaIcons.Info,
                            contentDescription = "About Developer",
                        )
                    }
                    IconButton(onClick = { onAction(MoreAction.OpenSettings) }) {
                        Icon(
                            imageVector = NovaIcons.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
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
                    .padding(horizontal = 12.dp)
                    .padding(
                        top = 12.dp,
                        bottom =
                            scaffoldPadding.calculateBottomPadding() +
                                LocalNavigationBottomPadding.current + 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Nova Dolby Atmos Hero Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .novaPressable { showDolbyStudio = true },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                        ) {
                            NovaDolbyVectorLogo(
                                modifier = Modifier.padding(12.dp),
                                leftColor = MaterialTheme.colorScheme.onPrimary,
                                rightColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dolby Atmos & Equalizer",
                                fontFamily = NovaRoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            )
                            val prof = NovaDolbyPresetsData.PROFILES.find { it.id == NovaDolbyController.currentProfile }?.name ?: "Movie"
                            Text(
                                text = if (NovaDolbyController.enabled) {
                                    "Active · $prof · ${NovaDolbyController.currentPresetName} (${NovaDolbyController.bandMode.displayName})"
                                } else {
                                    "Tap to configure 20-band EQ, bass & surround virtualizer"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                            )
                        }
                        Icon(
                            imageVector = NovaIcons.ArrowForward,
                            contentDescription = null,
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        onClick = { onAction(MoreAction.OpenVault) },
                    ) {
                        Icon(
                            imageVector = NovaIcons.Lock,
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
                            imageVector = NovaIcons.FileOpen,
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
                                imageVector = NovaIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(R.string.trash))
                        }
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        onClick = { onAction(MoreAction.OpenSettings) },
                    ) {
                        Icon(
                            imageVector = NovaIcons.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.settings))
                    }
                }
                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
            }
        }
    }

    if (showDolbyStudio) {
        NovaDolbyStudioDialog(onDismiss = { showDolbyStudio = false })
    }
    if (showDolbyAbout) {
        NovaDolbyAboutDialog(onDismiss = { showDolbyAbout = false })
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
                    imageVector = NovaIcons.ArrowForward,
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
