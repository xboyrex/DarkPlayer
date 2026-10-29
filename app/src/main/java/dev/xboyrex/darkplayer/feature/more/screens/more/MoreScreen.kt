package dev.xboyrex.darkplayer.feature.more.screens.more

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.media.services.MediaOperationsService
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.ui.R
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
import dev.xboyrex.darkplayer.feature.more.components.HistoryEmptyState
import dev.xboyrex.darkplayer.feature.player.service.AudioBoostProfile
import dev.xboyrex.darkplayer.feature.player.service.DolbyVolumeBoosterEngine
import dev.xboyrex.darkplayer.feature.videopicker.composables.VideoGridItem
import dev.xboyrex.darkplayer.navigation.GlassQuality
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val HubGlassBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.05f),
    ),
)

@Composable
fun MoreScreen(viewModel: MoreViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }

    var showDolbyDialog by remember { mutableStateOf(false) }
    var showStreamUrlDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val isLiquidGlass = GlassQuality.userEnabled
    val cardFill = if (isLiquidGlass) Color.White.copy(alpha = 0.05f) else Color(0xFF0E131F)

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
                        top = 6.dp,
                        bottom =
                            scaffoldPadding.calculateBottomPadding() +
                                LocalNavigationBottomPadding.current + 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1. Compact Header Bar + Liquid Glass / Solid Mode Quick Switcher
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = HubGlassBorder,
                            shape = RoundedCornerShape(18.dp),
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.darkplayer_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFFACC15).copy(alpha = 0.45f), CircleShape),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "by ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Interactive Liquid Glass <-> Solid UI Switch Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFACC15).copy(alpha = 0.45f),
                                    shape = CircleShape,
                                )
                                .clickable { onAction(MoreAction.ToggleLiquidGlass) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLiquidGlass) Color(0xFFFACC15) else Color.White.copy(alpha = 0.5f),
                                    ),
                            )
                            Text(
                                text = if (isLiquidGlass) "Liquid Glass" else "Solid UI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiquidGlass) Color(0xFFFACC15) else Color.White,
                            )
                        }
                    }
                }

                // 2. Compact 1000% Intelligent Dolby Volume Booster Strip
                Surface(
                    onClick = { showDolbyDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = cardFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusRing()
                        .border(
                            width = 1.dp,
                            brush = HubGlassBorder,
                            shape = RoundedCornerShape(18.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFFFACC15).copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = NextIcons.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFFFACC15),
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = "1000% Dolby Audio Booster",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "${DolbyVolumeBoosterEngine.activeProfile.label} · Bass ${DolbyVolumeBoosterEngine.bassStrength}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.62f),
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFACC15).copy(alpha = 0.18f),
                            ) {
                                Text(
                                    text = "${DolbyVolumeBoosterEngine.boostPercentage}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFACC15),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(100, 200, 400, 600, 800, 1000).forEach { preset ->
                                val selected = kotlin.math.abs(DolbyVolumeBoosterEngine.boostPercentage - preset) < 25
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFFACC15)
                                            else Color.White.copy(alpha = 0.06f),
                                        )
                                        .clickable { DolbyVolumeBoosterEngine.setVolumePercentage(preset) }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${preset}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF090D16) else Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Compact 2-Column Slim Action Pills (Low vertical footprint!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactHubPill(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.vault),
                        subtitle = "PIN & Biometric",
                        icon = NextIcons.Security,
                        cardFill = cardFill,
                        onClick = { onAction(MoreAction.OpenVault) },
                    )
                    CompactHubPill(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.pick_file),
                        subtitle = "Open Local Video",
                        icon = NextIcons.FileOpen,
                        cardFill = cardFill,
                        onClick = { filePicker.launch(input = "video/*") },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactHubPill(
                        modifier = Modifier.weight(1f),
                        title = "Stream URL",
                        subtitle = "HTTP / HLS / RTSP",
                        icon = NextIcons.Link,
                        cardFill = cardFill,
                        onClick = { showStreamUrlDialog = true },
                    )
                    CompactHubPill(
                        modifier = Modifier.weight(1f),
                        title = "Auto Update",
                        subtitle = "GitHub OTA Release",
                        icon = NextIcons.Update,
                        cardFill = cardFill,
                        onClick = { showUpdateDialog = true },
                    )
                }

                if (MediaOperationsService.supportsTrash()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompactHubPill(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.trash),
                            subtitle = "Restore or Purge",
                            icon = NextIcons.Delete,
                            cardFill = cardFill,
                            onClick = { onAction(MoreAction.OpenTrash) },
                        )
                        CompactHubPill(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.settings),
                            subtitle = "All Preferences",
                            icon = NextIcons.Settings,
                            cardFill = cardFill,
                            onClick = { onAction(MoreAction.OpenSettings) },
                        )
                    }
                }

                // 4. Compact Watch History Strip
                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
            }
        }
    }

    if (showDolbyDialog) {
        DolbyBoosterDialog(
            onDismiss = { showDolbyDialog = false },
        )
    }

    if (showStreamUrlDialog) {
        StreamUrlDialog(
            onDismiss = { showStreamUrlDialog = false },
            onPlayUrl = { url ->
                showStreamUrlDialog = false
                onAction(MoreAction.PlayVideo(url))
            },
        )
    }

    if (showUpdateDialog) {
        OtaUpdateDialog(
            onDismiss = { showUpdateDialog = false },
        )
    }
}

