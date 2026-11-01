package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.extractor.metadata.Chapter
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSegmentedListItem
import dev.xboyrex.darkplayer.feature.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.ChaptersView(
    show: Boolean,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Chapter) -> Unit,
) {
    OverlayView(
        show = show,
        title = stringResource(R.string.chapters),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = currentChapterIndex.coerceAtLeast(minimumValue = 0),
            ),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            itemsIndexed(
                items = chapters,
                key = { _, chapter ->
                    chapter.startTimeMs
                },
            ) { index, chapter ->
                val isCurrent = index == currentChapterIndex
                NextSegmentedListItem(
                    selected = isCurrent,
                    isFirstItem = index == 0,
                    isLastItem = index == chapters.lastIndex,
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        contentColor = Color.White,
                        supportingContentColor = Color.White.copy(alpha = 0.65f),
                        selectedContainerColor = Color.White.copy(alpha = 0.16f),
                        selectedContentColor = Color.White,
                        selectedSupportingContentColor = Color.White.copy(alpha = 0.85f),
                    ),
                    onClick = { onChapterSelected(chapter) },
                    content = {
                        Text(
                            text = chapter.titleOrDefault(index = index),
                            maxLines = 2,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = chapter.startTimeMs.milliseconds.formatted(),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Media3 [Chapter.title] is a [androidx.media3.common.Label]?, not a String.
 * Extract [androidx.media3.common.Label.value] for display; fall back to "Chapter N".
 */
@Composable
internal fun Chapter.titleOrDefault(index: Int): String {
    val labelValue = title?.value
    if (!labelValue.isNullOrBlank()) {
        return labelValue
    }
    return stringResource(R.string.chapter_number, index + 1)
}
