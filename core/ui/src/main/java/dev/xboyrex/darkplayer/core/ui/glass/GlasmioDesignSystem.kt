package dev.xboyrex.darkplayer.core.ui.glass

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Ported A-to-Z from NEORUAA/Glasmio (iOS 27 Liquid Glass UI Framework for Jetpack Compose).
 * Provides GlassTokens, GlasmioTheme, GlassSurface, GlasmioButton, GlasmioIconButton,
 * GlasmioSwitch, GlasmioSlider, GlasmioVolumeSlider, GlasmioSegmentedControl, GlasmioCard,
 * GlasmioGroupedList, GlasmioListRow, GlasmioSectionTitle, and GlasmioPreferenceEntry
 * with native iOS 27 spring physics, lens geometry, and liquid refraction by default.
 */

fun ContinuousRoundedRectangle(radius: Dp): CornerBasedShape = RoundedCornerShape(radius)

@Immutable
data class GlassAppearance(
    val liquidGlassEnabled: Boolean = true,
    val reduceTransparency: Boolean = false,
    val adaptiveColorScheme: Boolean = false,
) {
    val usesRefraction: Boolean
        get() = liquidGlassEnabled && !reduceTransparency && GlassQuality.effectsSupported

    val usesBackdrop: Boolean
        get() = !reduceTransparency && GlassQuality.effectsSupported
}

val LocalGlassAppearance = staticCompositionLocalOf { GlassAppearance() }

val IosPageHorizontalPadding = 16.dp

@Immutable
data class GlassColors(
    val isDark: Boolean,
    val accent: Color,
    val content: Color,
    val secondaryContent: Color,
    val container: Color,
    val prominentContainer: Color,
    val subtleStroke: Color,
    val groupedBackground: Color,
    val elevatedBackground: Color,
    val separator: Color,
    val tertiaryContent: Color,
    val destructive: Color,
)

@Immutable
data class GlassDimensions(
    val compactCornerRadius: Dp = 16.dp,
    val regularCornerRadius: Dp = 26.dp,
    val sheetCornerRadius: Dp = 38.dp,
    val controlHeight: Dp = 48.dp,
    val iconButtonSize: Dp = 44.dp,
    val bottomBarHeight: Dp = 64.dp,
)

val GlassColors.segmentedControlBackground: Color
    get() = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F767680)

val GlassColors.systemGray4: Color
    get() = if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)

val GlassColors.systemGray5: Color
    get() = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

@Immutable
data class GlasmioShapes(
    val extraSmall: CornerBasedShape = ContinuousRoundedRectangle(8.dp),
    val small: CornerBasedShape = ContinuousRoundedRectangle(16.dp),
    val medium: CornerBasedShape = ContinuousRoundedRectangle(26.dp),
    val large: CornerBasedShape = ContinuousRoundedRectangle(38.dp),
    val extraLarge: CornerBasedShape = ContinuousRoundedRectangle(44.dp),
) {
    fun materialShapes() = Shapes(extraSmall, small, medium, large, extraLarge)
}

@Immutable
data class GlasmioSpacing(
    val compact: Dp = 8.dp,
    val regular: Dp = 16.dp,
    val section: Dp = 32.dp,
)

object GlasmioMotion {
    fun <T> spring() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
}

object IosTypography {
    val fontFamily: FontFamily
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.Default
    val largeTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineLarge
    val title1: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium
    val title2: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineSmall
    val title3: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleLarge
    val headline: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val body: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge
    val callout: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium
    val subheadline: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium
    val footnote: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall
    val caption1: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelMedium
    val caption2: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelSmall
    val caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelMedium
}

fun defaultGlassColors(
    isDark: Boolean,
    accent: Color = if (isDark) Color(0xFFFACC15) else Color(0xFF0088FF),
): GlassColors {
    return GlassColors(
        isDark = isDark,
        accent = accent,
        content = if (isDark) Color.White else Color.Black,
        secondaryContent = if (isDark) Color(0xB2EBEBF5) else Color.Black.copy(alpha = 0.58f),
        container = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.58f) else Color(0xFFF8F8FA).copy(alpha = 0.64f),
        prominentContainer = accent.copy(alpha = if (isDark) 0.78f else 0.84f),
        subtleStroke = if (isDark) Color(0x2BFFFFFF) else Color.White.copy(alpha = 0.64f),
        groupedBackground = if (isDark) Color.Black else Color(0xFFF2F2F7),
        elevatedBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
        separator = if (isDark) Color(0x2BFFFFFF) else Color(0xFFE6E6E6),
        tertiaryContent = if (isDark) Color(0x4DEBEBF5) else Color(0x4D3C3C43),
        destructive = if (isDark) Color(0xFFFF4245) else Color(0xFFFF383C),
    )
}

val LocalGlassColors = compositionLocalOf {
    defaultGlassColors(isDark = true)
}

val LocalGlassContentColor = compositionLocalOf<Color?> { null }
val LocalGroupedListIconColor = staticCompositionLocalOf<Color?> { null }
val LocalGroupedListBackgroundAlpha = staticCompositionLocalOf { 1f }
val LocalGlassDimensions = staticCompositionLocalOf { GlassDimensions() }
val LocalGlasmioShapes = staticCompositionLocalOf { GlasmioShapes() }
val LocalGlasmioSpacing = staticCompositionLocalOf { GlasmioSpacing() }
internal val LocalMergedGlassCards = staticCompositionLocalOf { false }

