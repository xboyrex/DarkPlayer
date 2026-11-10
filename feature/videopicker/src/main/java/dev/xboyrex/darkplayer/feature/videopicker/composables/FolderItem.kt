package dev.xboyrex.darkplayer.feature.videopicker.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.common.Utils
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.Folder
import dev.xboyrex.darkplayer.core.model.MediaLayoutMode
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

@Composable
fun FolderItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> FolderListItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
            preferences = preferences,
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        MediaLayoutMode.GRID -> FolderGridItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
            preferences = preferences,
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FolderListItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val accent = UiStylePreferences.activeAccentColor
    val cardShape = RoundedCornerShape(16.dp)
    val bgColor = if (selected) accent.copy(alpha = 0.16f) else Color(0xFF12151C)
    val borderColor = if (selected) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.06f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(cardShape)
            .background(bgColor)
            .border(0.8.dp, borderColor, cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Sleek MX Player Folder Icon Box
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B202C)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
                    accent
                } else {
                    Color(0xFF38BDF8)
                },
                modifier = Modifier.size(34.dp),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else if (folder.newVideosCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = folder.newVideosCount.coerceAtMost(99).toString(),
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // Folder Title + Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = folder.name,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                fontWeight = FontWeight.SemiBold,
                color = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
                    accent
                } else {
                    Color.White
                },
                overflow = TextOverflow.Ellipsis,
            )

            val metaParts = buildList {
                if (folder.videosCount > 0) {
                    val label = stringResource(
                        id = R.string.video.takeIf { folder.videosCount == 1 } ?: R.string.videos,
                    )
                    add("${folder.videosCount} $label")
                }
                if (folder.foldersCount > 0) {
                    val label = stringResource(
                        id = R.string.folder.takeIf { folder.foldersCount == 1 } ?: R.string.folders,
                    )
                    add("${folder.foldersCount} $label")
                }
                if (preferences.showSizeField && folder.totalSize > 0) {
                    add(Utils.formatFileSize(folder.totalSize))
                }
                if (preferences.showFolderDurationField && folder.totalDuration > 0) {
                    add(Utils.formatDurationMillis(folder.totalDuration))
                }
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString("  •  "),
                    maxLines = 1,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (preferences.showPathField) {
                Text(
                    text = folder.path.substringBeforeLast(delimiter = "/"),
                    maxLines = 1,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.42f),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onLongClick != null) {
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Folder actions",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderGridItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val accent = UiStylePreferences.activeAccentColor
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Color(0xFF12151C))
            .border(
                width = 0.8.dp,
                color = if (selected) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.06f),
                shape = cardShape,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 11f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B202C)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = folder.name,
            maxLines = 1,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${folder.videosCount} videos",
            maxLines = 1,
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@PreviewLightDark
@Composable
private fun FolderItemPreview() {
    NextPlayerTheme {
        FolderItem(
            folder = Folder.sample,
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = false,
        )
    }
}
