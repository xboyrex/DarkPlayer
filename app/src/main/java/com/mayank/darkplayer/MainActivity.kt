package com.mayank.darkplayer

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mayank.darkplayer.core.common.extensions.isTelevision
import com.mayank.darkplayer.core.common.service.system.SystemService
import com.mayank.darkplayer.core.media.services.MediaOperationsService
import com.mayank.darkplayer.core.model.ThemeConfig
import com.mayank.darkplayer.core.ui.components.LocalNavigationBottomPadding
import com.mayank.darkplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import com.mayank.darkplayer.core.ui.components.LocalTopLevelFabSetter
import com.mayank.darkplayer.core.ui.components.TopLevelFabState
import com.mayank.darkplayer.core.ui.components.thenIf
import com.mayank.darkplayer.core.ui.theme.DarkPlayerTheme
import com.mayank.darkplayer.core.ui.theme.DarkPlayerThemeMode
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mayank.darkplayer.navigation.DarkNavigationBar
import com.mayank.darkplayer.navigation.LocalNavGlassBackdrop
import com.mayank.darkplayer.navigation.DarkNavigationRail
import com.mayank.darkplayer.navigation.TopLevelDestination
import com.mayank.darkplayer.navigation.TopLevelNavState
import com.mayank.darkplayer.navigation.mediaNavGraph
import com.mayank.darkplayer.navigation.moreNavGraph
import com.mayank.darkplayer.navigation.navigationTransition
import com.mayank.darkplayer.navigation.navigationTransitionModifier
import com.mayank.darkplayer.navigation.networkNavGraph
import com.mayank.darkplayer.navigation.openPlaylist
import com.mayank.darkplayer.navigation.playlistNavGraph
import com.mayank.darkplayer.navigation.rememberTopLevelNavState
import com.mayank.darkplayer.navigation.settingsNavGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mediaOperationsService: MediaOperationsService by inject()

    private val systemService: SystemService by inject()

    private val viewModel: MainViewModel by viewModel()

    private var pendingPlaylistId by mutableStateOf<Long?>(value = null)
    private var darkSystemBars = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
        }
        systemService.initialize(activity = this@MainActivity)
        mediaOperationsService.initialize(activity = this@MainActivity)

        installSplashScreen().setKeepOnScreenCondition {
            viewModel.state.value is MainActivityUiState.Loading
        }

        darkSystemBars = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
                detectDarkMode = { darkSystemBars },
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
                detectDarkMode = { darkSystemBars },
            ),
        )

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(state = state)

            LaunchedEffect(key1 = shouldUseDarkTheme, key2 = DarkPlayerUiPrefs.themeMode) {
                darkSystemBars = shouldUseDarkTheme
                WindowCompat.getInsetsController(
                    window,
                    window.decorView,
                ).apply {
                    DarkPlayerUiPrefs.ensureLoaded(this@MainActivity)
                    val effectiveDarkBars = when (DarkPlayerUiPrefs.themeMode) {
                        DarkPlayerThemeMode.SYSTEM -> shouldUseDarkTheme
                        DarkPlayerThemeMode.DARK, DarkPlayerThemeMode.AMOLED -> true
                        DarkPlayerThemeMode.LIGHT -> false
                    }
                    isAppearanceLightStatusBars = !effectiveDarkBars
                    isAppearanceLightNavigationBars = !effectiveDarkBars
                }
            }

            DarkPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(state = state),
                dynamicColor = shouldUseDynamicTheming(state = state),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    val navState = rememberTopLevelNavState()

                    LaunchedEffect(key1 = pendingPlaylistId) {
                        pendingPlaylistId?.let { playlistId ->
                            navState.openPlaylist(playlistId = playlistId)
                            pendingPlaylistId = null
                        }
                    }

                    val mediaStack = navState.backStacks.getValue(TopLevelDestination.MEDIA.route)
                    val playlistStack = navState.backStacks.getValue(
                        TopLevelDestination.PLAYLISTS.route,
                    )
                    val networkStack = navState.backStacks.getValue(
                        TopLevelDestination.NETWORK.route,
                    )
                    val moreStack = navState.backStacks.getValue(TopLevelDestination.MORE.route)

                    val provider = entryProvider {
                        mediaNavGraph(
                            context = this@MainActivity,
                            backStack = mediaStack,
                        )
                        playlistNavGraph(
                            context = this@MainActivity,
                            backStack = playlistStack,
                        )
                        networkNavGraph(
                            context = this@MainActivity,
                            backStack = networkStack,
                        )
                        moreNavGraph(
                            context = this@MainActivity,
                            backStack = moreStack,
                        )
                        settingsNavGraph(backStack = navState.currentStack)
                    }

                    val topLevelFabStates =
                        remember { mutableStateMapOf<String, TopLevelFabState>() }
                    var showTopLevelBottomBar by remember { mutableStateOf(value = true) }
                    NavigationLayout(
                        state = navState,
                        fabStates = topLevelFabStates,
                        showBottomBar = showTopLevelBottomBar,
                    ) { layoutPaddingValues ->
                        val railPadding = layoutPaddingValues.calculateStartPadding(
                            LocalLayoutDirection.current,
                        )
                        val navigationInsetsDecorator = remember(
                            navState,
                            railPadding,
                        ) {
                            NavEntryDecorator<NavKey> { entry ->
                                // Reserve rail space per entry without resizing the animated display.
                                Box(
                                    navState.navigationTransitionModifier(entry).thenIf(
                                        navState.topLevelContentKeys.contains(entry.contentKey),
                                    ) {
                                        padding(start = railPadding)
                                            .consumeWindowInsets(
                                                WindowInsets.displayCutout.only(
                                                    WindowInsetsSides.Start,
                                                ),
                                            )
                                    },
                                ) { entry.Content() }
                            }
                        }
                        CompositionLocalProvider(
                            LocalNavigationBottomPadding provides
                                layoutPaddingValues.calculateBottomPadding(),
                            LocalTopLevelBottomBarVisibleSetter provides
                                { showTopLevelBottomBar = it },
                            LocalTopLevelFabSetter provides { key, state ->
                                if (state == null) {
                                    topLevelFabStates.remove(key = key)
                                } else {
                                    topLevelFabStates[key] = state
                                }
                            },
                        ) {
                            NavDisplay(
                                entries = navState.rememberEntries(
                                    entryProvider = provider,
                                    entryDecorator = navigationInsetsDecorator,
                                ),
                                onBack = { navState.goBack() },
                                transitionSpec = {
                                    navState.navigationTransition(
                                        initialState = initialState,
                                        targetState = targetState,
                                    )
                                },
                                popTransitionSpec = {
                                    navState.navigationTransition(
                                        initialState = initialState,
                                        targetState = targetState,
                                    )
                                },
                                predictivePopTransitionSpec = {
                                    navState.navigationTransition(
                                        initialState = initialState,
                                        targetState = targetState,
                                        isPredictive = true,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationLayout(
    modifier: Modifier = Modifier,
    state: TopLevelNavState,
    fabStates: SnapshotStateMap<String, TopLevelFabState>,
    showBottomBar: Boolean,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isTv = LocalContext.current.isTelevision
    val density = LocalDensity.current
    var railWidth by remember(density) { mutableStateOf(0.dp) }
    val contentFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val showNavRail = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val showNavigation =
        state.currentStack.lastOrNull()?.let { state.topLevelContentKeys.contains(element = it) } ==
            true
    val selectedFabState = state.destinations[state.selectedIndex].fabKey?.let(
        block = fabStates::get,
    )
    var displayedFabState by remember { mutableStateOf<TopLevelFabState?>(value = null) }
    LaunchedEffect(key1 = selectedFabState) { selectedFabState?.let { displayedFabState = it } }

    val modalBlurRadius by animateDpAsState(
        targetValue = if (DarkPlayerUiPrefs.activeModalBlurCount > 0) 18.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
        label = "modal_bg_blur",
    )
    val navGlassBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalNavGlassBackdrop provides navGlassBackdrop) {
    Box(modifier = Modifier.fillMaxSize().blur(radius = modalBlurRadius)) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                AnimatedVisibility(
                    visible = showNavigation && showBottomBar,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    DarkNavigationBar(
                        state = state,
                        fabState = displayedFabState,
                        contentFocusRequester = contentFocusRequester,
                        fabFocusRequester = fabFocusRequester,
                        showFabOnly = showNavRail,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = WindowInsets(0.dp),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layerBackdrop(navGlassBackdrop)
                        .thenIf(condition = isTv) {
                            // The FAB sits outside screen content, whose full-height focus groups
                            // overlap it. Enter the content explicitly instead of searching the rail.
                            focusRequester(focusRequester = contentFocusRequester)
                                .focusProperties {
                                    onExit = {
                                        if (requestedFocusDirection == FocusDirection.Down &&
                                            showNavigation && showBottomBar &&
                                            displayedFabState != null
                                        ) {
                                            fabFocusRequester.requestFocus()
                                        }
                                    }
                                }
                                .focusGroup()
                        },
                ) {
                    content(
                        PaddingValues(
                            start = if (showNavRail) railWidth else 0.dp,
                            bottom = it.calculateBottomPadding(),
                        ),
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = showNavRail && showNavigation,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            DarkNavigationRail(
                state = state,
                modifier = Modifier.onSizeChanged {
                    railWidth =
                        with(receiver = density) { it.width.toDp() }
                },
            )
        }
    }
    }
}

/**
 * Returns `true` if dark theme should be used, as a function of the [state] and the
 * current system context.
 */
@Composable
fun shouldUseDarkTheme(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> isSystemInDarkTheme()

    is MainActivityUiState.Success -> when (state.preferences.themeConfig) {
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useHighContrastDarkTheme
}

/**
 * Returns `true` if the dynamic color is disabled, as a function of the [state].
 */
@Composable
fun shouldUseDynamicTheming(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
