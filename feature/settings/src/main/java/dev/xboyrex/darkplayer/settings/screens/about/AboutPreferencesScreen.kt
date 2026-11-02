package dev.xboyrex.darkplayer.settings.screens.about

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextSegmentedListItem
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons

private const val GITHUB_URL = "https://github.com/xboyrex/DarkPlayer"

private data class EcosystemPortal(
    val iconRes: Int?,
    val vectorIcon: ImageVector?,
    val title: String,
    val subtitle: String,
    val url: String,
    val accent: Color,
)

@Composable
fun AboutPreferencesScreen(viewModel: AboutPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AboutPreferencesScreenContent(
    state: AboutPreferencesUiState,
    onAction: (AboutPreferencesAction) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listFocusRequester = rememberTvListFocusRequester()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val portals = remember {
        listOf(
            EcosystemPortal(
                iconRes = R.drawable.ic_github,
                vectorIcon = null,
                title = "DarkPlayer Source & Releases",
                subtitle = "github.com/xboyrex/DarkPlayer",
                url = "https://github.com/xboyrex/DarkPlayer",
                accent = Color(0xFF0A84FF),
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_telegram,
                vectorIcon = null,
                title = "Mayank Droid Telegram",
                subtitle = "t.me/MayankDroid7",
                url = "https://t.me/MayankDroid7",
                accent = Color(0xFF38BDF8),
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_telegram,
                vectorIcon = null,
                title = "Onyx Hub Telegram",
                subtitle = "t.me/OnyxHub7",
                url = "https://t.me/OnyxHub7",
                accent = Color(0xFF06B6D4),
            ),
            EcosystemPortal(
                iconRes = null,
                vectorIcon = NextIcons.Player,
                title = "Mayank Droid YouTube",
                subtitle = "youtube.com/@mayankdroid",
                url = "https://youtube.com/@mayankdroid",
                accent = Color(0xFFEF4444),
            ),
            EcosystemPortal(
                iconRes = null,
                vectorIcon = NextIcons.Video,
                title = "Onyx Droid YouTube",
                subtitle = "youtube.com/@onyxdroid89",
                url = "https://youtube.com/@onyxdroid89",
                accent = Color(0xFFF97316),
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_portal_web,
                vectorIcon = null,
                title = "Instagram (@mayank.zx7)",
                subtitle = "instagram.com/mayank.zx7",
                url = "https://www.instagram.com/mayank.zx7",
                accent = Color(0xFFEC4899),
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_portal_hub,
                vectorIcon = null,
                title = "Developer Contact Email",
                subtitle = "xboyrex496@gmail.com",
                url = "mailto:xboyrex496@gmail.com",
                accent = Color(0xFF10B981),
            ),
        )
    }

    Scaffold(
        modifier = Modifier.popupBackgroundBlur(),
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AboutPreferencesAction.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = navBottomInset + 32.dp),
        ) {
            AboutApp(
                appVersion = state.appVersion,
                onGithubClick = {
                    uriHandler.openUriOrShowToast(
                        uri = GITHUB_URL,
                        context = context,
                    )
                },
                onLibrariesClick = { onAction(AboutPreferencesAction.OpenLibraries) },
            )
            ListSectionTitle(text = "DEVELOPER & OFFICIAL LINKS")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                portals.forEachIndexed { index, portal ->
                    val resolvedIcon = portal.vectorIcon
                        ?: ImageVector.vectorResource(id = portal.iconRes!!)
                    NextSegmentedListItem(
                        isFirstItem = index == 0,
                        isLastItem = index == portals.lastIndex,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(portal.accent.copy(alpha = 0.14f))
                                    .border(
                                        width = 0.8.dp,
                                        color = portal.accent.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(11.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = resolvedIcon,
                                    contentDescription = portal.title,
                                    tint = portal.accent,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        },
                        content = {
                            Text(
                                text = portal.title,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = portal.subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = NextIcons.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            uriHandler.openUriOrShowToast(
                                uri = portal.url,
                                context = context,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AboutApp(
    appVersion: String,
    modifier: Modifier = Modifier,
    onGithubClick: () -> Unit,
    onLibrariesClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "aboutGradient")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutFraction",
    )
    val cornerRadius = 22.dp

    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        c.primary.copy(alpha = 0.18f),
                        c.surfaceContainerHigh,
                        c.surfaceContainerHighest,
                    ),
                    start = Offset(
                        x = fraction * 900f,
                        y = 0f,
                    ),
                    end = Offset(
                        x = 900f - (fraction * 900f),
                        y = 900f,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = c.primary.copy(alpha = 0.30f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(all = 18.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.darkplayer_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(58.dp)
                    .clip(shape = RoundedCornerShape(15.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(c.primary.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "v$appVersion",
                            color = c.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.by,
                        stringResource(R.string.app_developer),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Next-Gen Media Engine · Hardware Dolby Atmos (@xboyrex)",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onLibrariesClick,
                colors = ButtonDefaults.buttonColors(
                    contentColor = c.onPrimary,
                    containerColor = c.primary,
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .weight(weight = 1f),
            ) {
                Icon(
                    imageVector = NextIcons.Code,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.libraries),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Button(
                onClick = onGithubClick,
                colors = ButtonDefaults.buttonColors(
                    contentColor = c.onSurface,
                    containerColor = c.surfaceContainerHighest,
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
                    .height(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.github),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

internal fun UriHandler.openUriOrShowToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_opening_link),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
