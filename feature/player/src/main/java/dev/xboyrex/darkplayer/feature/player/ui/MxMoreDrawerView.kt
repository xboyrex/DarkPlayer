package dev.xboyrex.darkplayer.feature.player.ui

import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.glass.MxQuickShortcut
import dev.xboyrex.darkplayer.core.ui.glass.NavBarStyle
import dev.xboyrex.darkplayer.core.ui.glass.SwitchStyle
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.glass.VolumeHudStyle
import dev.xboyrex.darkplayer.feature.player.utils.VideoFrameCapture

private data class MxGridAction(
    val title: String,
    val icon: ImageVector? = null,
    val badgeText: String? = null,
    val showRedDot: Boolean = false,
    val onClick: () -> Unit,
)

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MxMoreDrawerView(
    show: Boolean,
    player: Player,
    longPressSpeed: Float = 2.0f,
    onAspectRatioClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mxBlue = Color(0xFF38BDF8)
    var showMediaInfoDialog by remember { mutableStateOf(false) }
    var showDisplayStyleDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var bookmarkedPositionMs by remember { mutableLongStateOf(-1L) }

    val gridItems = listOf(
        MxGridAction(
            title = "Aspect Ratio",
            icon = Icons.Rounded.AspectRatio,
            onClick = {
                onAspectRatioClick()
            },
        ),
        MxGridAction(
            title = "Speed FF\n(Long Press)",
            badgeText = "${String.format("%.1f", longPressSpeed)}X",
            showRedDot = true,
            onClick = {
                onPlaybackSpeedClick()
            },
        ),
        MxGridAction(
            title = "Display\nSettings",
            icon = Icons.Rounded.Tune,
            onClick = {
                showDisplayStyleDialog = true
            },
        ),
        MxGridAction(
            title = "Playlists",
            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            onClick = {
                onPlaylistClick()
            },
        ),
        MxGridAction(
            title = "Network\nStream",
            icon = Icons.Rounded.Language,
            onClick = {
                val uriStr = player.currentMediaItem?.localConfiguration?.uri?.toString()
                    ?: player.currentMediaItem?.mediaId ?: "Local Media"
                Toast.makeText(context, "Source: $uriStr", Toast.LENGTH_SHORT).show()
            },
        ),
        MxGridAction(
            title = "Information",
            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
            onClick = {
                showMediaInfoDialog = true
            },
        ),
        MxGridAction(
            title = "Share",
            icon = Icons.Rounded.Share,
            onClick = {
                val uri = player.currentMediaItem?.localConfiguration?.uri
                if (uri != null) {
                    runCatching {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                    }
                } else {
                    Toast.makeText(context, "Unable to share stream", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            },
        ),
        MxGridAction(
            title = "Cut",
            icon = Icons.Rounded.ContentCut,
            onClick = {
                VideoFrameCapture.captureCurrentFrame(
                    context = context,
                    player = player,
                    scope = scope,
                )
                onDismiss()
            },
        ),
        MxGridAction(
            title = "Bookmark",
            icon = Icons.Rounded.BookmarkBorder,
            onClick = {
                if (bookmarkedPositionMs >= 0L) {
                    player.seekTo(bookmarkedPositionMs)
                    Toast.makeText(
                        context,
                        "Jumped to bookmark (${getStringForTime(bookmarkedPositionMs)})",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    bookmarkedPositionMs = player.currentPosition
                    Toast.makeText(
                        context,
                        "Bookmarked at ${getStringForTime(bookmarkedPositionMs)}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        ),
        MxGridAction(
            title = "Chapter",
            icon = Icons.Rounded.FormatListNumbered,
            onClick = {
                onChaptersClick()
            },
        ),
        MxGridAction(
            title = "Tutorial",
            icon = Icons.Rounded.Lightbulb,
            onClick = {
                showTutorialDialog = true
            },
        ),
        MxGridAction(
            title = "More",
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            onClick = {
                onEqualizerClick()
            },
        ),
    )

    OverlayView(
        show = show,
        title = "",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 4x3 MX Player Pro Icon Grid (matching 155720 & 155721)
            gridItems.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { item.onClick() }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (item.badgeText != null) {
                                        Text(
                                            text = item.badgeText,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                        )
                                    } else if (item.icon != null) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                if (item.showRedDot) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = 2.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                    )
                                }
                            }
                            Text(
                                text = item.title,
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Video Display switch row (matching 155720)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Video Display",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                NextSwitch(
                    checked = UiStylePreferences.videoDisplayOverlay,
                    onCheckedChange = { UiStylePreferences.updateVideoDisplayOverlay(it) },
                )
            }

            // Shortcuts switch row + 2-column MX Player Pro shortcut checkboxes (matching 155719 & 155720)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Shortcuts",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                NextSwitch(
                    checked = UiStylePreferences.shortcutsBarVisible,
                    onCheckedChange = { UiStylePreferences.updateShortcutsBarVisible(it) },
                )
            }

            if (UiStylePreferences.shortcutsBarVisible) {
                val shortcutPairs = listOf(
                    MxQuickShortcut.ROTATION to MxQuickShortcut.SPEED,
                    MxQuickShortcut.BACKGROUND_PLAY to MxQuickShortcut.LOOP,
                    MxQuickShortcut.MUTE to MxQuickShortcut.SHUFFLE,
                    MxQuickShortcut.EQUALIZER to MxQuickShortcut.SLEEP_TIMER,
                    MxQuickShortcut.AB_REPEAT to MxQuickShortcut.NIGHT_MODE,
                    MxQuickShortcut.CUSTOMISE to MxQuickShortcut.SCREENSHOT,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shortcutPairs.forEach { (leftItem, rightItem) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf(leftItem, rightItem).forEach { shortcut ->
                                val enabled = UiStylePreferences.enabledShortcuts.contains(shortcut.id)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { UiStylePreferences.toggleShortcut(shortcut) }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Checkbox(
                                        checked = enabled,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = mxBlue,
                                            uncheckedColor = Color.White.copy(alpha = 0.45f),
                                            checkmarkColor = Color.Black,
                                        ),
                                    )
                                    Text(
                                        text = shortcut.label,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMediaInfoDialog) {
        val videoSize = player.videoSize
        val durationStr = getStringForTime(player.duration)
        val titleStr = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Current Video"
        val uriStr = player.currentMediaItem?.localConfiguration?.uri?.toString()
            ?: player.currentMediaItem?.mediaId ?: "Unknown"
        AlertDialog(
            onDismissRequest = { showMediaInfoDialog = false },
            containerColor = Color(0xFF141822),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.88f),
            title = { Text("Media Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("File: $titleStr")
                    Text("Resolution: ${videoSize.width} × ${videoSize.height}")
                    Text("Duration: $durationStr")
                    Text("Playback Speed: ${player.playbackParameters.speed}x")
                    Text("Path / URI: $uriStr", fontSize = 12.sp, color = Color.White.copy(alpha = 0.68f))
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaInfoDialog = false }) {
                    Text("OK", color = mxBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showDisplayStyleDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayStyleDialog = false },
            containerColor = Color(0xFF141822),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Display & HUD Style", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Volume & Brightness Bar Style", color = mxBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    VolumeHudStyle.entries.forEach { hudStyle ->
                        val selected = UiStylePreferences.volumeHudStyle == hudStyle
                        RadioButtonRow(
                            selected = selected,
                            text = "${hudStyle.label} — ${hudStyle.subtitle}",
                            onClick = { UiStylePreferences.updateVolumeHudStyle(hudStyle) },
                        )
                    }
                    Text("Navigation Bar Style", color = mxBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    NavBarStyle.entries.forEach { navStyle ->
                        val selected = UiStylePreferences.navBarStyle == navStyle
                        RadioButtonRow(
                            selected = selected,
                            text = navStyle.label,
                            onClick = { UiStylePreferences.updateNavBarStyle(navStyle) },
                        )
                    }
                    Text("Switch Toggle Style", color = mxBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    SwitchStyle.entries.forEach { swStyle ->
                        val selected = UiStylePreferences.switchStyle == swStyle
                        RadioButtonRow(
                            selected = selected,
                            text = swStyle.label,
                            onClick = { UiStylePreferences.updateSwitchStyle(swStyle) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisplayStyleDialog = false }) {
                    Text("DONE", color = mxBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            containerColor = Color(0xFF141822),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.9f),
            title = { Text("Player Gestures & Shortcuts", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Hold / Long Press anywhere: Instant 2.0X Fast-Forward")
                    Text("• Left Vertical Swipe: Smooth Brightness control")
                    Text("• Right Vertical Swipe: Smooth Volume + 200% Boost")
                    Text("• Horizontal Swipe / Double Tap: Fast Frame Seek")
                    Text("• Pinch Two Fingers: Video Zoom & Pan")
                    Text("• A-B Repeat: Tap once for A, tap again for B, third tap to clear")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text("GOT IT", color = mxBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
