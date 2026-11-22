package com.mayank.darkplayer

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mayank.darkplayer.core.common.di.DiQualifiers
import com.mayank.darkplayer.core.data.repository.PreferencesRepository
import com.mayank.darkplayer.core.media.network.datasource.NextDataSourceFactory
import com.mayank.darkplayer.core.media.services.MediaOperationsService
import com.mayank.darkplayer.core.media.services.MediaService
import com.mayank.darkplayer.core.media.sync.MediaSynchronizer
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.PlayerPreferences
import com.mayank.darkplayer.core.model.SearchHistory
import com.mayank.darkplayer.feature.more.screens.history.HistoryViewModel
import com.mayank.darkplayer.feature.more.screens.more.MoreViewModel
import com.mayank.darkplayer.feature.more.screens.trash.TrashViewModel
import com.mayank.darkplayer.feature.network.screens.addconnection.AddConnectionViewModel
import com.mayank.darkplayer.feature.network.screens.browse.NetworkBrowseViewModel
import com.mayank.darkplayer.feature.network.screens.list.NetworkViewModel
import com.mayank.darkplayer.feature.player.PlayerViewModel
import com.mayank.darkplayer.feature.playlist.screens.detail.PlaylistDetailViewModel
import com.mayank.darkplayer.feature.playlist.screens.list.PlaylistListViewModel
import com.mayank.darkplayer.feature.videopicker.screens.mediapicker.MediaPickerViewModel
import com.mayank.darkplayer.feature.videopicker.screens.search.SearchViewModel
import com.mayank.darkplayer.feature.videopicker.screens.vault.VaultViewModel
import com.mayank.darkplayer.settings.screens.about.AboutPreferencesViewModel
import com.mayank.darkplayer.settings.screens.about.LibrariesViewModel
import com.mayank.darkplayer.settings.screens.appearance.AppearancePreferencesViewModel
import com.mayank.darkplayer.settings.screens.audio.AudioPreferencesViewModel
import com.mayank.darkplayer.settings.screens.general.GeneralPreferencesViewModel
import com.mayank.darkplayer.settings.screens.gesture.GesturePreferencesViewModel
import com.mayank.darkplayer.settings.screens.medialibrary.FolderPreferencesViewModel
import com.mayank.darkplayer.settings.screens.medialibrary.MediaLibraryPreferencesViewModel
import com.mayank.darkplayer.settings.screens.player.PlayerPreferencesViewModel
import com.mayank.darkplayer.settings.screens.subtitle.SubtitlePreferencesViewModel
import com.mayank.darkplayer.settings.screens.thumbnail.ThumbnailPreferencesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.viewmodel.resolveViewModel

@RunWith(AndroidJUnit4::class)
class KoinGraphTest {
    private val koin get() = GlobalContext.get()

    @Test
    fun sharedServicesAndAllThreePreferenceStoresResolve() {
        runBlocking {
            assertSame(
                koin.get<PreferencesRepository>(),
                koin.get<PreferencesRepository>(),
            )
            assertSame(
                koin.get<MediaOperationsService>(),
                koin.get<MediaOperationsService>(),
            )
            assertSame(
                koin.get<MediaService>(),
                koin.get<MediaService>(),
            )
            assertSame(
                koin.get<MediaSynchronizer>(),
                koin.get<MediaSynchronizer>(),
            )
            assertSame(
                koin.get<NextDataSourceFactory>(),
                koin.get<NextDataSourceFactory>(),
            )
            withTimeout(timeMillis = 10_000) {
                // Read the typed values to catch collisions between erased DataStore keys.
                koin.get<DataStore<ApplicationPreferences>>(
                    qualifier = named(DiQualifiers.APP_PREFERENCES),
                ).data.first().mediaViewMode
                koin.get<DataStore<PlayerPreferences>>(
                    qualifier = named(DiQualifiers.PLAYER_PREFERENCES),
                ).data.first().resume
                koin.get<DataStore<SearchHistory>>(
                    qualifier = named(DiQualifiers.SEARCH_HISTORY),
                ).data.first().queries.size
            }
        }
    }

