package dev.xboyrex.darkplayer.feature.more.screens.more

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.media.services.MediaOperationsService
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.audio.EqualizerAndDolbyStudioContent
import dev.xboyrex.darkplayer.core.ui.base.DataState
import dev.xboyrex.darkplayer.core.ui.components.BindTopLevelFab
import dev.xboyrex.darkplayer.core.ui.components.LocalNavigationBottomPadding
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.TopLevelFabKey
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.components.tvListFocus
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.MxClassicThemeSwatches
import dev.xboyrex.darkplayer.core.ui.glass.NavBarStyle
import dev.xboyrex.darkplayer.core.ui.glass.SwitchStyle
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.glass.VolumeHudStyle
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.feature.more.components.HistoryEmptyState
import dev.xboyrex.darkplayer.feature.videopicker.composables.VideoGridItem
import java.io.File

@Composable
fun MoreScreen(
    viewModel: MoreViewModel,
    directOutput: MoreViewModel.Output? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = { action ->
            if (directOutput != null) {
                when (action) {
                    is MoreAction.OpenHistory -> directOutput.openHistory()
                    is MoreAction.PlayVideo -> directOutput.playVideo(action.uri)
                    is MoreAction.OpenSettings -> directOutput.openSettings()
                    is MoreAction.OpenTrash -> directOutput.openTrash()
                    is MoreAction.OpenVault -> directOutput.openVault()
                }
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        UiStylePreferences.init(context)
    }
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { pickedUri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        pickedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                onAction(MoreAction.PlayVideo(pickedUri.toString()))
            }
        }
    val fallbackGetContentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }

    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }
    var showAppThemeDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showWhatsAppStatusDialog by remember { mutableStateOf(false) }

    val statusFiles = remember {
        runCatching {
            val root = Environment.getExternalStorageDirectory()
            val candidates = listOf(
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(root, "WhatsApp/Media/.Statuses"),
                File(root, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            )
            candidates
                .filter { it.exists() && it.isDirectory }
                .flatMap { dir ->
                    dir.listFiles()?.filter { f ->
                        f.isFile && (f.name.endsWith(".mp4", true) || f.name.endsWith(".mkv", true) || f.name.endsWith(".webm", true))
                    }.orEmpty()
                }
        }.getOrDefault(emptyList())
    }

    val mxBlue = UiStylePreferences.activeAccentColor

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.more),
                fontWeight = FontWeight.Bold,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .tvListFocus()
                    .padding(horizontal = 14.dp)
                    .padding(
                        top = 8.dp,
                        bottom =
                            scaffoldPadding.calculateBottomPadding() +
                                LocalNavigationBottomPadding.current + 104.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. MX Player Pro 3x2 Feature Grid Card (matching 155349_MX Player Pro.png)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF131720))
                        .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(vertical = 18.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MxHubGridItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SwapHoriz,
                            title = stringResource(R.string.pick_file),
                            tint = mxBlue,
                            onClick = {
                                runCatching {
                                    openDocumentLauncher.launch(
                                        arrayOf("video/*", "audio/*", "application/x-matroska", "application/ogg"),
                                    )
                                }.onFailure {
                                    fallbackGetContentLauncher.launch("video/*")
                                }
                            },
                        )
                        MxHubGridItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Lock,
                            title = stringResource(R.string.vault),
                            tint = mxBlue,
                            onClick = { onAction(MoreAction.OpenVault) },
                        )
                        MxHubGridItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.history),
                            tint = mxBlue,
                            onClick = { onAction(MoreAction.OpenHistory) },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MxHubGridItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Language,
                            title = "Network\nStream",
                            tint = mxBlue,
                            onClick = { showNetworkStreamDialog = true },
                        )
                        if (MediaOperationsService.supportsTrash()) {
                            MxHubGridItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.DeleteOutline,
                                title = stringResource(R.string.trash),
                                tint = mxBlue,
                                onClick = { onAction(MoreAction.OpenTrash) },
                            )
                        }
                        MxHubGridItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Equalizer,
                            title = "Dolby &\nEqualizer",
                            tint = mxBlue,
                            onClick = { showEqualizerDialog = true },
                        )
                    }
                }

                // 2. WhatsApp Status Saver Card (matching 155349_MX Player Pro.png)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF131720))
                        .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .pressable(pressedScale = 0.96f) { showWhatsAppStatusDialog = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderSpecial,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "WhatsApp Status Saver",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    if (statusFiles.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = statusFiles.size.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                    )
                }

                // 3. App Theme, UI Style & Settings Card (matching 155349_MX Player Pro.png)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF131720))
                        .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(pressedScale = 0.97f) { showAppThemeDialog = true }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = mxBlue,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "App Theme & UI Style",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = "${UiStylePreferences.navBarStyle.label} • ${UiStylePreferences.volumeHudStyle.label} HUD",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(pressedScale = 0.97f) { onAction(MoreAction.OpenSettings) }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = mxBlue,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = stringResource(R.string.settings),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }

                // 4. Watch History Section
                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
            }
        }
    }

    if (showNetworkStreamDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            containerColor = Color(0xFF141822),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Network Stream", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a direct video URL (HTTP, HTTPS, HLS .m3u8, DASH, RTSP):",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = { streamUrlInput = it },
                        placeholder = { Text("https://example.com/stream.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = streamUrlInput.trim()
                        if (trimmed.isNotEmpty()) {
                            showNetworkStreamDialog = false
                            onAction(MoreAction.PlayVideo(trimmed))
                        }
                    },
                ) {
                    Text("PLAY", color = mxBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkStreamDialog = false }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.7f))
                }
            },
        )
    }

    if (showAppThemeDialog) {
        Dialog(onDismissRequest = { showAppThemeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141822),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("App Theme & UI Customizer", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // Classic Themes Color Swatches (matching 155414_MX Player Pro.png)
                    Text("Classic Theme Swatches", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = mxBlue)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MxClassicThemeSwatches.forEachIndexed { index, swatchColor ->
                            val selected = UiStylePreferences.mxSwatchIndex == index
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 0.8.dp,
                                        color = if (selected) Color.White else Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { UiStylePreferences.updateMxSwatchIndex(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text("Navigation Bar Design", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = mxBlue)
                    NavBarStyle.entries.forEach { style ->
                        val selected = UiStylePreferences.navBarStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) mxBlue.copy(alpha = 0.16f) else Color(0xFF1B202C))
                                .clickable { UiStylePreferences.updateNavBarStyle(style) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(style.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(style.subtitle, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.65f))
                            }
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = mxBlue)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text("Volume & Brightness HUD Style", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = mxBlue)
                    VolumeHudStyle.entries.forEach { hud ->
                        val selected = UiStylePreferences.volumeHudStyle == hud
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) mxBlue.copy(alpha = 0.16f) else Color(0xFF1B202C))
                                .clickable { UiStylePreferences.updateVolumeHudStyle(hud) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(hud.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(hud.subtitle, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.65f))
                            }
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = mxBlue)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text("Switch Toggle Style", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = mxBlue)
                    SwitchStyle.entries.forEach { sw ->
                        val selected = UiStylePreferences.switchStyle == sw
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) mxBlue.copy(alpha = 0.16f) else Color(0xFF1B202C))
                                .clickable { UiStylePreferences.updateSwitchStyle(sw) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(sw.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = mxBlue)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showAppThemeDialog = false }) {
                            Text("DONE", color = mxBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEqualizerDialog) {
        Dialog(onDismissRequest = { showEqualizerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF12151E),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(620.dp),
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Dolby Atmos & Equalizer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton(onClick = { showEqualizerDialog = false }) {
                            Text("DONE", color = mxBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    EqualizerAndDolbyStudioContent()
                }
            }
        }
    }

    if (showWhatsAppStatusDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppStatusDialog = false },
            containerColor = Color(0xFF141822),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("WhatsApp Video Statuses", fontWeight = FontWeight.Bold) },
            text = {
                if (statusFiles.isEmpty()) {
                    Text(
                        text = "No cached WhatsApp video statuses found in .Statuses folder. View a video status in WhatsApp first or use Pick File to open any video.",
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        statusFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C2230))
                                    .clickable {
                                        showWhatsAppStatusDialog = false
                                        onAction(MoreAction.PlayVideo(Uri.fromFile(file).toString()))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = file.name,
                                    fontSize = 13.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.PlayCircleOutline,
                                    contentDescription = "Play status",
                                    tint = Color(0xFF25D366),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhatsAppStatusDialog = false }) {
                    Text("CLOSE", color = mxBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun MxHubGridItem(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = title,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistorySection(
    history: DataState<List<Video>>,
    preferences: ApplicationPreferences,
    onMoreClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
) {
    val videos = history.result.orEmpty().take(n = 10)
    val isTv = LocalContext.current.isTelevision
    val historyButtonFocusRequester = remember { FocusRequester() }
    val historyFocusState = rememberRestorableFocusState()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().thenIf(condition = isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp).weight(weight = 1f),
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .focusRequester(focusRequester = historyButtonFocusRequester)
                    .focusProperties {
                        if (isTv && !preferences.isHistoryPaused && videos.isNotEmpty()) {
                            down = historyFocusState.requester
                        }
                    }
                    .tvFocusRing(),
            ) {
                Icon(
                    imageVector = NextIcons.ArrowForward,
                    contentDescription = stringResource(R.string.history),
                )
            }
        }
        when {
            preferences.isHistoryPaused -> HistoryEmptyState(
                isHistoryOff = true,
                modifier = Modifier.fillMaxWidth(),
            )

            history is DataState.Success && videos.isEmpty() -> HistoryEmptyState(
                isHistoryOff = false,
                modifier = Modifier.fillMaxWidth(),
            )

            else -> {
                LazyRow(
                    modifier = Modifier
                        .focusProperties {
                            onExit = {
                                if (isTv && requestedFocusDirection == FocusDirection.Up) {
                                    historyButtonFocusRequester.requestFocus()
                                }
                            }
                        }
                        .restorableFocusGroup(
                            state = historyFocusState,
                            ready = false,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = videos,
                        key = { it.uriString },
                    ) { video ->
                        VideoGridItem(
                            video = video,
                            isRecentlyPlayedVideo = false,
                            textStyle = MaterialTheme.typography.bodySmall,
                            preferences = preferences,
                            modifier = Modifier
                                .width(146.dp)
                                .restorableFocusItem(
                                    state = historyFocusState,
                                    video.uriString,
                                ),
                            onClick = { onVideoClick(video) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MoreScreenContentPreview() {
    MoreScreenContent(
        state = MoreUiState(
            history = DataState.Success(value = emptyList()),
        ),
        onAction = {},
    )
}