@Composable
private fun CompactHubPill(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    cardFill: Color,
    onClick: () -> Unit,
) {
    val pillShape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        shape = pillShape,
        color = cardFill,
        modifier = modifier
            .tvFocusRing()
            .border(
                width = 1.dp,
                brush = HubGlassBorder,
                shape = pillShape,
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(17.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DolbyBoosterDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0F19),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "1000% Dolby Audio Booster",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${DolbyVolumeBoosterEngine.boostPercentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFACC15),
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Slider(
                    value = DolbyVolumeBoosterEngine.boostPercentage.toFloat(),
                    onValueChange = { DolbyVolumeBoosterEngine.setVolumePercentage(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFACC15),
                        activeTrackColor = Color(0xFFFACC15),
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                    ),
                )
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AudioBoostProfile.entries.forEach { profile ->
                        val selected = DolbyVolumeBoosterEngine.activeProfile == profile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(0xFFFACC15).copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.06f),
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFFFACC15) else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { DolbyVolumeBoosterEngine.selectProfile(profile) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profile.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color(0xFFFACC15) else Color.White,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sub-Bass Harmonic Punch",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = "${DolbyVolumeBoosterEngine.bassStrength}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFACC15),
                    )
                }
                Slider(
                    value = DolbyVolumeBoosterEngine.bassStrength.toFloat(),
                    onValueChange = { DolbyVolumeBoosterEngine.setBassBoostStrength(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFACC15),
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFACC15),
                    contentColor = Color(0xFF090D16),
                ),
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    runCatching {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "Built-in 1000% Dolby DSP active", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text("System EQ", color = Color.White.copy(alpha = 0.8f))
            }
        },
    )
}

@Composable
private fun StreamUrlDialog(
    onDismiss: () -> Unit,
    onPlayUrl: (String) -> Unit,
) {
    var urlText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0F19),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "Play Network Stream URL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a direct HTTP, HTTPS, HLS (.m3u8), DASH (.mpd), or RTSP video stream link:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/stream.m3u8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = urlText.trim()
                    if (trimmed.isNotEmpty()) onPlayUrl(trimmed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFACC15),
                    contentColor = Color(0xFF090D16),
                ),
            ) {
                Text("Play Stream", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.75f))
            }
        },
    )
}

@Composable
private fun OtaUpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(true) }
    var latestTag by remember { mutableStateOf("v1.2.0") }
    var releaseUrl by remember { mutableStateOf("https://github.com/xboyrex/DarkPlayer/releases/latest") }
    var apkDownloadUrl by remember { mutableStateOf("https://github.com/xboyrex/DarkPlayer/releases/latest") }
    var releaseNotes by remember {
        mutableStateOf(
            """
            • 1000% Intelligent Dolby Audio & Bass Booster Engine
            • Liquid Glass <-> Solid AMOLED UI Switcher
            • Translucent Liquid Glass Player & Overlay Sheets
            • Compact Hub & Instant Keyframe Thumbnails
            """.trimIndent()
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://api.github.com/repos/xboyrex/DarkPlayer/releases/latest").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    latestTag = json.optString("tag_name", latestTag)
                    releaseUrl = json.optString("html_url", releaseUrl)
                    val notes = json.optString("body", "")
                    if (notes.isNotBlank()) {
                        releaseNotes = notes.take(360)
                    }
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val dl = asset.optString("browser_download_url", "")
                            if (dl.endsWith("arm64-v8a.apk") || dl.endsWith("universal.apk")) {
                                apkDownloadUrl = dl
                                break
                            }
                        }
                    }
                }
            }
            checking = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0F19),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Dark Player OTA Update",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFACC15).copy(alpha = 0.18f),
                ) {
                    Text(
                        text = if (checking) "Checking..." else latestTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFACC15),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Official GitHub Release Channel (xboyrex/DarkPlayer)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFACC15),
                )
                Text(
                    text = releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl)))
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFACC15),
                    contentColor = Color(0xFF090D16),
                ),
            ) {
                Text("Download APK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.75f))
            }
        },
    )
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .thenIf(condition = isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(weight = 1f),
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(32.dp)
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
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(18.dp),
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
                    contentPadding = PaddingValues(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = videos,
                        key = { it.uriString },
                    ) { video ->
                        VideoGridItem(
                            video = video,
                            isRecentlyPlayedVideo = false,
                            textStyle = MaterialTheme.typography.labelSmall,
                            preferences = preferences,
                            modifier = Modifier
                                .width(118.dp)
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
