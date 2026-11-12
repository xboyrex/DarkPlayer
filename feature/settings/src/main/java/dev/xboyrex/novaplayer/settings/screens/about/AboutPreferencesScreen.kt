package dev.xboyrex.novaplayer.settings.screens.about

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.novaplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.novaplayer.core.ui.components.NovaTopAppBar
import dev.xboyrex.novaplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.novaplayer.core.ui.components.tvFocusDown
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyAboutDialog
import kotlinx.coroutines.launch

private const val GITHUB_REPO_URL = "https://github.com/xboyrex/NovaPlayer"
private const val GITHUB_PROFILE_URL = "https://github.com/xboyrex"
private const val TELEGRAM_MAIN_URL = "https://t.me/MayankDroid7"
private const val TELEGRAM_HUB_URL = "https://t.me/OnyxHub7"
private const val YOUTUBE_MAIN_URL = "https://youtube.com/@mayankdroid?si=slX8DDU0dpLSIYyY"
private const val YOUTUBE_ONYX_URL = "https://youtube.com/@onyxdroid89?si=9gxpZfa3bpI9PKku"
private const val INSTAGRAM_URL = "https://www.instagram.com/mayank.zx7"
private const val DEVELOPER_EMAIL = "xboyrex496@gmail.com"

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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showDolbyCredits by rememberSaveable { mutableStateOf(false) }

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NovaTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AboutPreferencesAction.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NovaIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
        ) {
            AboutApp(
                appVersion = state.appVersion,
                onGithubClick = {
                    uriHandler.openUriOrShowToast(
                        uri = GITHUB_REPO_URL,
                        context = context,
                    )
                },
                onLibrariesClick = { onAction(AboutPreferencesAction.OpenLibraries) },
            )

            ListSectionTitle(text = "Developer & Dolby Studio")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = "Dolby Studio & Developer Credits",
                    description = "ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく · Mayank (@xboyrex)",
                    icon = Icons.Rounded.GraphicEq,
                    onClick = { showDolbyCredits = true },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = "GitHub · @xboyrex",
                    description = "Android modding, Web & App developer repositories",
                    icon = ImageVector.vectorResource(R.drawable.ic_github),
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = GITHUB_PROFILE_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Telegram · @MayankDroid7",
                    description = "Official channel for Nova Player releases & updates",
                    icon = Icons.Rounded.Send,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = TELEGRAM_MAIN_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Onyx Hub · @OnyxHub7",
                    description = "Community discussion, support & feature requests",
                    icon = Icons.Rounded.Forum,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = TELEGRAM_HUB_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "YouTube · @mayankdroid",
                    description = "Mayank Droid official YouTube channel",
                    icon = Icons.Rounded.PlayCircle,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = YOUTUBE_MAIN_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "YouTube · @onyxdroid89",
                    description = "Onyx Droid official YouTube channel",
                    icon = Icons.Rounded.PlayCircle,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = YOUTUBE_ONYX_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Instagram · @mayank.zx7",
                    description = "Follow Mayank on Instagram",
                    icon = Icons.Rounded.PhotoCamera,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            uri = INSTAGRAM_URL,
                            context = context,
                        )
                    },
                )
                ClickablePreferenceItem(
                    title = "Email",
                    description = DEVELOPER_EMAIL,
                    icon = Icons.Rounded.AlternateEmail,
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                clipEntry = ClipEntry(
                                    ClipData.newPlainText(
                                        "email",
                                        DEVELOPER_EMAIL,
                                    ),
                                ),
                            )
                            Toast.makeText(
                                context,
                                "Email copied to clipboard",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    isLastItem = true,
                )
            }
        }
    }

    if (showDolbyCredits) {
        NovaDolbyAboutDialog(onDismiss = { showDolbyCredits = false })
    }
}

@Composable
fun AboutApp(
    appVersion: String,
    modifier: Modifier = Modifier,
    onGithubClick: () -> Unit,
    onLibrariesClick: () -> Unit,
) {
    val colorPrimary = MaterialTheme.colorScheme.primaryContainer
    val colorTertiary = MaterialTheme.colorScheme.tertiaryContainer

    val transition = rememberInfiniteTransition(label = "aboutGlow")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aboutFraction",
    )
    val cornerRadius = 24.dp

    Column(
        modifier = modifier
            .padding(
                vertical = 12.dp,
                horizontal = 4.dp,
            )
            .drawWithCache {
                val cx = size.width - size.width * fraction
                val cy = size.height * fraction

                val gradient = Brush.radialGradient(
                    colors = listOf(
                        colorPrimary,
                        colorTertiary,
                    ),
                    center = Offset(
                        x = cx,
                        y = cy,
                    ),
                    radius = 800f,
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = gradient,
                        cornerRadius = CornerRadius(
                            cornerRadius.toPx(),
                            cornerRadius.toPx(),
                        ),
                    )
                }
            }
            .padding(all = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_nova_logo),
                contentDescription = "Nova Player Logo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(shape = CircleShape),
            )
            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "v$appVersion",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                    )
                    Text(
                        text = "· by Mayank (@xboyrex)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onLibrariesClick,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .weight(weight = 1f),
            ) {
                Text(text = stringResource(R.string.libraries), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onGithubClick,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
                    .height(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.github), fontWeight = FontWeight.SemiBold)
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
