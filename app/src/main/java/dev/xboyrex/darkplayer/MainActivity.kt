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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import dev.xboyrex.darkplayer.core.media.sync.MediaSynchronizer
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.darkplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import dev.xboyrex.darkplayer.core.ui.components.LocalTopLevelFabSetter
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabState
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import dev.xboyrex.darkplayer.core.ui.theme.DarkPlayerTheme
import dev.xboyrex.darkplayer.core.ui.theme.ThemeState
import dev.xboyrex.darkplayer.core.ui.updater.UpdateOverlayDialog
import dev.xboyrex.darkplayer.core.ui.updater.rememberUpdateState
import dev.xboyrex.darkplayer.feature.playlist.navigation.PlaylistDetailRoute
import dev.xboyrex.darkplayer.navigation.NextNavigationBar
import dev.xboyrex.darkplayer.navigation.NextNavigationRail
import dev.xboyrex.darkplayer.navigation.TopLevelDestination
import dev.xboyrex.darkplayer.navigation.TopLevelNavState
import dev.xboyrex.darkplayer.navigation.ensureRoot
import dev.xboyrex.darkplayer.navigation.mediaNavGraph
import dev.xboyrex.darkplayer.navigation.moreNavGraph
import dev.xboyrex.darkplayer.navigation.navigationTransition
import dev.xboyrex.darkplayer.navigation.networkNavGraph
import dev.xboyrex.darkplayer.navigation.playlistNavGraph
import dev.xboyrex.darkplayer.navigation.rememberTopLevelNavState
import dev.xboyrex.darkplayer.navigation.settingsNavGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val synchronizer: MediaSynchronizer by inject()
    private val mediaService: MediaOperationsService by inject()
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
        GlassSettings.init(this)
        GlassQuality.init(this)
        enableHighRefreshRate()
        if (savedInstanceState == null) {
            pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
        }
        synchronizer.startSync()
        mediaService.initialize(activity = this)
        systemService.initialize(activity = this)

        installSplashScreen()
        window.setBackgroundDrawableResource(android.R.color.black)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(state = state)
            val updateState = rememberUpdateState()

            LaunchedEffect(Unit) {
                updateState.check(context = this@MainActivity, manual = false)
            }

            LaunchedEffect(state) {
                val currentState = state
                if (currentState is MainActivityUiState.Success) {
                    GlassQuality.userEnabled = currentState.preferences.useLiquidGlass
                    ThemeState.accentSeed = currentState.preferences.themeAccentSeed
                    ThemeState.useSystemFont = currentState.preferences.useSystemFont
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

            DarkPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(state = state),
                dynamicColor = shouldUseDynamicTheming(state = state),
                accentSeed = (state as? MainActivityUiState.Success)?.preferences?.themeAccentSeed
                    ?: ThemeState.accentSeed,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val navState = rememberTopLevelNavState()

                        LaunchedEffect(key1 = pendingPlaylistId) {
                            val playlistId = pendingPlaylistId ?: return@LaunchedEffect
                            pendingPlaylistId = null
                            val playlistRoute = TopLevelDestination.PLAYLISTS.route
                            val playlistStack = navState.backStacks.getValue(playlistRoute)
                            playlistStack.clear()
                            playlistStack.ensureRoot(playlistRoute)
                            playlistStack.add(element = PlaylistDetailRoute(playlistId = playlistId))
                            navState.switchTo(route = playlistRoute)
                        }

                        val showNavigation by remember(navState) {
                            derivedStateOf { navState.currentStack.size <= 1 }
                        }
                        val layoutDirection = LocalLayoutDirection.current
                        val fabStates = remember { mutableStateMapOf<String, TopLevelFabState>() }
                        var showBottomBar by remember { mutableStateOf(value = true) }

                        CompositionLocalProvider(
                            LocalTopLevelFabSetter provides { key, fabState ->
                                if (fabState != null) {
                                    fabStates[key] = fabState
                                } else {
                                    fabStates.remove(key = key)
                                }
                            },
                            LocalTopLevelBottomBarVisibleSetter provides { visible ->
                                showBottomBar = visible
                            },
                        ) {
                            NavigationLayout(
                                state = navState,
                                fabStates = fabStates,
                                showNavigation = showNavigation,
                                showBottomBar = showBottomBar,
                                modifier = Modifier.popupBackgroundBlur(
                                    extraBlur = updateState.isPopupActive,
                                ),
                            ) { contentPadding ->
                                val railPadding = contentPadding.calculateStartPadding(
                                    layoutDirection = layoutDirection,
                                )
                                val bottomPadding = contentPadding.calculateBottomPadding()
                                val navEntryDecorator = remember(
                                    navState,
                                    railPadding,
                                ) {
                                    NavEntryDecorator<NavKey> { entry ->
                                        Box(
                                            Modifier.thenIf(
                                                navState.topLevelContentKeys.contains(entry.contentKey),
                                            ) {
                                                padding(start = railPadding)
                                            },
                                        ) {
                                            entry.Content()
                                        }
                                    }
                                }
                                CompositionLocalProvider(
                                    value = LocalNavigationBottomPadding provides bottomPadding,
                                ) {
                                    NavDisplay(
                                        entries = navState.rememberEntries(
                                            entryDecorator = navEntryDecorator,
                                            entryProvider = entryProvider {
                                                mediaNavGraph(
                                                    context = this@MainActivity,
                                                    backStackProvider = { navState.currentStack },
                                                )
                                                playlistNavGraph(
                                                    context = this@MainActivity,
                                                    backStackProvider = { navState.currentStack },
                                                )
                                                networkNavGraph(
                                                    context = this@MainActivity,
                                                    backStackProvider = { navState.currentStack },
                                                )
                                                moreNavGraph(
                                                    context = this@MainActivity,
                                                    backStackProvider = { navState.currentStack },
                                                )
                                                settingsNavGraph(
                                                    backStackProvider = { navState.currentStack },
                                                )
                                            },
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .consumeWindowInsets(
                                                PaddingValues(
                                                    start = railPadding,
                                                    bottom = bottomPadding,
                                                ),
                                            ),
                                    )
                                }
                            }
                        }

                        UpdateOverlayDialog(state = updateState)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationLayout(
    state: TopLevelNavState,
    fabStates: Map<String, TopLevelFabState>,
    showNavigation: Boolean,
    showBottomBar: Boolean,
    modifier: Modifier = Modifier,
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
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val showNavRail = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    ) || !adaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )
    val selectedFabKey = state.destinations.getOrNull(state.selectedIndex)?.fabKey
    val selectedFabState = selectedFabKey?.let { fabStates[it] }
    var displayedFabState by remember { mutableStateOf<TopLevelFabState?>(value = null) }
    LaunchedEffect(key1 = selectedFabState) { selectedFabState?.let { displayedFabState = it } }

    var isNavCompact by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedIndex) {
        isNavCompact = false
    }

    val navScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (consumed.y < -10f) {
                        isNavCompact = true
                    } else if (consumed.y > 10f || (available.y > 6f && consumed.y >= 0f)) {
                        isNavCompact = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarPadding = if (showNavigation && showBottomBar && !showNavRail) {
        navBottomInset + 84.dp
    } else {
        0.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(navScrollConnection),
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
            enter = slideInVertically(tween(220, easing = Motion.Emphasized)) { it } + fadeIn(tween(140)),
            exit = slideOutVertically(tween(180, easing = Motion.Emphasized)) { it } + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            NextNavigationBar(
                state = state,
                fabState = displayedFabState,
                contentFocusRequester = contentFocusRequester,
                fabFocusRequester = fabFocusRequester,
                showFabOnly = showNavRail,
                isCompact = isNavCompact,
                onExpandCompact = { isNavCompact = false },
                backdrop = pageBackdrop,
            )
        }

        AnimatedVisibility(
            visible = showNavRail && showNavigation,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            NextNavigationRail(
                state = state,
                modifier = Modifier
                    .onSizeChanged { railWidth = with(density) { it.width.toDp() } }
                    .focusProperties {
                        if (isTv) right = contentFocusRequester
                    }
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start)),
            )
        }
    }
}

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
    MainActivityUiState.Loading -> true
    is MainActivityUiState.Success -> state.preferences.useHighContrastDarkTheme
}

@Composable
fun shouldUseDynamicTheming(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
