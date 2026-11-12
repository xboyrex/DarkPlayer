package dev.xboyrex.novaplayer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.xboyrex.novaplayer.core.common.extensions.isTelevision
import dev.xboyrex.novaplayer.core.common.service.system.SystemService
import dev.xboyrex.novaplayer.core.media.services.MediaOperationsService
import dev.xboyrex.novaplayer.core.model.ThemeConfig
import dev.xboyrex.novaplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.novaplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import dev.xboyrex.novaplayer.core.ui.components.LocalTopLevelFabSetter
import dev.xboyrex.novaplayer.core.ui.components.TopLevelFabState
import dev.xboyrex.novaplayer.core.ui.components.thenIf
import dev.xboyrex.novaplayer.core.ui.glass.GlassQuality
import dev.xboyrex.novaplayer.core.ui.glass.GlassSettings
import dev.xboyrex.novaplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.novaplayer.core.ui.glass.NovaMotion
import dev.xboyrex.novaplayer.core.ui.theme.NovaPlayerTheme
import dev.xboyrex.novaplayer.core.ui.update.NovaUpdateOverlay
import dev.xboyrex.novaplayer.core.ui.update.NovaUpdateState
import dev.xboyrex.novaplayer.navigation.NovaNavigationBar
import dev.xboyrex.novaplayer.navigation.NovaNavigationRail
import dev.xboyrex.novaplayer.navigation.TopLevelDestination
import dev.xboyrex.novaplayer.navigation.TopLevelNavState
import dev.xboyrex.novaplayer.navigation.mediaNavGraph
import dev.xboyrex.novaplayer.navigation.moreNavGraph
import dev.xboyrex.novaplayer.navigation.navigationTransition
import dev.xboyrex.novaplayer.navigation.navigationTransitionModifier
import dev.xboyrex.novaplayer.navigation.networkNavGraph
import dev.xboyrex.novaplayer.navigation.openPlaylist
import dev.xboyrex.novaplayer.navigation.playlistNavGraph
import dev.xboyrex.novaplayer.navigation.rememberTopLevelNavState
import dev.xboyrex.novaplayer.navigation.settingsNavGraph
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mediaOperationsService: MediaOperationsService by inject()

    private val systemService: SystemService by inject()

    private val viewModel: MainViewModel by viewModel()

    private var pendingPlaylistId by mutableStateOf<Long?>(value = null)
    private var darkSystemBars = true

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlaylistId = PlaylistShortcut.playlistId(intent = intent)
    }

    override fun onResume() {
        super.onResume()
        setHighestRefreshRate()
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlassSettings.init(this)
        GlassQuality.init(this)
        setHighestRefreshRate()
        enableHdrOutput()

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
            val scope = rememberCoroutineScope()
            val updateState = remember { NovaUpdateState(scope, BuildConfig.VERSION_NAME) }
            val pageBackdrop = rememberLayerBackdrop()

            LaunchedEffect(Unit) {
                delay(1400)
                updateState.check(manual = false)
            }

            LaunchedEffect(updateState.message) {
                updateState.message?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    updateState.message = null
                }
            }

            LaunchedEffect(key1 = shouldUseDarkTheme) {
                darkSystemBars = shouldUseDarkTheme
                GlassSettings.updateThemeMode(if (shouldUseDarkTheme) "amoled" else "white")
                WindowCompat.getInsetsController(
                    window,
                    window.decorView,
                ).apply {
                    isAppearanceLightStatusBars = !shouldUseDarkTheme
                    isAppearanceLightNavigationBars = !shouldUseDarkTheme
                }
            }

            NovaPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = true,
                dynamicColor = shouldUseDynamicTheming(state = state),
            ) {
                CompositionLocalProvider(LocalGlassBackdrop provides pageBackdrop) {
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
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .layerBackdrop(pageBackdrop),
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
                            NovaUpdateOverlay(state = updateState, backdrop = pageBackdrop)
                        }
                    }
                }
            }
        }
    }

    private fun setHighestRefreshRate() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val disp = display ?: return
                val currentMode = disp.mode
                val modes = disp.supportedModes
                val bestMode = modes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }
                    ?: modes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = bestMode.modeId
                    lp.preferredRefreshRate = bestMode.refreshRate
                    window.attributes = lp
                }
            }
        }
    }

    private fun enableHdrOutput() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            @Suppress("DEPRECATION")
            val disp = display ?: return
            if (disp.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true) {
                window.colorMode = ActivityInfo.COLOR_MODE_HDR
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                AnimatedVisibility(
                    visible = showNavigation && showBottomBar,
                    enter = slideInVertically(tween(260, easing = NovaMotion.Emphasized)) { it } + fadeIn(tween(160)),
                    exit = slideOutVertically(tween(200, easing = NovaMotion.Emphasized)) { it } + fadeOut(tween(140)),
                ) {
                    NovaNavigationBar(
                        state = state,
                        fabState = displayedFabState,
                        contentFocusRequester = contentFocusRequester,
                        fabFocusRequester = fabFocusRequester,
                        showFabOnly = showNavRail,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
            NovaNavigationRail(
                state = state,
                modifier = Modifier.onSizeChanged {
                    railWidth =
                        with(receiver = density) { it.width.toDp() }
                },
            )
        }
    }
}

/**
 * Strictly two color modes:
 * - Pure AMOLED Black (`true`): default (`ThemeConfig.SYSTEM` and `ThemeConfig.ON`)
 * - Pure White (`false`): when `ThemeConfig.OFF` or `GlassSettings.themeMode == "white"`
 */
@Composable
fun shouldUseDarkTheme(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> GlassSettings.themeMode != "white"

    is MainActivityUiState.Success -> when (state.preferences.themeConfig) {
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
        ThemeConfig.SYSTEM -> GlassSettings.themeMode != "white"
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(state: MainActivityUiState): Boolean = true

@Composable
fun shouldUseDynamicTheming(state: MainActivityUiState): Boolean = when (state) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
