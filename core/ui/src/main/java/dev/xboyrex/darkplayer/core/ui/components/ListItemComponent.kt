package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NextSegmentedListItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = GlasmioTheme.colors.elevatedBackground,
        headlineColor = GlasmioTheme.colors.content,
        supportingColor = GlasmioTheme.colors.secondaryContent,
        leadingIconColor = GlasmioTheme.colors.accent,
        trailingIconColor = GlasmioTheme.colors.secondaryContent,
    ),
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val overrideShape = RoundedCornerShape(26.dp)
    val innerCornerShape = RoundedCornerShape(6.dp)

    val focusInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    val isPressed by focusInteractionSource.collectIsPressedAsState()
    val interactiveScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.975f
            isFocused -> 1.01f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = if (isPressed) 850f else 420f,
        ),
        label = "interactiveScale",
    )

    val resolvedShape = remember(isFirstItem, isLastItem) {
        RoundedCornerShape(
            topStart = if (isFirstItem) 26.dp else 6.dp,
            topEnd = if (isFirstItem) 26.dp else 6.dp,
            bottomStart = if (isLastItem) 26.dp else 6.dp,
            bottomEnd = if (isLastItem) 26.dp else 6.dp,
        )
    }

    SegmentedListItem(
        modifier = modifier
            .zIndex(zIndex = if (isFocused) 1f else 0f)
            .scale(scale = interactiveScale)
            .border(
                width = if (isFocused) 2.dp else 0.6.dp,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    GlasmioTheme.colors.subtleStroke.copy(alpha = 0.14f)
                },
                shape = resolvedShape,
            ),
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        shapes = remember(
            isFirstItem,
            isLastItem,
            shapes,
        ) {
            val defaultBaseShape = shapes.shape
            if (defaultBaseShape is CornerBasedShape) {
                shapes.copy(
                    shape = defaultBaseShape.copy(
                        topStart = overrideShape.topStart.takeIf { isFirstItem }
                            ?: innerCornerShape.topStart,
                        topEnd = overrideShape.topEnd.takeIf { isFirstItem }
                            ?: innerCornerShape.topEnd,
                        bottomStart = overrideShape.bottomStart.takeIf { isLastItem }
                            ?: innerCornerShape.bottomStart,
                        bottomEnd = overrideShape.bottomEnd.takeIf { isLastItem }
                            ?: innerCornerShape.bottomEnd,
                    ),
                )
            } else {
                shapes
            }
        },
        colors = colors,
        contentPadding = contentPadding,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        interactionSource = focusInteractionSource,
        content = content,
    )
}

@Composable
fun ListSectionTitle(
    modifier: Modifier = Modifier,
    text: String,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 22.dp,
        bottom = 8.dp,
    ),
    color: Color = GlasmioTheme.colors.secondaryContent,
) {
    Text(
        text = text,
        modifier = modifier.padding(paddingValues = contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        ),
    )
}
