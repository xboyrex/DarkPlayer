package dev.xboyrex.darkplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.MediaLayoutMode
import dev.xboyrex.darkplayer.core.model.MediaListCardStyle
import dev.xboyrex.darkplayer.core.model.Video
import dev.xboyrex.darkplayer.core.model.isNew
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSegmentedListItem
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

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
        MediaLayoutMode.LIST -> when (preferences.mediaListCardStyle) {
            MediaListCardStyle.MX_PRO_CLEAN -> MxVideoListItem(
                video = video,
                isRecentlyPlayedVideo = isRecentlyPlayedVideo,
                preferences = preferences,
                modifier = modifier,
                isLastItem = isLastItem,
                selected = selected,
                onClick = onClick,
                onLongClick = onLongClick,
            )

            MediaListCardStyle.LIQUID_GLASS_CARD,
            MediaListCardStyle.SEGMENTED_CARD,
            -> VideoListItem(
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

/**
 * MX Player Pro Clean Video Row (Fixes Bug #11 `153809_Nova Player.png`):
 * Clean unboxed row with crisp 16:9 thumbnail, duration badge, smooth progress indicator,
 * full-legibility title, and clean metadata subtitle without cluttered box borders.
 */
@Composable
fun MxVideoListItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val accent = LocalNovaAccentColor.current
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val shape = RoundedCornerShape(14.dp)
    val isHighlighted = isRecentlyPlayedVideo && preferences.markLastPlayedMedia
    val path = video.path.substringBeforeLast(delimiter = "/")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) accent.copy(alpha = 0.20f) else Color.Transparent)
                .then(
                    if (selected) {
                        Modifier.border(1.5.dp, accent.copy(alpha = 0.65f), shape)
                    } else {
                        Modifier
                    },
                )
                .combinedClickable(
                    onClick = {
                        view.novaHaptic(10L, 90, hapticsEnabled)
                        onClick()
                    },
                    onLongClick = onLongClick?.let { long ->
                        {
                            view.novaHaptic(22L, 180, hapticsEnabled)
                            long()
                        }
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThumbnailView(
                video = video,
                preferences = preferences,
                modifier = Modifier.width(
                    min(132.dp, LocalConfiguration.current.screenWidthDp.dp * 0.34f),
                ),
            )

            Spacer(modifier = Modifier.width(14.dp))

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
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineBreak = LineBreak.Heading,
                        color = when {
                            selected || isHighlighted -> accent
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    ),
                )

                val metaParts = buildList {
                    if (preferences.showSizeField && video.size > 0 && video.formattedFileSize.isNotBlank()) {
                        add(video.formattedFileSize)
                    }
                    if (preferences.showResolutionField && video.height > 0) {
                        add("${video.height}p")
                    }
                    if (isHighlighted || (video.playedPercentage != null && video.playedPercentage!! > 0.02f)) {
                        add("RESUME")
                    }
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString("  •  "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHighlighted) accent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (preferences.showPathField && path.isNotBlank()) {
                    Text(
                        text = path,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        if (!isLastItem && !selected) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 158.dp, end = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            )
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
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
    val path = video.path.substringBeforeLast(delimiter = "/")
    val showPath = preferences.showPathField && path.isNotBlank()
    val showSize =
        preferences.showSizeField && video.size > 0 && video.formattedFileSize.isNotBlank()
    val showResolution = preferences.showResolutionField && video.height > 0

    NextSegmentedListItem(
        modifier = modifier,
        selected = selected,
        contentPadding = PaddingValues(8.dp),
        colors = ListItemDefaults.segmentedColors(
            contentColor = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                MaterialTheme.colorScheme.primary
            } else {
                ListItemDefaults.segmentedColors().contentColor
            },
            supportingContentColor = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                MaterialTheme.colorScheme.primary
            } else {
                ListItemDefaults.colors().supportingContentColor
            },
            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
        ),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            ThumbnailView(
                video = video,
                preferences = preferences,
                modifier = Modifier
                    .width(
                        width = min(
                            150.dp,
                            b =
                                LocalConfiguration.current.screenWidthDp.dp * 0.35f,
                        ),
                    ),
            )
        },
        content = {
            Text(
                text = if (preferences.showExtensionField) {
                    video.nameWithExtension
                } else {
                    video.displayName
                },
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = if (showPath || showSize || showResolution) {
            {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showPath) {
                        Text(
                            text = path,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showSize || showResolution) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            if (showSize) {
                                InfoChip(text = video.formattedFileSize)
                            }
                            if (showResolution) {
                                InfoChip(text = "${video.height}p")
                            }
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    NextSegmentedListItem(
        modifier = modifier.width(IntrinsicSize.Min),
        selected = selected,
        contentPadding = PaddingValues(8.dp),
        colors = ListItemDefaults.segmentedColors(
            contentColor = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                MaterialTheme.colorScheme.primary
            } else {
                ListItemDefaults.segmentedColors().contentColor
            },
            supportingContentColor = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                MaterialTheme.colorScheme.primary
            } else {
                ListItemDefaults.colors().supportingContentColor
            },
            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
        ),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    style = textStyle.copy(lineBreak = LineBreak.Heading),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        ListItemDefaults.colors().contentColor
                    },
                )
            }
        },
    )
}

private fun Video.isLocalUri(): Boolean {
    val scheme = uriString.toUri().scheme
    return scheme.equals(
        other = "content",
        ignoreCase = true,
    ) ||
        scheme.equals(
            other = "file",
            ignoreCase = true,
        )
}

@Composable
private fun ThumbnailView(
    modifier: Modifier = Modifier,
    video: Video,
    preferences: ApplicationPreferences,
) {
    val context = LocalContext.current
    val accent = LocalNovaAccentColor.current
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
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
        if (preferences.showThumbnailField && isLocalVideo) {
            AsyncImage(
                model = ImageRequest.Builder(context = context)
                    .data(video.uriString)
                    .crossfade(enable = true)
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
                backgroundColor = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White,
                shape = RoundedCornerShape(4.dp),
            )
        }

        if (isNew) {
            InfoChip(
                text = stringResource(R.string.new_label),
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart),
                backgroundColor = accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(4.dp),
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
                        .background(Color.Black.copy(alpha = 0.45f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = video.playedPercentage!!)
                        .fillMaxHeight()
                        .clip(shape = CircleShape)
                        .background(accent),
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