object GlasmioTheme {
    val colors: GlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current
    val appearance: GlassAppearance
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassAppearance.current
    val dimensions: GlassDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassDimensions.current
    val shapes: GlasmioShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalGlasmioShapes.current
    val spacing: GlasmioSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalGlasmioSpacing.current
}

@Composable
fun ProvideGlasmioTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    accent: Color = MaterialTheme.colorScheme.primary,
    appearance: GlassAppearance = GlassAppearance(
        liquidGlassEnabled = GlassQuality.effectsSupported,
        reduceTransparency = !GlassQuality.effectsSupported,
    ),
    dimensions: GlassDimensions = GlassDimensions(),
    shapes: GlasmioShapes = GlasmioShapes(),
    spacing: GlasmioSpacing = GlasmioSpacing(),
    content: @Composable () -> Unit,
) {
    val glassColors = remember(isDark, accent) {
        defaultGlassColors(isDark = isDark, accent = accent)
    }
    CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalGlassAppearance provides appearance,
        LocalGlassDimensions provides dimensions,
        LocalGlasmioShapes provides shapes,
        LocalGlasmioSpacing provides spacing,
        content = content,
    )
}

/** Native iOS 27 Control Lens geometry styles from Glasmio. */
enum class NativeControlLensStyle {
    SwitchThumb,
    SliderThumb,
    SegmentedIndicator,
    TabBarIndicator,
}

const val SegmentedIndicatorPressedScale = 1.18f

/** Resize the rendered lens while retaining its resting layout and input bounds (from Glasmio GlassControls.kt). */
fun Modifier.nativeControlLensGeometry(
    style: NativeControlLensStyle,
    horizontalProgress: () -> Float,
    verticalProgress: () -> Float = horizontalProgress,
): Modifier = layout { measurable, constraints ->
    val restingWidth = constraints.maxWidth
    val restingHeight = constraints.maxHeight
    val horizontal = horizontalProgress()
    val vertical = verticalProgress()
    val width = when (style) {
        NativeControlLensStyle.SwitchThumb -> restingWidth * (1f + (58f / 37f - 1f) * horizontal)
        NativeControlLensStyle.SliderThumb -> restingWidth * (1f + 0.55f * horizontal)
        NativeControlLensStyle.SegmentedIndicator -> restingWidth + 24.dp.toPx() * horizontal
        NativeControlLensStyle.TabBarIndicator -> restingWidth + 16.dp.toPx() * horizontal
    }.fastRoundToInt().coerceAtLeast(1)
    val height = when (style) {
        NativeControlLensStyle.SwitchThumb -> restingHeight * (1f + (38.333333f / 24f - 1f) * vertical)
        NativeControlLensStyle.SliderThumb -> restingHeight * (1f + 0.55f * vertical)
        NativeControlLensStyle.SegmentedIndicator,
        NativeControlLensStyle.TabBarIndicator -> restingHeight + 16.dp.toPx() * vertical
    }.fastRoundToInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(restingWidth, restingHeight) {
        placeable.place((restingWidth - width) / 2, (restingHeight - height) / 2)
    }
}

