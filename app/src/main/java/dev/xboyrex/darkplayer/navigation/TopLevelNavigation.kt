package dev.xboyrex.darkplayer.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabKey
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabState
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.feature.more.navigation.MoreRoute
import dev.xboyrex.darkplayer.feature.network.navigation.NetworkRoute
import dev.xboyrex.darkplayer.feature.playlist.navigation.PlaylistListRoute
import dev.xboyrex.darkplayer.feature.videopicker.navigation.MediaPickerRoute

internal val YtxEmphasizedEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1f)

enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    @StringRes val labelRes: Int,
    val fabKey: String? = null,
) {
    MEDIA(
        MediaPickerRoute(),
        NextIcons.HomeOutline,
        NextIcons.Home,
        R.string.home,
        TopLevelFabKey.MEDIA,
    ),
    PLAYLISTS(
        PlaylistListRoute,
        NextIcons.VideoLibraryOutline,
        NextIcons.VideoLibrary,
        R.string.playlists,
        TopLevelFabKey.PLAYLISTS,
    ),
    NETWORK(
        NetworkRoute,
        NextIcons.NetworkOutline,
        NextIcons.Network,
        R.string.network,
        TopLevelFabKey.NETWORK,
    ),
    MORE(
        MoreRoute,
        NextIcons.DashBoardOutline,
        NextIcons.DashBoard,
        R.string.more,
        TopLevelFabKey.MORE,
    ),
}

@Composable
fun rememberTopLevelNavState(): TopLevelNavState {
    val destinations = TopLevelDestination.entries
    val backStacks = destinations.associate { dest ->
        val backStack = rememberNavBackStack(dest.route)
        backStack.ensureRoot(dest.route)
        dest.route to backStack
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(value = 0) }
    return remember(
        backStacks,
        selectedIndex,
    ) {
        TopLevelNavState(
            destinations = destinations,
            backStacks = backStacks,
            selectedIndexState = selectedIndex,
        )
    }
}

@Stable
class TopLevelNavState(
    val destinations: List<TopLevelDestination>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    private val selectedIndexState: MutableIntState,
) {
    var selectedIndex by selectedIndexState
        private set

    private val startRoute: NavKey get() = destinations.first().route

    val topLevelRoute: NavKey get() = destinations[selectedIndex].route

    val currentStack: NavBackStack<NavKey> get() = backStacks.getValue(key = topLevelRoute)

    private val stacksInUse: List<NavKey>
        get() = if (selectedIndex == 0) {
            listOf(element = startRoute)
        } else {
            listOf(
                startRoute,
                topLevelRoute,
            )
        }

    val topLevelContentKeys: Set<Any> = destinations.flatMap { dest ->
        listOf(
            dest.route,
            dest.route.toString(),
            Pair(
                first = "${dest.route}",
                second = "${dest.route::class}",
            ),
        )
    }.toSet()

    fun switchTo(route: NavKey) {
        val index = destinations.indexOfFirst { it.route == route }
        if (index >= 0) selectedIndex = index
    }

    fun goBack() {
        val stack = currentStack
        when {
            stack.size > 1 -> stack.removeLastOrNull()
            selectedIndex != 0 -> selectedIndex = 0
        }
    }

    @Composable
    fun rememberEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
        entryDecorator: NavEntryDecorator<NavKey>,
    ): SnapshotStateList<NavEntry<NavKey>> {
        val decoratedByRoute = LinkedHashMap<NavKey, List<NavEntry<NavKey>>>()
        for (dest in destinations) {
            decoratedByRoute[dest.route] = rememberDecoratedNavEntries(
                backStack = backStacks.getValue(dest.route),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    entryDecorator,
                ),
                entryProvider = entryProvider,
            )
        }
        return stacksInUse.flatMap { decoratedByRoute.getValue(key = it) }.toMutableStateList()
    }
}

fun TopLevelNavState.isNavigationBetweenTopLevelDestinations(
    initialState: Scene<NavKey>,
    targetState: Scene<NavKey>,
): Boolean = topLevelContentKeys.run {
    contains(initialState.entries.lastOrNull()?.contentKey) &&
        contains(targetState.entries.lastOrNull()?.contentKey)
}

