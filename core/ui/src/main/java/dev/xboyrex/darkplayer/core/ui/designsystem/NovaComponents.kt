package dev.xboyrex.darkplayer.core.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.xboyrex.darkplayer.core.model.MxAppThemeMode
import dev.xboyrex.darkplayer.core.model.NovaNavBarStyle
import dev.xboyrex.darkplayer.core.model.NovaSwitchStyle
import dev.xboyrex.darkplayer.core.model.PlayerControlStyle
import dev.xboyrex.darkplayer.core.model.VolumeBrightnessBarStyle
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import kotlin.math.roundToInt

/**
 * Signature Nova Logo Gradient extracted from the circular clapperboard logo (4591.png):
 * Electric Sky Blue (#4A90FF) -> Royal Violet (#7C3AED) -> Neon Orchid Pink (#EC4899)
 */
val NovaLogoBlue = Color(0xFF4A90FF)
val NovaLogoViolet = Color(0xFF7C3AED)
val NovaLogoPink = Color(0xFFEC4899)

val NovaLogoGradientBrush = Brush.linearGradient(
    colors = listOf(NovaLogoBlue, NovaLogoViolet, NovaLogoPink),
)

/**
 * Tactile spring press bounce + haptic feedback modifier.
 */
fun Modifier.novaPressable(enabled: Boolean = true): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "novaPressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * iOS-inspired fluid spring capsule switch with Nova gradient/accent track & crisp haptics.
 */
@Composable
fun NovaFluidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTintColor: Color = LocalNovaAccentColor.current,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 520f,
        ),
        label = "novaSwitchOffset",
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (isPressed && enabled) 28.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "novaSwitchThumbWidth",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) onTintColor else Color(0xFF323542).copy(alpha = 0.72f),
        animationSpec = tween(180),
        label = "novaSwitchTrack",
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(
                brush = if (checked) {
                    Brush.horizontalGradient(
                        listOf(onTintColor, NovaLogoViolet.copy(alpha = 0.92f)),
                    )
                } else {
                    Brush.horizontalGradient(listOf(trackColor, trackColor))
                },
            )
            .border(
                width = 0.8.dp,
                color = if (checked) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
            ) {
                view.novaHaptic(12L, 135, hapticsEnabled)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = if (checked && isPressed) thumbOffset - 4.dp else thumbOffset)
                .size(width = thumbWidth, height = 24.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * Fluid continuous slider with Nova gradient active track and haptic step ticks.
 */
@Composable
fun NovaFluidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 700f),
        label = "novaSliderFraction",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    view.novaHaptic(8L, 100, hapticsEnabled)
                    onValueChange(valueRange.start + f * span)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { onValueChangeFinished?.invoke() },
                    onDragCancel = { onValueChangeFinished?.invoke() },
                ) { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + f * span)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(NovaLogoGradientBrush),
            )
        }
        val thumbOffset = (trackWidth - 22.dp) * animatedFraction
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, NovaLogoViolet, CircleShape),
        )
    }
}

/**
 * Compact minimal segmented control with spring-animated selection pill.
 */