/** Shared liquid drag deformation used by Glasmio glass controls and animated glass shells. */
fun GraphicsLayerScope.applyGlassDragScale(
    pressProgress: Float,
    offset: Offset,
    nativeButtonLift: Boolean = false,
    allowPressOvershoot: Boolean = false,
) {
    val progress = if (allowPressOvershoot) pressProgress else pressProgress.coerceIn(0f, 1f)
    val controlHeight = size.height.coerceAtLeast(1f)
    val lift = if (nativeButtonLift) 6.dp.toPx() / size.maxDimension.coerceAtLeast(1f)
    else 4.dp.toPx() / controlHeight
    val scale = lerp(1f, 1f + lift, progress)
    val maxOffset = size.minDimension.coerceAtLeast(1f)
    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
    val maxDragScale = 4.dp.toPx() / controlHeight
    val angle = atan2(offset.y, offset.x)
    scaleX = scale
    scaleY = scale
    scaleX += maxDragScale * abs(cos(angle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
        (size.width / controlHeight).fastCoerceAtMost(1f)
    scaleY += maxDragScale * abs(sin(angle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
        (controlHeight / size.width.coerceAtLeast(1f)).fastCoerceAtMost(1f)
}

enum class GlassEmphasis {
    Regular,
    Prominent,
}

enum class GlassSurfaceStyle {
    Standard,
    Navigation,
}

val LocalGlassSurfaceStyle = staticCompositionLocalOf { GlassSurfaceStyle.Navigation }

/**
 * Glasmio GlassSurface ported to use com.kyant.backdrop and InteractiveHighlight.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    shape: Shape = ContinuousRoundedRectangle(LocalGlassDimensions.current.regularCornerRadius),
    style: GlassSurfaceStyle = LocalGlassSurfaceStyle.current,
    navigationSurfaceColor: Color? = null,
    navigationSurfaceAlphaMultiplier: Float = 1.25f,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    enabled: Boolean = true,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 22.dp,
    sampleBackdrop: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val baseColor = when (emphasis) {
        GlassEmphasis.Regular -> navigationSurfaceColor ?: colors.container
        GlassEmphasis.Prominent -> navigationSurfaceColor ?: colors.prominentContainer
    }
    val resolvedTint = baseColor.copy(
        alpha = (baseColor.alpha * navigationSurfaceAlphaMultiplier).coerceIn(0f, 1f),
    )

    val surfaceModifier = if (sampleBackdrop && appearance.usesBackdrop && backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(16.dp.toPx())
                if (appearance.usesRefraction) {
                    val lift = 1f + 0.35f * interactiveHighlight.pressProgress
                    lens(
                        refractionHeight.toPx() * lift,
                        refractionAmount.toPx() * lift,
                        chromaticAberration = true,
                    )
                }
            },
            highlight = {
                Highlight.Default.copy(
                    alpha = (0.65f + 0.35f * interactiveHighlight.pressProgress).coerceIn(0f, 1f),
                )
            },
            shadow = {
                Shadow.Default.copy(alpha = 0.28f + 0.22f * interactiveHighlight.pressProgress)
            },
            innerShadow = {
                InnerShadow(
                    radius = 10.dp,
                    alpha = 0.45f + 0.25f * interactiveHighlight.pressProgress,
                )
            },
            layerBlock = {
                applyGlassDragScale(
                    pressProgress = interactiveHighlight.pressProgress,
                    offset = interactiveHighlight.offset,
                    nativeButtonLift = onClick != null,
                )
            },
            onDrawSurface = {
                drawRect(resolvedTint)
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (colors.isDark) 0.16f else 0.28f),
                        0.5f to Color.White.copy(alpha = 0.04f),
                        1f to Color.Black.copy(alpha = 0.08f),
                    ),
                )
            },
        ).border(
            width = 0.75.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (colors.isDark) 0.34f else 0.62f),
                    Color.White.copy(alpha = if (colors.isDark) 0.10f else 0.22f),
                ),
            ),
            shape = shape,
        )
    } else {
        Modifier
            .graphicsLayer {
                applyGlassDragScale(
                    pressProgress = interactiveHighlight.pressProgress,
                    offset = interactiveHighlight.offset,
                    nativeButtonLift = onClick != null,
                )
            }
            .clip(shape)
            .background(
                if (appearance.reduceTransparency) {
                    if (emphasis == GlassEmphasis.Prominent) colors.accent else colors.elevatedBackground
                } else {
                    resolvedTint
                },
            )
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (colors.isDark) 0.14f else 0.24f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.08f),
                    ),
                )
            }
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (colors.isDark) 0.28f else 0.52f),
                        Color.White.copy(alpha = if (colors.isDark) 0.08f else 0.16f),
                    ),
                ),
                shape = shape,
            )
    }

    val clickModifier = if (onClick != null) {
        val view = LocalView.current
        val click = if (onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
            )
        } else {
            Modifier.clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
            )
        }
        click.then(if (enabled) interactiveHighlight.gestureModifier else Modifier)
    } else {
        Modifier
    }

    val contentColor = when {
        !enabled -> colors.content.copy(alpha = 0.42f)
        emphasis == GlassEmphasis.Prominent -> Color.Black.takeIf { colors.accent == Color(0xFFFACC15) } ?: Color.White
        else -> colors.content
    }

    Box(
        modifier = modifier
            .then(surfaceModifier)
            .then(if (onClick != null && enabled) interactiveHighlight.modifier else Modifier)
            .then(clickModifier),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalGlassContentColor provides contentColor,
        ) {
            content()
        }
    }
}

enum class GlasmioButtonSize { Small, Medium, Large }

object GlasmioButtonDefaults {
    val SmallHeight = 28.dp
    val MediumHeight = 36.dp
    val LargeHeight = 50.dp

    fun height(size: GlasmioButtonSize?, dimensions: GlassDimensions): Dp = when (size) {
        GlasmioButtonSize.Small -> SmallHeight
        GlasmioButtonSize.Medium -> MediumHeight
        GlasmioButtonSize.Large -> LargeHeight
        null -> dimensions.controlHeight
    }
}

@Composable
fun GlasmioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    style: GlassSurfaceStyle = LocalGlassSurfaceStyle.current,
    navigationSurfaceColor: Color? = null,
    enabled: Boolean = true,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    navigationSurfaceAlphaMultiplier: Float = 1.25f,
    sampleBackdrop: Boolean = true,
    size: GlasmioButtonSize? = GlasmioButtonSize.Medium,
    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val buttonColors = when {
        !enabled -> colors.copy(content = if (colors.isDark) Color(0xFF505054) else Color(0xFFBFBFBF))
        destructive -> colors.copy(
            content = colors.destructive,
            prominentContainer = colors.destructive,
        )
        else -> colors
    }
    val height = GlasmioButtonDefaults.height(size, LocalGlassDimensions.current)
    val horizontalPadding = when (size) {
        GlasmioButtonSize.Small -> 12.dp
        GlasmioButtonSize.Medium -> 16.dp
        GlasmioButtonSize.Large -> 22.dp
        null -> 16.dp
    }
    val explicitTextStyle = size?.let {
        MaterialTheme.typography.labelLarge.copy(
            fontSize = if (it == GlasmioButtonSize.Small) 14.sp else 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    CompositionLocalProvider(LocalGlassColors provides buttonColors) {
        GlassSurface(
            modifier = modifier.height(height),
            backdrop = backdrop,
            style = style,
            navigationSurfaceColor = navigationSurfaceColor.takeIf { enabled },
            navigationSurfaceAlphaMultiplier = navigationSurfaceAlphaMultiplier,
            shape = Capsule(),
            emphasis = if (enabled) emphasis else GlassEmphasis.Regular,
            enabled = enabled,
            sampleBackdrop = sampleBackdrop,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (explicitTextStyle != null) ProvideTextStyle(explicitTextStyle) { content() }
                else content()
            }
        }
    }
}

@Composable
fun GlasmioIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    style: GlassSurfaceStyle = LocalGlassSurfaceStyle.current,
    navigationSurfaceColor: Color? = null,
    navigationSurfaceAlphaMultiplier: Float = 1.25f,
    enabled: Boolean = true,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    sampleBackdrop: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    size: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.size(size ?: LocalGlassDimensions.current.iconButtonSize),
        backdrop = backdrop,
        style = style,
        navigationSurfaceColor = navigationSurfaceColor.takeIf { enabled },
        navigationSurfaceAlphaMultiplier = navigationSurfaceAlphaMultiplier,
        shape = CircleShape,
        emphasis = if (enabled) emphasis else GlassEmphasis.Regular,
        enabled = enabled,
        sampleBackdrop = sampleBackdrop,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
    )
}

/**
 * iOS 27 Glasmio Switch: 63 x 28 dp track and 37 x 24 dp elastic liquid thumb.
 * Features DampedDragAnimation, NativeControlLensStyle.SwitchThumb expansion, and velocity deformation.
 */
@Composable
fun GlasmioSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    onTintColor: Color = Color.Unspecified,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(context) {
        UiStylePreferences.init(context)
    }
    val switchStyle = UiStylePreferences.switchStyle
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val light = !colors.isDark
    val onColor = when {
        onTintColor != Color.Unspecified && onTintColor != Color(0xFFFACC15) -> onTintColor
        switchStyle == SwitchStyle.MX_PRO_BLUE -> Color(0xFF38BDF8)
        switchStyle == SwitchStyle.YTX_EXPRESSIVE -> UiStylePreferences.activeAccentColor
        else -> if (colors.isDark) Color(0xFF30D158) else Color(0xFF34C759)
    }
    val track = (if (light) {
        Color(0xFF787878).copy(alpha = 0.24f)
    } else {
        Color(0xFF787880).copy(alpha = 0.38f)
    }).let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val density = LocalDensity.current
    val view = LocalView.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val trackWidthDp = if (switchStyle == SwitchStyle.MX_PRO_BLUE) 52.dp else 63.dp
    val trackHeightDp = if (switchStyle == SwitchStyle.MX_PRO_BLUE) 26.dp else 28.dp
    val thumbWidthDp = if (switchStyle == SwitchStyle.MX_PRO_BLUE) 22.dp else 37.dp
    val thumbHeightDp = if (switchStyle == SwitchStyle.MX_PRO_BLUE) 22.dp else 24.dp
    val travelDp = trackWidthDp - thumbWidthDp - 4.dp
    val travelPx = with(density) { travelDp.toPx() }
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled && onCheckedChange != null)
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "switchFraction",
    )
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 900f),
        label = "switchPress",
    )

    LaunchedEffect(checked) {
        fraction = if (checked) 1f else 0f
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .size(width = trackWidthDp, height = trackHeightDp)
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier
                        .semantics(mergeDescendants = true) {
                            role = Role.Switch
                            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                            onClick {
                                if (currentEnabled) {
                                    view.navHaptic(14L, 155)
                                    val next = !currentChecked
                                    fraction = if (next) 1f else 0f
                                    currentOnCheckedChange?.invoke(next)
                                }
                                currentEnabled
                            }
                        }
                        .pointerInput(enabled, travelPx, ltr) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                isPressed = true
                                view.navHaptic(10L, 110)
                                var didDrag = false
                                var dragFraction = if (currentChecked) 1f else 0f
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            isPressed = false
                                            val nextChecked = if (didDrag) {
                                                dragFraction >= 0.5f
                                            } else {
                                                !currentChecked
                                            }
                                            fraction = if (nextChecked) 1f else 0f
                                            view.navHaptic(14L, 155)
                                            currentOnCheckedChange?.invoke(nextChecked)
                                            break
                                        }
                                        val dx = change.position.x - change.previousPosition.x
                                        if (abs(change.position.x - down.position.x) > viewConfiguration.touchSlop * 0.4f) {
                                            didDrag = true
                                        }
                                        if (didDrag && travelPx > 0f) {
                                            val delta = dx / travelPx * if (ltr) 1f else -1f
                                            dragFraction = (dragFraction + delta).fastCoerceIn(0f, 1f)
                                            fraction = dragFraction
                                        }
                                        change.consume()
                                    }
                                } finally {
                                    isPressed = false
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track capsule
        Box(
            Modifier
                .clip(Capsule())
                .drawBehind {
                    val activeTrackColor = colorLerp(track, onColor, animatedFraction)
                    drawRect(activeTrackColor)
                }
                .border(
                    width = 0.75.dp,
                    color = Color.White.copy(alpha = if (colors.isDark) 0.16f else 0.25f),
                    shape = Capsule(),
                )
                .size(width = trackWidthDp, height = trackHeightDp),
        )
        // Elastic Liquid Glass Thumb
        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (ltr) lerp(padding, padding + travelPx, animatedFraction)
                    else lerp(-padding, -(padding + travelPx), animatedFraction)
                }
                .size(width = thumbWidthDp, height = thumbHeightDp)
                .then(
                    if (switchStyle == SwitchStyle.GLASMIO_LIQUID) {
                        Modifier.nativeControlLensGeometry(
                            NativeControlLensStyle.SwitchThumb,
                            horizontalProgress = { if (appearance.usesRefraction) pressProgress else 0f },
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(Capsule())
                .drawBehind {
                    val press = pressProgress
                    val fillAlpha = if (appearance.usesRefraction && switchStyle == SwitchStyle.GLASMIO_LIQUID) {
                        lerp(0.97f, 0.42f, press)
                    } else {
                        0.98f
                    }
                    drawRect(Color.White.copy(alpha = fillAlpha))
                    if (press > 0.01f) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.62f * press),
                                0.5f to onColor.copy(alpha = 0.22f * press),
                                1f to Color.White.copy(alpha = 0.40f * press),
                            ),
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.52f),
                        ),
                    ),
                    shape = Capsule(),
                ),
        )
    }
}