internal fun TopLevelNavState.navigationTransition(
    initialState: Scene<NavKey>,
    targetState: Scene<NavKey>,
): ContentTransform {
    if (isNavigationBetweenTopLevelDestinations(
            initialState = initialState,
            targetState = targetState,
        )
    ) {
        return (
            fadeIn(
                animationSpec = tween(
                    durationMillis = 220,
                    easing = YtxEmphasizedEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = YtxEmphasizedEasing,
                ),
                initialOffsetY = { (it / 35).coerceIn(8, 24) },
            )
            ) togetherWith
            fadeOut(
                animationSpec = tween(
                    durationMillis = 90,
                ),
            )
    }

    val isPop = initialState.previousEntries.any {
        it.contentKey == targetState.entries.lastOrNull()?.contentKey
    }
    return (
        slideInHorizontally(
            initialOffsetX = { if (isPop) -(it * 0.28f).toInt() else it },
            animationSpec = tween(
                durationMillis = 280,
                easing = YtxEmphasizedEasing,
            ),
        ) + fadeIn(tween(120))
        ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { if (isPop) it else -(it * 0.28f).toInt() },
            animationSpec = tween(
                durationMillis = 240,
                easing = YtxEmphasizedEasing,
            ),
        ) + fadeOut(tween(180))
        )
}

@Composable
fun NextNavigationBar(
    state: TopLevelNavState,
    fabState: TopLevelFabState?,
    contentFocusRequester: FocusRequester,
    fabFocusRequester: FocusRequester,
    showFabOnly: Boolean = false,
    backdrop: Backdrop = rememberLayerBackdrop(),
) {
    val isTv = LocalContext.current.isTelevision
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(bottom = 12.dp)
            .padding(horizontal = 16.dp)
            .thenIf(condition = !showFabOnly) { pointerInput(key1 = Unit) {} },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!showFabOnly) {
                val destinations = state.destinations
                LiquidBottomTabs(
                    selectedTabIndex = { state.selectedIndex },
                    onTabSelected = { index ->
                        destinations.getOrNull(index)?.let { state.switchTo(it.route) }
                    },
                    backdrop = backdrop,
                    tabsCount = destinations.size,
                    modifier = Modifier.weight(1f),
                ) {
                    destinations.forEach { destination ->
                        val selected = destination.route == state.topLevelRoute
                        LiquidNavTab(
                            label = stringResource(destination.labelRes),
                            icon = destination.icon,
                            selectedIcon = destination.selectedIcon,
                            selected = selected,
                            onClick = { state.switchTo(destination.route) },
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(weight = 1f))
            }

            fabState?.let { fab ->
                val glassEnabled = GlassQuality.effectsSupported
                val accentFill = Color(0xFF10131C).copy(alpha = if (isDark) 0.48f else 0.62f)
                val fabGlassModifier = if (glassEnabled) {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(12.dp.toPx())
                                lens(18.dp.toPx(), 24.dp.toPx(), chromaticAberration = true)
                            },
                            highlight = { Highlight.Default },
                            onDrawSurface = {
                                drawRect(accentFill)
                                drawRect(Color(0xFFFACC15).copy(alpha = 0.16f))
                            },
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFACC15).copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.12f),
                                ),
                            ),
                            shape = CircleShape,
                        )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFACC15))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                }

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .testTag(tag = "top_level_fab")
                        .focusRequester(focusRequester = fabFocusRequester)
                        .tvFocusRing(shape = CircleShape)
                        .focusProperties {
                            if (isTv) {
                                up =
                                    fab.upFocusRequester.takeUnless { it == FocusRequester.Default }
                                        ?: contentFocusRequester
                            }
                        }
                        .then(fabGlassModifier)
                        .clickable(onClick = fab.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = fab,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = FastOutSlowInEasing,
                                ),
                            ) +
                                scaleIn(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                    initialScale = 0.25f,
                                ) togetherWith
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                ) +
                                scaleOut(
                                    animationSpec = tween(
                                        durationMillis = 250,
                                        easing = FastOutSlowInEasing,
                                    ),
                                    targetScale = 0.25f,
                                )
                        },
                        label = "fab_icon_swap",
                    ) { fabItem ->
                        Icon(
                            imageVector = fabItem.icon,
                            contentDescription = null,
                            tint = if (GlassQuality.effectsSupported) Color(0xFFFACC15) else Color(0xFF090D16),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NextNavigationRail(state: TopLevelNavState, modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                8.dp,
                Alignment.CenterVertically,
            ),
        ) {
            state.destinations.forEach { dest ->
                val selected = state.topLevelRoute == dest.route
                Box(
                    Modifier
                        .tvFocusRing(shape = RoundedCornerShape(99.dp))
                        .clip(shape = CircleShape)
                        .selectable(
                            selected = state.topLevelRoute == dest.route,
                            onClick = { state.switchTo(dest.route) },
                            role = Role.Tab,
                        )
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                    propagateMinConstraints = true,
                ) {
                    CompositionLocalProvider(
                        value = LocalContentColor provides if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