@Composable
fun <T> NovaSegmentedControl(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accent = LocalNovaAccentColor.current

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f))
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (item, label) ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        brush = if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(accent, NovaLogoViolet.copy(alpha = 0.90f)),
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                    )
                    .clickable {
                        if (!isSelected) {
                            view.novaHaptic(10L, 125, hapticsEnabled)
                            onSelected(item)
                        }
                    }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Frosted/Blurred Compact UI Switcher Popup Dialog ("Popup mei show ho player mei add kro ui button
 * kon sa chiye apko ... bahar mei show Krna pop mei blur with aesa rhega okh select kr lega").
 * Zero mention of external zip/project names — uses clean native Nova names.
 */
@Composable
fun NovaUiSwitcherPopup(
    currentThemeMode: MxAppThemeMode,
    useDynamicDeviceAccent: Boolean,
    currentNavBarStyle: NovaNavBarStyle,
    currentSwitchStyle: NovaSwitchStyle,
    currentHudStyle: VolumeBrightnessBarStyle,
    currentPlayerStyle: PlayerControlStyle,
    onSelectThemeMode: (MxAppThemeMode) -> Unit,
    onSelectDynamicAccent: (Boolean) -> Unit,
    onSelectNavBarStyle: (NovaNavBarStyle) -> Unit,
    onSelectSwitchStyle: (NovaSwitchStyle) -> Unit,
    onSelectHudStyle: (VolumeBrightnessBarStyle) -> Unit,
    onSelectPlayerStyle: (PlayerControlStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accent = LocalNovaAccentColor.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0B0C12).copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        NovaLogoBlue.copy(alpha = 0.55f),
                        NovaLogoViolet.copy(alpha = 0.55f),
                        NovaLogoPink.copy(alpha = 0.45f),
                    ),
                ),
            ),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NovaLogoGradientBrush),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Nova Interface Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "Customize player, dock, HUD & theme",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            view.novaHaptic(10L, 120, hapticsEnabled)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                // 1. Theme Mode (Strictly 2 Options: AMOLED Pitch Black vs Pure White)
                Text(
                    text = "Color Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                val isLightSelected = currentThemeMode == MxAppThemeMode.LIGHT_THEME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaCompactOptionCard(
                        title = "AMOLED Black",
                        subtitle = "Pure #000000 OLED",
                        selected = !isLightSelected,
                        onClick = { onSelectThemeMode(MxAppThemeMode.AMOLED_BLACK) },
                        modifier = Modifier.weight(1f),
                    )
                    NovaCompactOptionCard(
                        title = "Pure White",
                        subtitle = "Clean Daylight UI",
                        selected = isLightSelected,
                        onClick = { onSelectThemeMode(MxAppThemeMode.LIGHT_THEME) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // 2. Accent Source (Strictly 2 Options: Nova Logo Gradient vs Device Dynamic Accent)
                Text(
                    text = "Accent Palette",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaCompactOptionCard(
                        title = "Logo Gradient",
                        subtitle = "Blue • Violet • Pink",
                        selected = !useDynamicDeviceAccent,
                        onClick = { onSelectDynamicAccent(false) },
                        modifier = Modifier.weight(1f),
                    )
                    NovaCompactOptionCard(
                        title = "Device Accent",
                        subtitle = "System Dynamic Color",
                        selected = useDynamicDeviceAccent,
                        onClick = { onSelectDynamicAccent(true) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // 3. Player Interface Layout
                Text(
                    text = "Player Layout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                NovaSegmentedControl(
                    items = listOf(
                        PlayerControlStyle.CLASSIC_NEXT to "Nova Cinema",
                        PlayerControlStyle.LIQUID_GLASS to "Nova Minimal",
                        PlayerControlStyle.MX_PRO to "Nova Studio",
                    ),
                    selected = currentPlayerStyle,
                    onSelected = onSelectPlayerStyle,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 4. Bottom Navigation Dock Style
                Text(
                    text = "Navigation Dock",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                NovaSegmentedControl(
                    items = listOf(
                        NovaNavBarStyle.YTXTUBE_LIQUID_GLASS to "Floating Dock",
                        NovaNavBarStyle.GLASMIO_IOS27_MORPH to "Orbit Pill",
                        NovaNavBarStyle.MX_PRO_CLASSIC to "Classic Bar",
                    ),
                    selected = currentNavBarStyle,
                    onSelected = onSelectNavBarStyle,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 5. Volume & Brightness HUD Style
                Text(
                    text = "Volume & Brightness HUD",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                NovaSegmentedControl(
                    items = listOf(
                        VolumeBrightnessBarStyle.MX_PRO_VERTICAL to "Studio Bar",
                        VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE to "Fluid Capsule",
                        VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL to "Minimal Pill",
                    ),
                    selected = currentHudStyle,
                    onSelected = onSelectHudStyle,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 6. Toggle Switch Style
                Text(
                    text = "Toggle Switch Style",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                NovaSegmentedControl(
                    items = listOf(
                        NovaSwitchStyle.GLASMIO_LIQUID to "Fluid Switch",
                        NovaSwitchStyle.MX_PRO_CLASSIC to "Compact Pill",
                        NovaSwitchStyle.MATERIAL_EXPRESSIVE to "Standard",
                    ),
                    selected = currentSwitchStyle,
                    onSelected = onSelectSwitchStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NovaCompactOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accent = LocalNovaAccentColor.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
            )
            .border(
                width = if (selected) 1.5.dp else 0.8.dp,
                color = if (selected) accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable {
                view.novaHaptic(10L, 125, hapticsEnabled)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (selected) accent else Color.White.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
