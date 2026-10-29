package dev.xboyrex.darkplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.common.service.system.SystemService
import dev.xboyrex.darkplayer.core.media.services.MediaOperationsService
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.darkplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import dev.xboyrex.darkplayer.core.ui.components.LocalTopLevelFabSetter
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabState
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.navigation.GlassQuality
import dev.xboyrex.darkplayer.navigation.NextNavigationBar
import dev.xboyrex.darkplayer.navigation.NextNavigationRail
import dev.xboyrex.darkplayer.navigation.TopLevelDestination
import dev.xboyrex.darkplayer.navigation.TopLevelNavState
import dev.xboyrex.darkplayer.navigation.YtxEmphasizedEasing
import dev.xboyrex.darkplayer.navigation.mediaNavGraph
import dev.xboyrex.darkplayer.navigation.moreNavGraph
import dev.xboyrex.darkplayer.navigation.navigationTransition
import dev.xboyrex.darkplayer.navigation.networkNavGraph
import dev.xboyrex.darkplayer.navigation.openPlaylist
import dev.xboyrex.darkplayer.navigation.playlistNavGraph
import dev.xboyrex.darkplayer.navigation.rememberTopLevelNavState
import dev.xboyrex.darkplayer.navigation.settingsNavGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mediaOperationsService: MediaOperationsService by inject()

    private val systemService: SystemService by inject()

    private val viewModel: MainViewModel by viewModel()

    private var pendingPlaylistId by mutableStateOf<Long?>(value = null)

    private fun enableHighRefreshRate() {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val display = this.display ?: return
                val bestMode = display.supportedModes.maxWithOrNull(
                    compareBy<android.view.Display.Mode> { it.refreshRate }
                        .thenBy { it.physicalWidth * it.physicalHeight },
                )
                if (bestMode != null && bestMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = bestMode.modeId
                    window.attributes = params
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlassQuality.init(this)
        enableHighRefreshRate()
        if (savedInstanceState == null) {
            pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
        }
        systemService.initialize(activity = this@MainActivity)
        mediaOperationsService.initialize(activity = this@MainActivity)

        installSplashScreen().setKeepOnScreenCondition {
            viewModel.state.value is MainActivityUiState.Loading
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(state = state)
            LaunchedEffect(state) {
                val currentState = state
                if (currentState is MainActivityUiState.Success) {
                    GlassQuality.userEnabled = currentState.preferences.useLiquidGlass
                }
            }

            LaunchedEffect(key1 = shouldUseDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                )
            }

            NextPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(state = state),
                dynamicColor = shouldUseDynamicTheming(state = state),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
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
                                Box(
                                    Modifier.thenIf(
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
    val bgColor = MaterialTheme.colorScheme.background
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }
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

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarPadding = if (showNavigation && showBottomBar && !showNavRail) {
        navBottomInset + 84.dp
    } else {
        0.dp
    }

    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { launched = true }
    val launchAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (launched) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = YtxEmphasizedEasing),
        label = "launchAlpha",
    )
    val launchScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (launched) 1f else 0.96f,
        animationSpec = tween(durationMillis = 420, easing = YtxEmphasizedEasing),
        label = "launchScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = launchAlpha
                scaleX = launchScale
                scaleY = launchScale
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .thenIf(condition = isTv) {
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
                    bottom = floatingBarPadding,
                ),
            )
        }

        AnimatedVisibility(
            visible = showNavigation && showBottomBar,
            enter = slideInVertically(tween(260, easing = YtxEmphasizedEasing)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(200, easing = YtxEmphasizedEasing)) { it } + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            NextNavigationBar(
                state = state,
                fabState = displayedFabState,
                contentFocusRequester = contentFocusRequester,
                fabFocusRequester = fabFocusRequester,
                showFabOnly = showNavRail,
                backdrop = pageBackdrop,
            )
        }

        AnimatedVisibility(
            visible = showNavRail && showNavigation,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            NextNavigationRail(
                state = state,
                modifier = Modifier.onSizeChanged {
                    railWidth =
                        with(receiver = density) { it.width.toDp() }
                },
            )
        }
    }
}

@Composable
fun shouldUseDarkTheme(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> true

    is MainActivityUiState.Success -> when (state.preferences.themeConfig) {
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> true
    is MainActivityUiState.Success -> state.preferences.useHighContrastDarkTheme
}

@Composable
fun shouldUseDynamicTheming(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