/**
 * iOS 27 Glasmio Liquid Slider with 6dp capsule track and 37x24dp elastic liquid thumb.
 */
@Composable
fun GlasmioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    accentColor: Color = LocalGlassColors.current.accent,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val safeStart = if (valueRange.start.isFinite()) valueRange.start else 0f
    val safeEnd = if (valueRange.endInclusive.isFinite() && valueRange.endInclusive > safeStart) {
        valueRange.endInclusive
    } else {
        safeStart + 1f
    }
    val safeRange = safeStart..safeEnd
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val light = !colors.isDark
    val track = (if (light) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }).let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentValue by rememberUpdatedState(value.coerceIn(safeRange))
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var dragValue by remember(safeRange, enabled) {
        mutableFloatStateOf(value.coerceIn(safeRange))
    }
    var dragging by remember(safeRange, enabled) { mutableStateOf(false) }
    val displayedValue = if (dragging) dragValue else currentValue
    val updateValue by rememberUpdatedState<(Float) -> Unit> { next ->
        val target = next.coerceIn(safeRange)
        val previous = if (dragging) dragValue else currentValue
        if (dragging) dragValue = target
        if (target != previous) currentOnValueChange(target)
    }
    val progress = {
        ((displayedValue - safeRange.start) / (safeRange.endInclusive - safeRange.start)).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .progressSemantics(displayedValue, safeRange)
            .semantics {
                if (!enabled) disabled()
                setProgress { requested ->
                    if (!enabled || !requested.isFinite()) {
                        false
                    } else {
                        val target = requested.coerceIn(safeRange)
                        if (target == displayedValue) {
                            false
                        } else {
                            updateValue(target)
                            currentOnValueChangeFinished?.invoke()
                            true
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth
        val thumbWidthPx = with(LocalDensity.current) { 37.dp.toPx() }
        val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val currentTrackWidth by rememberUpdatedState(trackWidth)
        val currentThumbWidth by rememberUpdatedState(thumbWidthPx)
        val currentLtr by rememberUpdatedState(ltr)
        val scope = rememberCoroutineScope()
        val animation = remember(scope, safeRange, enabled) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = value.fastCoerceIn(safeRange.start, safeRange.endInclusive),
                valueRange = safeRange,
                visibilityThreshold = (safeRange.endInclusive - safeRange.start) / 1000f,
                initialScale = 1f,
                pressedScale = 1.45f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
            )
        }
        val updateFromPosition by rememberUpdatedState<(Float) -> Unit> { x ->
            val fraction = ((x - currentThumbWidth / 2f) /
                (currentTrackWidth - currentThumbWidth).coerceAtLeast(1f)).coerceIn(0f, 1f)
            updateValue(
                if (currentLtr) lerp(safeRange.start, safeRange.endInclusive, fraction)
                else lerp(safeRange.endInclusive, safeRange.start, fraction),
            )
        }
        val finishGesture by rememberUpdatedState {
            if (currentEnabled) currentOnValueChangeFinished?.invoke()
            dragging = false
            animation.release()
        }

        // Track
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(track)
                    .height(6.dp)
                    .fillMaxWidth(),
            )
            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, trackConstraints ->
                        val placeable = measurable.measure(trackConstraints)
                        val width = (trackConstraints.maxWidth * progress()).fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }

        // 37x24dp Elastic Liquid Thumb
        Box(
            Modifier
                .graphicsLayer {
                    translationX = (trackWidth - size.width).coerceAtLeast(0f) * progress() * if (ltr) 1f else -1f
                    alpha = if (enabled) 1f else 0.45f
                }
                .size(width = 37.dp, height = 24.dp)
                .nativeControlLensGeometry(
                    NativeControlLensStyle.SliderThumb,
                    horizontalProgress = { if (appearance.usesRefraction) animation.pressProgress else 0f },
                )
                .clip(Capsule())
                .drawBehind {
                    val press = animation.pressProgress
                    val fillAlpha = if (appearance.usesRefraction) lerp(0.95f, 0.34f, press) else 0.98f
                    drawRect(Color.White.copy(alpha = fillAlpha))
                    if (press > 0.01f) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.58f * press),
                                0.5f to accentColor.copy(alpha = 0.22f * press),
                                1f to Color.White.copy(alpha = 0.36f * press),
                            ),
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.45f),
                        ),
                    ),
                    shape = Capsule(),
                ),
        )

        // Stationary pointer surface
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(enabled, safeRange, animation) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            animation.press()
                            tryAwaitRelease()
                            if (!dragging) animation.release()
                        },
                        onTap = { position ->
                            if (currentEnabled) {
                                updateFromPosition(position.x)
                                currentOnValueChangeFinished?.invoke()
                            }
                        },
                    )
                }
                .pointerInput(enabled, safeRange, animation) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { position ->
                            if (currentEnabled) {
                                dragValue = currentValue
                                dragging = true
                                animation.press()
                                updateFromPosition(position.x)
                            }
                        },
                        onDragEnd = { finishGesture() },
                        onDragCancel = { finishGesture() },
                        onHorizontalDrag = { change, _ ->
                            if (currentEnabled) updateFromPosition(change.position.x)
                        },
                    )
                },
        )
    }
}

