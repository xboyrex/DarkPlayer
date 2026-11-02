package dev.xboyrex.darkplayer.feature.more.screens.more

import android.content.Intent
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
import androidx.compose.runtime.remember
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
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.media.services.MediaOperationsService
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.base.DataState
import dev.xboyrex.darkplayer.core.ui.components.BindTopLevelFab
import dev.xboyrex.darkplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabKey
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.feature.more.components.HistoryEmptyState
import dev.xboyrex.darkplayer.feature.videopicker.composables.VideoGridItem

@Composable
fun MoreScreen(
    viewModel: MoreViewModel,
    directOutput: MoreViewModel.Output? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = { action ->
            if (directOutput != null) {
                when (action) {
                    is MoreAction.OpenHistory -> directOutput.openHistory()
                    is MoreAction.PlayVideo -> directOutput.playVideo(action.uri)
                    is MoreAction.OpenSettings -> directOutput.openSettings()
                    is MoreAction.OpenTrash -> directOutput.openTrash()
                    is MoreAction.OpenVault -> directOutput.openVault()
                }
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    val context = LocalContext.current
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { pickedUri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        pickedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                onAction(MoreAction.PlayVideo(pickedUri.toString()))
            }
        }
    val fallbackGetContentLauncher =
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
                        onClick = {
                            runCatching {
                                openDocumentLauncher.launch(arrayOf("video/*", "audio/*", "application/x-matroska", "application/ogg"))
                            }.onFailure {
                                fallbackGetContentLauncher.launch("video/*")
                            }
                        },
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
            }
        }
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
