package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic

/**
 * Clean Glasmio/MX-style grouped list item that fixes:
 * - Bug #8 (`153558_Nova Player.png`): Eliminates ugly double-nested borders and jagged left corner gaps
 * - Bug #18 (`154019_Nova Player.png`): Properly clips selection highlight inside the rounded item corners
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NextSegmentedListItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    @Suppress("UNUSED_PARAMETER") shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val itemShape = remember(isFirstItem, isLastItem) {
        RoundedCornerShape(
            topStart = if (isFirstItem) 20.dp else 4.dp,
            topEnd = if (isFirstItem) 20.dp else 4.dp,
            bottomStart = if (isLastItem) 20.dp else 4.dp,
            bottomEnd = if (isLastItem) 20.dp else 4.dp,
        )
    }

    val focusInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.01f else 1f,
        label = "focusScale",
    )
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accent = LocalNovaAccentColor.current

    val containerBg = when {
        selected -> accent.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    }
    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> accent.copy(alpha = 0.65f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val separatorColor = Color.White.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(zIndex = if (isFocused) 1f else 0f)
            .scale(scale = focusScale)
            .clip(shape = itemShape)
            .background(color = containerBg, shape = itemShape)
            .border(
                width = if (isFocused || selected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = itemShape,
            )
            .drawBehind {
                if (!isLastItem && !selected) {
                    drawLine(
                        color = separatorColor,
                        start = Offset(16.dp.toPx(), size.height),
                        end = Offset(size.width - 16.dp.toPx(), size.height),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                }
            }
            .combinedClickable(
                interactionSource = focusInteractionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    view.novaHaptic(10L, 95, hapticsEnabled)
                    onClick()
                },
                onLongClick = onLongClick?.let { longClick ->
                    {
                        view.novaHaptic(22L, 180, hapticsEnabled)
                        longClick()
                    }
                },
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides if (selected) accent else colors.leadingContentColor,
                ) {
                    leadingContent()
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (overlineContent != null) {
                    ProvideTextStyle(
                        MaterialTheme.typography.labelSmall.copy(
                            color = colors.overlineContentColor,
                        ),
                    ) {
                        overlineContent()
                    }
                }
                ProvideTextStyle(
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (selected) accent else colors.contentColor,
                    ),
                ) {
                    content()
                }
                if (supportingContent != null) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium.copy(
                            color = colors.supportingContentColor.copy(alpha = 0.78f),
                        ),
                    ) {
                        supportingContent()
                    }
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                CompositionLocalProvider(
                    LocalContentColor provides colors.trailingContentColor,
                ) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun ListSectionTitle(
    modifier: Modifier = Modifier,
    text: String,
    contentPadding: PaddingValues = PaddingValues(
        start = 12.dp,
        top = 20.dp,
        bottom = 10.dp,
    ),
    color: Color = LocalNovaAccentColor.current,
) {
    Text(
        text = text,
        modifier = modifier.padding(paddingValues = contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