/**
 * Glasmio filled iOS volume capsule (52dp height) with immediate local drag feedback.
 */
@Composable
fun GlasmioVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillColor: Color = Color.White,
    onValueChangeFinished: (() -> Unit)? = null,
    leadingIcon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val spacing = GlasmioTheme.spacing
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val track = colors.segmentedControlBackground.let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentValue by rememberUpdatedState(value.coerceIn(0f, 1f))
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var dragging by remember(enabled) { mutableStateOf(false) }
    var dragValue by remember(enabled) { mutableFloatStateOf(currentValue) }
    var focused by remember { mutableStateOf(false) }
    val displayedValue = if (dragging) dragValue else currentValue
    val updateValue by rememberUpdatedState<(Float) -> Boolean> { requested ->
        val next = requested.coerceIn(0f, 1f)
        val previous = if (dragging) dragValue else currentValue
        if (next == previous) {
            false
        } else {
            if (dragging) dragValue = next
            currentOnValueChange(next)
            true
        }
    }
    val finishGesture by rememberUpdatedState {
        if (dragging && currentEnabled) currentOnValueChangeFinished?.invoke()
        dragging = false
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .progressSemantics(displayedValue, 0f..1f)
            .onKeyEvent { event ->
                if (!currentEnabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val next = when (event.key) {
                    Key.DirectionLeft -> displayedValue + if (ltr) -0.05f else 0.05f
                    Key.DirectionRight -> displayedValue + if (ltr) 0.05f else -0.05f
                    Key.DirectionUp -> displayedValue + 0.05f
                    Key.DirectionDown -> displayedValue - 0.05f
                    else -> return@onKeyEvent false
                }
                if (updateValue(next)) currentOnValueChangeFinished?.invoke()
                true
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .pointerInput(enabled, ltr) {
                if (!enabled) return@pointerInput
                fun updateFromPosition(x: Float) {
                    val fraction = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    updateValue(if (ltr) fraction else 1f - fraction)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var verticalIntent = false
                    try {
                        val start = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                            if (!verticalIntent && abs(overSlop.x) > abs(overSlop.y)) {
                                if (currentEnabled) {
                                    change.consume()
                                    dragValue = currentValue
                                    dragging = true
                                    updateFromPosition(change.position.x)
                                }
                            } else {
                                verticalIntent = true
                            }
                        }
                        if (start != null && dragging) {
                            val ended = drag(start.id) { change ->
                                if (currentEnabled) {
                                    change.consume()
                                    updateFromPosition(change.position.x)
                                }
                            }
                            if (ended) {
                                currentEvent.changes.forEach { if (!it.pressed) it.consume() }
                            }
                        } else if (!verticalIntent && currentEnabled) {
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (up?.changedToUpIgnoreConsumed() == true && !up.isConsumed) {
                                up.consume()
                                updateFromPosition(up.position.x)
                                currentOnValueChangeFinished?.invoke()
                            }
                        }
                    } finally {
                        if (dragging) finishGesture()
                    }
                }
            }
            .clip(Capsule())
            .drawBehind {
                drawRect(track)
                val width = size.width * displayedValue
                drawRect(
                    color = fillColor,
                    topLeft = Offset(if (ltr) 0f else size.width - width, 0f),
                    size = Size(width, size.height),
                )
            }
            .border(
                width = if (enabled && focused) 2.dp else 0.75.dp,
                color = if (enabled && focused) colors.accent else Color.White.copy(alpha = 0.16f),
                shape = Capsule(),
            ),
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = spacing.regular),
            ) {
                leadingIcon( if (displayedValue > 0.15f) Color.Black else colors.content )
            }
        }
    }
}

