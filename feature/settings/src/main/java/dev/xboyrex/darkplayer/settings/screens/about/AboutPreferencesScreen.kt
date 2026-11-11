package dev.xboyrex.darkplayer.settings.screens.about

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.xboyrex.darkplayer.core.ui.components.rememberTvListFocusRequester
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.updater.UpdateOverlayDialog
import dev.xboyrex.darkplayer.core.ui.updater.rememberUpdateState

private const val GITHUB_URL = "https://github.com/xboyrex/DarkPlayer"

private data class EcosystemPortal(
    val iconRes: Int?,
    val vectorIcon: ImageVector?,
    val title: String,
    val subtitle: String,
    val url: String,
)

@Composable
fun AboutPreferencesScreen(viewModel: AboutPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutPreferencesScreenContent(
    state: AboutPreferencesUiState,
    onAction: (AboutPreferencesAction) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listFocusRequester = rememberTvListFocusRequester()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val updateState = rememberUpdateState()

    val portals = remember {
        listOf(
            EcosystemPortal(
                iconRes = R.drawable.ic_github,
                vectorIcon = null,
                title = "Nova Player Source & Releases",
                subtitle = "github.com/xboyrex/DarkPlayer",
                url = "https://github.com/xboyrex/DarkPlayer",
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_telegram,
                vectorIcon = null,
                title = "Mayank Droid Telegram",
                subtitle = "t.me/MayankDroid7",
                url = "https://t.me/MayankDroid7",
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_telegram,
                vectorIcon = null,
                title = "Onyx Hub Telegram",
                subtitle = "t.me/OnyxHub7",
                url = "https://t.me/OnyxHub7",
            ),
            EcosystemPortal(
                iconRes = null,
                vectorIcon = NextIcons.Player,
                title = "Mayank Droid YouTube",
                subtitle = "youtube.com/@mayankdroid",
                url = "https://youtube.com/@mayankdroid",
            ),
            EcosystemPortal(
                iconRes = null,
                vectorIcon = NextIcons.Video,
                title = "Onyx Droid YouTube",
                subtitle = "youtube.com/@onyxdroid89",
                url = "https://youtube.com/@onyxdroid89",
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_portal_web,
                vectorIcon = null,
                title = "Instagram (@mayank.zx7)",
                subtitle = "instagram.com/mayank.zx7",
                url = "https://www.instagram.com/mayank.zx7",
            ),
            EcosystemPortal(
                iconRes = R.drawable.ic_portal_hub,
                vectorIcon = null,
                title = "Developer Contact Email",
                subtitle = "xboyrex496@gmail.com",
                url = "mailto:xboyrex496@gmail.com",
            ),
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Scaffold(
            modifier = Modifier.popupBackgroundBlur(extraBlur = updateState.isPopupActive),
            containerColor = Color.Black,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusTopInset + 8.dp, bottom = 8.dp, start = 14.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16181D))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            .pressable { onAction(AboutPreferencesAction.NavigateUp) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(id = R.string.about_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.4).sp,
                        color = Color.White,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .tvListFocus(focusRequester = listFocusRequester)
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = navBottomInset + 32.dp),
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

                Text(
                    text = "VERSION & UPDATES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14161A))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2229)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Version", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text("Nova Player v${state.appVersion}", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF132818))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2ECC71)))
                                Spacer(Modifier.width(5.dp))
                                Text("Up to date", color = Color(0xFF2ECC71), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable { updateState.check(context = context, manual = true) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2229)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.SystemUpdateAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Check for updates", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text("New GitHub releases install from inside the app", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                        }
                        Icon(Icons.AutoMirrored.Rounded.NavigateNext, null, tint = Color(0xFF757575), modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "DEVELOPER & OFFICIAL LINKS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14161A))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
                ) {
                    portals.forEachIndexed { index, portal ->
                        val resolvedIcon = portal.vectorIcon
                            ?: ImageVector.vectorResource(id = portal.iconRes!!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressable {
                                    uriHandler.openUriOrShowToast(
                                        uri = portal.url,
                                        context = context,
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1F2229)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = resolvedIcon,
                                    contentDescription = portal.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = portal.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                )
                                Text(
                                    text = portal.subtitle,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E),
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                contentDescription = null,
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (index < portals.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                        }
                    }
                }
            }
        }

        UpdateOverlayDialog(state = updateState)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF14161A))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.darkplayer_logo),
                contentDescription = "Nova Player Logo",
                modifier = Modifier
                    .size(58.dp)
                    .clip(shape = RoundedCornerShape(16.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Nova Player",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1F2229))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Reality Display Engine • Hardware Dolby Atmos • YTX Glass",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E),
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
                    contentColor = Color.Black,
                    containerColor = Color.White,
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .weight(weight = 1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
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
                    contentColor = Color.White,
                    containerColor = Color(0xFF1F2229),
                ),
                shape = RoundedCornerShape(50),
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
