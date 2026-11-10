package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
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
        containerColor = Color(0xFF16181D),
        contentColor = Color.White,
        supportingContentColor = Color(0xB8EBEBF5),
        leadingContentColor = GlasmioTheme.colors.accent,
        trailingContentColor = Color(0xB8EBEBF5),
        selectedContainerColor = Color(0xFF1F2430),
        selectedContentColor = Color.White,
        selectedSupportingContentColor = Color(0xFFE2E8F0),
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
    val focusInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by focusInteractionSource.collectIsFocusedAsState()
    val isPressed by focusInteractionSource.collectIsPressedAsState()
    val interactiveScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.988f
            isFocused -> 1.01f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = if (isPressed) 900f else 480f,
        ),
        label = "interactiveScale",
    )

    val resolvedShape = remember(isFirstItem, isLastItem) {
        RoundedCornerShape(
            topStart = if (isFirstItem) 22.dp else 4.dp,
            topEnd = if (isFirstItem) 22.dp else 4.dp,
            bottomStart = if (isLastItem) 22.dp else 4.dp,
            bottomEnd = if (isLastItem) 22.dp else 4.dp,
        )
    }

    val fixedShapes = remember(resolvedShape, shapes) {
        shapes.copy(
            shape = resolvedShape,
            selectedShape = resolvedShape,
            pressedShape = resolvedShape,
            focusedShape = resolvedShape,
            hoveredShape = resolvedShape,
            draggedShape = resolvedShape,
        )
    }

    SegmentedListItem(
        modifier = modifier
            .zIndex(zIndex = if (isFocused) 1f else 0f)
            .scale(scale = interactiveScale)
            .clip(resolvedShape)
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 1.8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = resolvedShape,
                    )
                } else {
                    Modifier.border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.07f),
                        shape = resolvedShape,
                    )
                },
            ),
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        shapes = fixedShapes,
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
        start = 14.dp,
        top = 20.dp,
        bottom = 8.dp,
    ),
    color: Color = Color(0xFF38BDF8),
) {
    Text(
        text = text,
        modifier = modifier.padding(paddingValues = contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        ),
    )
}