@Composable
fun GlasmioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val merged = LocalMergedGlassCards.current
    val shape = ContinuousRoundedRectangle(LocalGlassDimensions.current.regularCornerRadius)
    Box(
        modifier = modifier
            .then(
                if (merged) {
                    Modifier.drawBehind {
                        drawLine(
                            colors.separator,
                            Offset(16.dp.toPx(), 0f),
                            Offset(size.width - 16.dp.toPx(), 0f),
                            1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                        .clip(shape)
                        .background(colors.elevatedBackground)
                        .border(0.75.dp, colors.subtleStroke, shape)
                },
            )
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ) else Modifier,
            ),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides colors.content,
                LocalGlassContentColor provides colors.content,
            ) {
                content()
            }
        },
    )
}

/**
 * Glasmio Segmented Control with elastic liquid indicator lens and spring physics.
 */
@Composable
fun <T> GlasmioSegmentedControl(
    items: List<Pair<T, String>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    positionAnimation: Animatable<Float, AnimationVector1D>? = null,
    height: Dp = 36.dp,
    enabled: Boolean = true,
    itemContent: (@Composable BoxScope.(value: T, selected: Boolean) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val isLight = !colors.isDark
    val selectedIndex = if (selected == null) -1 else items.indexOfFirst { it.first == selected }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentItems by rememberUpdatedState(items)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val scope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val trackColor = colors.segmentedControlBackground.let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val indicatorHeight = height - 4.dp
    val indicatorColor = if (isLight) Color.White else Color(0xFF48484A)

    BoxWithConstraints(
        modifier
            .height(height)
            .selectableGroup()
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        val density = LocalDensity.current
        val contentWidthPx = constraints.maxWidth.coerceAtLeast(items.size).toFloat()
        val tabWidthPx = contentWidthPx / items.size
        val indicatorWidthPx = (tabWidthPx - with(density) { 4.dp.toPx() }).coerceAtLeast(1f)
        val currentTabWidthPx by rememberUpdatedState(tabWidthPx)
        val trackPaddingPx = with(density) { 2.dp.toPx() }

        val animation = remember(scope, items.size, isLtr, positionAnimation) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedIndex.coerceAtLeast(0).toFloat(),
                valueRange = 0f..items.lastIndex.coerceAtLeast(1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = SegmentedIndicatorPressedScale,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, currentItems.lastIndex)
                    if (currentEnabled) currentOnSelected(currentItems[target].first)
                    animateToValue(target.toFloat())
                },
                onDrag = { _, dragAmount ->
                    if (!currentEnabled) return@DampedDragAnimation
                    updateValue(
                        (targetValue + dragAmount.x / currentTabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, currentItems.lastIndex.toFloat()),
                    )
                },
            )
        }
        LaunchedEffect(selectedIndex, enabled, animation) {
            if (selectedIndex >= 0) animation.updateValue(selectedIndex.toFloat())
        }

        // 1. Track background
        Box(
            Modifier
                .fillMaxSize()
                .clip(Capsule())
                .background(trackColor)
                .border(0.6.dp, colors.subtleStroke, Capsule()),
        )

        // 2. Elastic Liquid Indicator pill
        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .graphicsLayer {
                        val visualIndex = if (isLtr) animation.value else items.lastIndex - animation.value
                        translationX = trackPaddingPx + visualIndex * tabWidthPx
                        translationY = trackPaddingPx
                        val velocity = animation.velocity / 10f
                        scaleX = 1f / (1f - (velocity * 0.75f).fastCoerceIn(-0.18f, 0.18f))
                        scaleY = 1f - (velocity * 0.25f).fastCoerceIn(-0.14f, 0.14f)
                        transformOrigin = TransformOrigin.Center
                    }
                    .then(if (enabled) animation.modifier else Modifier)
                    .height(indicatorHeight)
                    .layout { measurable, segConstraints ->
                        val width = indicatorWidthPx.fastRoundToInt()
                        val placeable = measurable.measure(
                            segConstraints.copy(minWidth = width, maxWidth = width),
                        )
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .nativeControlLensGeometry(
                        NativeControlLensStyle.SegmentedIndicator,
                        horizontalProgress = { if (appearance.usesRefraction) animation.pressProgress else 0f },
                    )
                    .clip(Capsule())
                    .background(indicatorColor)
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.22f),
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.10f),
                            ),
                        )
                    }
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.28f),
                        shape = Capsule(),
                    ),
            )
        }

        // 3. Labels
        Row(
            Modifier
                .fillMaxSize()
                .clip(Capsule())
                .padding(vertical = 2.dp),
        ) {
            items.forEachIndexed { index, (key, label) ->
                val isSelected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (enabled) {
                                    animation.press()
                                    currentOnSelected(key)
                                    animation.release()
                                }
                            },
                        )
                        .then(if (itemContent != null) Modifier.semantics { contentDescription = label } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (itemContent == null) {
                        Text(
                            text = label,
                            style = IosTypography.caption.copy(fontSize = 13.sp, lineHeight = 16.sp),
                            color = if (isSelected) colors.content else colors.secondaryContent,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.content,
                            LocalGlassContentColor provides colors.content,
                        ) {
                            Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                                itemContent(key, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val LocalGroupedListCoordinates = staticCompositionLocalOf<State<LayoutCoordinates?>?> { null }

@Composable
fun Modifier.glasmioGroupedListSeparator(
    color: Color,
    visible: Boolean = true,
    horizontalInset: Dp = 0.dp,
): Modifier {
    if (!visible) return this
    val group = LocalGroupedListCoordinates.current
    var coordinates by remember(group) { mutableStateOf<LayoutCoordinates?>(null) }
    var position by remember(group) { mutableStateOf(Offset.Unspecified) }
    return this
        .then(
            if (group != null) Modifier.onGloballyPositioned {
                coordinates = it
                position = it.positionInRoot()
            } else Modifier,
        )
        .drawBehind {
            val owner = group?.value
            val row = coordinates.takeIf { position.isSpecified }
            val atTopEdge = owner != null && row != null && owner.isAttached && row.isAttached &&
                owner.findRootCoordinates() === row.findRootCoordinates() &&
                abs(owner.localPositionOf(row, Offset.Zero).y) < 0.5f
            if (!atTopEdge) {
                val inset = horizontalInset.toPx()
                drawLine(color, Offset(inset, 0f), Offset(size.width - inset, 0f), 0.75.dp.toPx())
            }
        }
}

@Composable
fun Modifier.glasmioGroupedListBackground(
    shape: Shape = ContinuousRoundedRectangle(26.dp),
): Modifier {
    val colors = LocalGlassColors.current
    return clip(shape)
        .background(
            colors.elevatedBackground.copy(
                alpha = LocalGroupedListBackgroundAlpha.current.coerceIn(0f, 1f),
            ),
            shape,
        )
        .border(0.75.dp, colors.subtleStroke, shape)
}

@Composable
fun GlasmioGroupedList(
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val shape = ContinuousRoundedRectangle(26.dp)
    val groupCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier
                        .glasmioGroupedListBackground(shape)
                        .onGloballyPositioned { groupCoordinates.value = it }
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(
            LocalGroupedListCoordinates provides if (framed) groupCoordinates else null,
            LocalMergedGlassCards provides true,
            LocalGroupedListIconColor provides colors.accent,
            LocalContentColor provides colors.content,
        ) {
            content()
        }
    }
}

@Composable
fun GlasmioListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showTopSeparator: Boolean = true,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    titleAlignment: TextAlign = TextAlign.Start,
) {
    val colors = LocalGlassColors.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) 52.dp else 68.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            Box(Modifier.alpha(if (enabled) 1f else 0.4f)) { it() }
            Spacer(Modifier.width(12.dp))
        }
        Row(
            Modifier
                .weight(1f)
                .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator)
                .alpha(if (enabled) 1f else 0.4f)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = IosTypography.body,
                    color = if (titleColor == Color.Unspecified) colors.content else titleColor,
                    textAlign = titleAlignment,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                detail?.let {
                    Text(
                        text = it,
                        style = IosTypography.body,
                        color = colors.secondaryContent,
                        maxLines = 1,
                    )
                }
                trailing?.let { trailingContent ->
                    CompositionLocalProvider(
                        LocalGroupedListIconColor provides colors.content,
                        LocalContentColor provides colors.content,
                    ) {
                        trailingContent()
                    }
                }
            }
        }
    }
}

