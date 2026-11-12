package dev.xboyrex.darkplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.xboyrex.darkplayer.core.common.Utils
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.feature.player.state.rememberPlaylistState
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Redesigned MX Player Pro "Playlist" Right-Side Drawer
 * Fixes Bug #14 (`153843_Nova Player.png`):
 * Clean borderless rows with 16:9 thumbnail, active playing tint, and reorder handle.
 */
@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaylistView(modifier: Modifier = Modifier, show: Boolean, player: Player) {
    val hapticFeedback = LocalHapticFeedback.current
    val playlistState = rememberPlaylistState(player = player)
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            playlistState.moveItem(
                from.index,
                to.index,
            )
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    LaunchedEffect(key1 = show) {
        if (show && playlistState.timeline.windowCount != 0) {
            val currentIndex = playlistState.currentMediaItemIndex
            if (currentIndex < playlistState.mediaItemCount) {
                lazyListState.scrollToItem(index = currentIndex)
            }
        }
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Playlist (${playlistState.mediaItemCount})",
        widthFraction = 0.44f,
    ) {
        if (playlistState.mediaItemCount == 0) {
            EmptyPlaylistView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    count = playlistState.mediaItemCount,
                    key = { index -> playlistState.getMediaItemAt(index = index).mediaId },
                ) { index ->
                    val mediaItem = playlistState.getMediaItemAt(index = index)
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = mediaItem.mediaId,
                    ) {
                        val isCurrentItem = index == playlistState.currentMediaItemIndex
                        PlaylistItemView(
                            mediaItem = mediaItem,
                            isCurrentItem = isCurrentItem,
                            canDelete = playlistState.mediaItemCount > 1,
                            onClick = { playlistState.seekToItem(index = index) },
                            onDelete = { playlistState.removeItem(index = index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlaylistItemView(
    mediaItem: MediaItem,
    isCurrentItem: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val accent = LocalNovaAccentColor.current
    val shape = RoundedCornerShape(12.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isCurrentItem) accent.copy(alpha = 0.20f) else Color.Transparent,
                )
                .then(
                    if (isCurrentItem) {
                        Modifier.border(1.dp, accent.copy(alpha = 0.55f), shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick)
                .draggableHandle(
                    onDragStarted = {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.GestureThresholdActivate,
                        )
                    },
                    onDragStopped = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    },
                    interactionSource = interactionSource,
                    dragGestureDetector = DragGestureDetector.LongPress,
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.reorder),
                tint = if (isCurrentItem) accent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))

            ThumbnailView(
                mediaItem = mediaItem,
                modifier = Modifier.width(88.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = mediaItem.mediaMetadata.title?.toString() ?: stringResource(R.string.unknown),
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrentItem) FontWeight.Bold else FontWeight.Medium,
                    lineBreak = LineBreak.Heading,
                ),
                color = if (isCurrentItem) accent else Color.White,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.remove),
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (!isCurrentItem) {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.06f),
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun ThumbnailView(modifier: Modifier = Modifier, mediaItem: MediaItem) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .aspectRatio(ratio = 16f / 10f),
    ) {
        Icon(
            imageVector = NextIcons.Video,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(fraction = 0.45f),
        )

        AsyncImage(
            model = ImageRequest.Builder(context = context)
                .data(mediaItem.mediaId)
                .crossfade(enable = true)
                .build(),
            contentDescription = null,
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        mediaItem.mediaMetadata.durationMs?.let { durationMs ->
            if (durationMs > 0) {
                Text(
                    text = Utils.formatDurationMillis(millis = durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(
                            vertical = 1.dp,
                            horizontal = 3.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = NextIcons.Video,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxSize(fraction = 0.3f),
        )
        Text(
            text = stringResource(R.string.no_videos_in_queue),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}
