package com.mayank.darkplayer.settings.screens.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mayank.darkplayer.core.common.extensions.appIcon
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.components.NextTopAppBar
import com.mayank.darkplayer.core.ui.designsystem.NextIcons
import com.mayank.darkplayer.core.ui.theme.calmEntrance
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateDialog
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState

private const val GITHUB_URL = "https://github.com/xboyrex/DarkPlayer"

@Composable
fun AboutPreferencesScreen(viewModel: AboutPreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutPreferencesScreenContent(
    state: AboutPreferencesUiState,
    onAction: (AboutPreferencesAction) -> Unit,
) {
    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val updateState = remember(scope, state.appVersion) {
        DarkPlayerUpdateState(scope = scope, currentVersionName = state.appVersion)
    }
    val appIconBitmap = remember(context) { context.appIcon()?.asImageBitmap() }

    val versionInfo = remember(context, state.appVersion) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
            "v${pInfo.versionName ?: state.appVersion} ($code)"
        } catch (_: Exception) {
            "v${state.appVersion}"
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val surfaceCard = MaterialTheme.colorScheme.surfaceContainer
    val innerTile = MaterialTheme.colorScheme.surfaceContainerHigh
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    DarkPlayerUpdateDialog(state = updateState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(AboutPreferencesAction.NavigateUp) }) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Editorial Borderless Hero Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 0)
                    .clip(RoundedCornerShape(28.dp))
                    .background(surfaceCard)
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(accentContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "Dark Player Logo",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "Dark Player Logo",
                            tint = accent,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Dark Player",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Crafted by Mayank (xboyrex)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = muted,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "$versionInfo  •  Release Build",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = onAccentContainer,
                    )
                }
            }

            // 2x2 Borderless Capability Tiles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 55),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.PlayCircle,
                    title = "Media3 Engine",
                    subtitle = "Hardware accelerated",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Memory,
                    title = "FFmpeg 10-bit",
                    subtitle = "Auto HW+ seeking",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 85),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.HdrOn,
                    title = "HDR10 / SDR",
                    subtitle = "Live tone control",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
                AboutCapabilityTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Liquid Glass",
                    subtitle = "Calm adaptive UI",
                    bg = surfaceCard,
                    accent = accent,
                    accentContainer = accentContainer,
                    muted = muted,
                )
            }

            // Borderless Actions Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .calmEntrance(delayMillis = 120)
                    .clip(RoundedCornerShape(24.dp))
                    .background(surfaceCard)
                    .padding(vertical = 6.dp),
            ) {
                AboutActionRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = "Fetch latest release from GitHub",
                    accent = accent,
                    badgeBg = accentContainer,
                    muted = muted,
                    onClick = { updateState.check(manual = true) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.Code,
                    title = "GitHub Repository",
                    subtitle = "github.com/xboyrex/DarkPlayer",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = { uriHandler.openUriOrShowToast(GITHUB_URL, context) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.libraries),
                    subtitle = "Open-source licenses & attributions",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = { onAction(AboutPreferencesAction.OpenLibraries) },
                )
                AboutActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    title = "Copy Build & Device Info",
                    subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MODEL}",
                    accent = accent,
                    badgeBg = innerTile,
                    muted = muted,
                    onClick = {
                        val info = "Dark Player $versionInfo | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Dark Player Info", info))
                        Toast.makeText(context, "Build info copied", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AboutCapabilityTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    bg: Color,
    accent: Color,
    accentContainer: Color,
    muted: Color,
) {
    Row(
        modifier = modifier
            .calmPressScale(pressedScale = 0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    badgeBg: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

internal fun UriHandler.openUriOrShowToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_opening_link),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