@Composable
fun GlasmioSectionTitle(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(39.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = title,
            style = IosTypography.body.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            ),
            color = LocalGlassColors.current.secondaryContent,
        )
    }
}

@Composable
fun GlasmioGroupedTableFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
        style = IosTypography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = LocalGlassColors.current.secondaryContent,
    )
}

@Composable
fun GlasmioListValuePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalGlassColors.current
    Box(
        modifier
            .height(34.dp)
            .clip(Capsule())
            .background(colors.segmentedControlBackground)
            .border(0.6.dp, colors.subtleStroke, Capsule())
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IosTypography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = colors.content.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
fun GlasmioPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    showTopSeparator: Boolean = true,
) {
    val colors = LocalGlassColors.current
    val minimumHeight = if (description != null || content != null) 68.dp else 52.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = isEnabled && onClick != null,
                onClick = onClick ?: {},
            )
            .padding(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .alpha(if (isEnabled) 1f else 0.5f)
                    .padding(horizontal = 4.dp),
            ) {
                icon()
            }
            Spacer(Modifier.width(12.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = minimumHeight)
                .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator)
                .alpha(if (isEnabled) 1f else 0.5f)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                ProvideTextStyle(IosTypography.body) {
                    title()
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent,
                    )
                }
                content?.invoke()
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

@Composable
fun GlasmioSwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
    showTopSeparator: Boolean = true,
) {
    GlasmioPreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            GlasmioSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
            )
        },
        onClick = { onCheckedChange(!checked) },
        isEnabled = isEnabled,
        showTopSeparator = showTopSeparator,
    )
}