    @Test
    fun viewModelsResolveWithRuntimeParametersAndRespectTheirStore() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = ViewModelStore()
            val secondStore = ViewModelStore()
            try {
                viewModel<MainViewModel>(store = store)
                viewModel<PlayerViewModel>(
                    store = store,
                    PlayerViewModel.Output(
                        navigateUp = {},
                        selectSubtitle = {},
                        selectAudio = {},
                        playInBackground = {},
                        setVideoDecoderMode = {},
                        setAudioDecoderMode = {},
                        tryDecoderFallback = {},
                    ),
                )
                viewModel<HistoryViewModel>(
                    store = store,
                    HistoryViewModel.Output(
                        navigateUp = {},
                        playVideo = {},
                    ),
                )
                viewModel<MoreViewModel>(
                    store = store,
                    MoreViewModel.Output(
                        openHistory = {},
                        playVideo = {},
                        openSettings = {},
                        openTrash = {},
                        openVault = {},
                    ),
                )
                viewModel<TrashViewModel>(
                    store = store,
                    TrashViewModel.Output(
                        navigateUp = {},
                        playVideo = {},
                    ),
                )
                viewModel<NetworkViewModel>(
                    store = store,
                    NetworkViewModel.Output(
                        addConnection = {},
                        editConnection = {},
                        openConnection = {},
                        openSettings = {},
                        openStream = {},
                    ),
                )
                viewModel<AddConnectionViewModel>(
                    store = store,
                    AddConnectionViewModel.Input(connectionId = null),
                    AddConnectionViewModel.Output(navigateUp = {}),
                )
                viewModel<NetworkBrowseViewModel>(
                    store = store,
                    NetworkBrowseViewModel.Input(
                        connectionId = -1,
                        path = "/",
                    ),
                    NetworkBrowseViewModel.Output(
                        navigateUp = {},
                        playVideos = { _, _ -> },
                        openFolder = { _, _ -> },
                    ),
                )
                viewModel<PlaylistListViewModel>(
                    store = store,
                    PlaylistListViewModel.Output(
                        openPlaylist = {},
                        openSettings = {},
                    ),
                )
                viewModel<PlaylistDetailViewModel>(
                    store = store,
                    PlaylistDetailViewModel.Input(playlistId = -1),
                    PlaylistDetailViewModel.Output(
                        navigateUp = {},
                        playPlaylist = { _, _ -> },
                    ),
                )
                viewModel<SearchViewModel>(
                    store = store,
                    SearchViewModel.Output(
                        navigateUp = {},
                        playVideo = {},
                        openFolder = {},
                    ),
                )
                viewModel<VaultViewModel>(
                    store = store,
                    VaultViewModel.Output(
                        navigateUp = {},
                        playVideo = {},
                        playVideos = {},
                    ),
                )
                viewModel<AboutPreferencesViewModel>(
                    store = store,
                    AboutPreferencesViewModel.Output(
                        navigateUp = {},
                        openLibraries = {},
                    ),
                )
                viewModel<LibrariesViewModel>(
                    store = store,
                    LibrariesViewModel.Output(navigateUp = {}),
                )
                viewModel<AppearancePreferencesViewModel>(
                    store = store,
                    AppearancePreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<AudioPreferencesViewModel>(
                    store = store,
                    AudioPreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<GeneralPreferencesViewModel>(
                    store = store,
                    GeneralPreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<GesturePreferencesViewModel>(
                    store = store,
                    GesturePreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<FolderPreferencesViewModel>(
                    store = store,
                    FolderPreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<MediaLibraryPreferencesViewModel>(
                    store = store,
                    MediaLibraryPreferencesViewModel.Output(
                        navigateUp = {},
                        openFolders = {},
                        openThumbnails = {},
                    ),
                )
                viewModel<PlayerPreferencesViewModel>(
                    store = store,
                    PlayerPreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<SubtitlePreferencesViewModel>(
                    store = store,
                    SubtitlePreferencesViewModel.Output(navigateUp = {}),
                )
                viewModel<ThumbnailPreferencesViewModel>(
                    store = store,
                    ThumbnailPreferencesViewModel.Output(navigateUp = {}),
                )

                val output = MediaPickerViewModel.Output(
                    navigateUp = {},
                    playVideo = {},
                    playVideos = {},
                    openFolder = {},
                    openSettings = {},
                    openSearch = {},
                    openVault = {},
                )
                val first = viewModel<MediaPickerViewModel>(
                    store = store,
                    MediaPickerViewModel.Input(folderId = "/first"),
                    output,
                )
                assertEquals(
                    "/first",
                    first.folderPath,
                )
                assertSame(
                    first,
                    viewModel<MediaPickerViewModel>(
                        store = store,
                        MediaPickerViewModel.Input(folderId = "/ignored"),
                        output,
                    ),
                )
                val second = viewModel<MediaPickerViewModel>(
                    store = secondStore,
                    MediaPickerViewModel.Input(folderId = "/second"),
                    output,
                )
                assertNotSame(
                    first,
                    second,
                )
                assertEquals(
                    "/second",
                    second.folderPath,
                )
            } finally {
                store.clear()
                secondStore.clear()
            }
        }
    }

    @OptIn(KoinInternalApi::class)
    private inline fun <reified T : ViewModel> viewModel(
        store: ViewModelStore,
        vararg parameters: Any,
    ): T = resolveViewModel(
        vmClass = T::class,
        viewModelStore = store,
        extras = CreationExtras.Empty,
        scope = koin.scopeRegistry.rootScope,
        parameters = { parametersOf(*parameters) },
    )
}
