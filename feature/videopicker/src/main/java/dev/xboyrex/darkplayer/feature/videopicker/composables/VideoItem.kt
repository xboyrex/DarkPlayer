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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.MediaLayoutMode
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.model.isNew
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

private val NewBadgeYellow = Color(0xFF38BDF8)
private val NewBadgeDarkText = Color(0xFF06090F)

@Composable
fun VideoItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> VideoListItem(
            video = video,
            isRecentlyPlayedVideo = isRecentlyPlayedVideo,
            preferences = preferences,
            modifier = modifier,
            isFirstItem = isFirstItem,
            isLastItem = isLastItem,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        MediaLayoutMode.GRID -> VideoGridItem(
            video = video,
            isRecentlyPlayedVideo = isRecentlyPlayedVideo,
            preferences = preferences,
            modifier = modifier,
            isFirstItem = isFirstItem,
            isLastItem = isLastItem,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoListItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val accent = UiStylePreferences.activeAccentColor
    val path = video.path.substringBeforeLast(delimiter = "/")
    val showPath = preferences.showPathField && path.isNotBlank()
    val showSize = preferences.showSizeField && video.size > 0 && video.formattedFileSize.isNotBlank()
    val showResolution = preferences.showResolutionField && video.height > 0
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
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            ThumbnailView(
                video = video,
                preferences = preferences,
                modifier = Modifier.width(
                    min(126.dp, LocalConfiguration.current.screenWidthDp.dp * 0.32f),
                ),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (preferences.showExtensionField) {
                    video.nameWithExtension
                } else {
                    video.displayName
                },
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineBreak = LineBreak.Heading,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                    accent
                } else {
                    Color.White
                },
                overflow = TextOverflow.Ellipsis,
            )

            if (showPath) {
                Text(
                    text = path,
                    maxLines = 1,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showSize || showResolution || (isRecentlyPlayedVideo && preferences.markLastPlayedMedia)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showSize) {
                        InfoChip(text = video.formattedFileSize)
                    }
                    if (showResolution) {
                        InfoChip(text = "${video.height}p")
                    }
                    if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                        InfoChip(
                            text = "RESUME",
                            backgroundColor = accent.copy(alpha = 0.22f),
                            contentColor = accent,
                        )
                    }
                }
            }
        }

        if (onLongClick != null) {
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Video options",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    modifier: Modifier = Modifier,
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val accent = UiStylePreferences.activeAccentColor
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
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
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThumbnailView(
            video = video,
            preferences = preferences,
        )
        Text(
            text = if (preferences.showExtensionField) {
                video.nameWithExtension
            } else {
                video.displayName
            },
            maxLines = 2,
            style = textStyle.copy(lineBreak = LineBreak.Heading, fontSize = 13.5.sp),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                accent
            } else {
                Color.White
            },
        )
    }
}

private fun Video.isLocalUri(): Boolean {
    val scheme = uriString.toUri().scheme
    return scheme.equals("content", ignoreCase = true) ||
        scheme.equals("file", ignoreCase = true)
}

@Composable
private fun ThumbnailView(
    modifier: Modifier = Modifier,
    video: Video,
    preferences: ApplicationPreferences,
) {
    val context = LocalContext.current
    val isLocalVideo = remember(video.uriString) { video.isLocalUri() }
    val isNew by produceState(
        initialValue = video.isNew(thresholdDays = preferences.newVideoThresholdDays),
        key1 = video.dateAdded,
        key2 = video.lastPlayedAt,
        key3 = preferences.newVideoThresholdDays,
    ) {
        while (value) {
            delay(60.seconds)
            value = video.isNew(thresholdDays = preferences.newVideoThresholdDays)
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .aspectRatio(ratio = 16f / 10f),
    ) {
        Icon(
            imageVector = NextIcons.Video,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(fraction = 0.5f),
        )
        if (preferences.showThumbnailField && isLocalVideo) {
            AsyncImage(
                model = ImageRequest.Builder(context = context)
                    .data(video.uriString)
                    .size(480, 300)
                    .crossfade(100)
                    .build(),
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (preferences.showDurationField && video.duration > 0 &&
            video.formattedDuration.isNotBlank()
        ) {
            InfoChip(
                text = video.formattedDuration,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.BottomEnd),
                backgroundColor = Color.Black.copy(alpha = 0.78f),
                contentColor = Color.White,
                shape = RoundedCornerShape(5.dp),
            )
        }

        if (isNew) {
            InfoChip(
                text = stringResource(R.string.new_label),
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart),
                backgroundColor = NewBadgeYellow,
                contentColor = NewBadgeDarkText,
                shape = RoundedCornerShape(5.dp),
            )
        }

        if (preferences.showPlayedProgress && video.playedPercentage != null) {
            Box(
                modifier = Modifier
                    .height(3.5.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = video.playedPercentage!!)
                        .fillMaxHeight()
                        .clip(shape = CircleShape)
                        .background(Color(0xFF38BDF8)),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun VideoItemRecentlyPlayedPreview() {
    NextPlayerTheme {
        Surface {
            VideoListItem(
                video = Video.sample,
                preferences = ApplicationPreferences(),
                isRecentlyPlayedVideo = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun VideoItemPreview() {
    NextPlayerTheme {
        Surface {
            VideoListItem(
                video = Video.sample,
                preferences = ApplicationPreferences(),
                isRecentlyPlayedVideo = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun VideoGridItemPreview() {
    NextPlayerTheme {
        VideoGridItem(
            video = Video.sample,
            preferences = ApplicationPreferences(),
            isRecentlyPlayedVideo = true,
        )
    }
}
