package dev.xboyrex.darkplayer.feature.player.ui

import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.common.extensions.round
import dev.xboyrex.darkplayer.core.common.extensions.toString
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.feature.player.state.rememberPlaybackParametersState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MIN_SPEED = 0.25f
private const val MAX_SPEED = 4.0f
private const val SPEED_STEP = 0.05f
private val RULER_STEP_WIDTH = 12.dp
private val LAST_SPEED_STEP = ((MAX_SPEED - MIN_SPEED) / SPEED_STEP).roundToInt()

private fun speedAtStep(step: Int): Float =
    (MIN_SPEED + step.coerceIn(0, LAST_SPEED_STEP) * SPEED_STEP).round(2)

private fun stepForSpeed(speed: Float): Int =
    ((speed.coerceIn(MIN_SPEED, MAX_SPEED) - MIN_SPEED) / SPEED_STEP).roundToInt()

private fun snapSpeed(speed: Float): Float = speedAtStep(stepForSpeed(speed))

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
) {
    val playbackParametersState = rememberPlaybackParametersState(player = player)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val haptics = remember(view) { SpeedRulerHaptics(view) }
    val pxPerUnit = with(LocalDensity.current) { RULER_STEP_WIDTH.toPx() } / SPEED_STEP

    val value = remember {
        Animatable(playbackParametersState.speed.coerceIn(MIN_SPEED, MAX_SPEED)).apply {
            updateBounds(MIN_SPEED, MAX_SPEED)
        }
    }
    var driving by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val generation = remember { intArrayOf(0) }

    LaunchedEffect(playbackParametersState.speed) {
        if (!driving) {
            value.snapTo(playbackParametersState.speed.coerceIn(MIN_SPEED, MAX_SPEED))
        }
    }

    LaunchedEffect(Unit) {
        var last = stepForSpeed(value.value)
        snapshotFlow { stepForSpeed(value.value) }.collect { step ->
            if (step == last) return@collect
            last = step
            if (!driving) return@collect
            haptics.step(step)
            playbackParametersState.setPlaybackSpeed(speedAtStep(step))
        }
    }

    fun drive(block: suspend () -> Unit) {
        job?.cancel()
        val mine = ++generation[0]
        job = scope.launch {
            driving = true
            try {
                block()
            } finally {
                if (generation[0] == mine) {
                    playbackParametersState.setPlaybackSpeed(snapSpeed(value.value))
                    driving = false
                }
            }
        }
    }

    fun glideTo(speed: Float) = drive {
        value.animateTo(
            targetValue = speed.coerceIn(MIN_SPEED, MAX_SPEED),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        )
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Big Speed Readout + Fine Step Controls + 1x Reset Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val newSpeed = (playbackParametersState.speed - SPEED_STEP)
                            .coerceAtLeast(MIN_SPEED)
                            .round(2)
                        playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = null,
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = playbackParametersState.speed.toString(decimalPlaces = 2),
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-1.2).sp,
                        color = Color.White,
                    )
                    Text(
                        text = "×",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val newSpeed = (playbackParametersState.speed + SPEED_STEP)
                                .coerceAtMost(MAX_SPEED)
                                .round(2)
                            playbackParametersState.setPlaybackSpeed(speed = newSpeed)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                        )
                    }
                    val atOne = abs(playbackParametersState.speed - 1.0f) < 0.02f
                    val resetAlpha by animateFloatAsState(
                        targetValue = if (atOne) 0.38f else 1f,
                        animationSpec = tween(200),
                        label = "resetSpeedAlpha",
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer { alpha = resetAlpha }
                            .height(38.dp)
                            .pressable(enabled = !atOne) { glideTo(1.0f) }
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(19.dp),
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "1×",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }

            // Interactive Haptic Speed Ruler Canvas
            val tickColor = Color.White.copy(alpha = 0.65f)
            val nearColor = Color.White
            val needleColor = MaterialTheme.colorScheme.primary
            val measurer = rememberTextMeasurer()
            val labels = remember(measurer) {
                val style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                listOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f).map { s ->
                    s to measurer.measure("${s}×", style)
                }
            }
            val fade = remember {
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0f),
                    0.18f to Color.Black,
                    0.82f to Color.Black,
                    1f to Color.Black.copy(alpha = 0f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(vertical = 8.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .draggable(
                            state = rememberDraggableState { delta ->
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    value.snapTo(
                                        (value.value - delta / pxPerUnit).coerceIn(
                                            MIN_SPEED,
                                            MAX_SPEED,
                                        ),
                                    )
                                }
                            },
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                job?.cancel()
                                generation[0]++
                                driving = true
                            },
                            onDragStopped = { velocity ->
                                val v = -velocity / pxPerUnit
                                drive {
                                    if (abs(v) > 0.05f) {
                                        value.animateDecay(
                                            v,
                                            exponentialDecay(frictionMultiplier = 2f),
                                        )
                                    }
                                    value.animateTo(
                                        snapSpeed(value.value),
                                        spring(dampingRatio = 1f, stiffness = 900f),
                                    )
                                }
                            },
                        )
                        .pointerInput(pxPerUnit) {
                            detectTapGestures { offset ->
                                glideTo(
                                    snapSpeed(
                                        value.value + (offset.x - size.width / 2f) / pxPerUnit,
                                    ),
                                )
                            }
                        }
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(fade, blendMode = BlendMode.DstIn)
                        },
                ) {
                    val v = value.value
                    val cx = size.width / 2f
                    val stroke = 2.dp.toPx()
                    for (i in 0..LAST_SPEED_STEP) {
                        val x = cx + (speedAtStep(i) - v) * pxPerUnit
                        if (x < -stroke || x > size.width + stroke) continue
                        val (top, length, alpha) = when {
                            i % 10 == 0 -> Triple(6.dp, 26.dp, 1f)
                            i % 5 == 0 -> Triple(9.dp, 20.dp, 0.72f)
                            else -> Triple(11.dp, 12.dp, 0.42f)
                        }
                        drawLine(
                            color = tickColor.copy(alpha = alpha),
                            start = Offset(x, top.toPx()),
                            end = Offset(x, (top + length).toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                    labels.forEach { (s, layout) ->
                        val x = cx + (s - v) * pxPerUnit
                        if (x < -layout.size.width || x > size.width + layout.size.width) {
                            return@forEach
                        }
                        drawText(
                            textLayoutResult = layout,
                            color = if (abs(s - v) < 0.13f) nearColor else tickColor,
                            topLeft = Offset(x - layout.size.width / 2f, 38.dp.toPx()),
                        )
                    }
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(cx - 5.dp.toPx(), 0f),
                        size = Size(10.dp.toPx(), 40.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                    )
                    drawRoundRect(
                        color = needleColor,
                        topLeft = Offset(cx - 2.dp.toPx(), 3.dp.toPx()),
                        size = Size(4.dp.toPx(), 34.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }

            // Quick Preset Chips
            FlowRow(
                maxItemsInEachRow = 5,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    0.25f,
                    0.5f,
                    0.75f,
                    1.0f,
                    1.25f,
                    1.5f,
                    1.75f,
                    2.0f,
                    2.5f,
                    3.0f,
                    3.5f,
                    4.0f,
                ).forEach { speed ->
                    val isSelectedSpeed = abs(playbackParametersState.speed - speed) < 0.02f
                    Box(
                        modifier = Modifier
                            .clip(shape = CircleShape)
                            .background(
                                if (isSelectedSpeed) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                } else {
                                    Color.White.copy(alpha = 0.06f)
                                },
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelectedSpeed) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.14f)
                                },
                                shape = CircleShape,
                            )
                            .clickable { glideTo(speed) }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp,
                            )
                            .weight(weight = 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedSpeed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            },
                        )
                    }
                }
            }

            // Skip Silence Toggle
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .toggleable(
                        value = playbackParametersState.skipSilenceEnabled,
                        onValueChange = {
                            playbackParametersState.setIsSkipSilenceEnabled(enabled = it)
                        },
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.skip_silence),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(weight = 1f),
                )
                NextSwitch(
                    checked = playbackParametersState.skipSilenceEnabled,
                    onCheckedChange = null,
                )
            }
        }
    }
}

private class SpeedRulerHaptics(private val view: View) {
    private enum class Kind { Step, Quarter, One, End }

    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= 31 ->
            view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else -> @Suppress("DEPRECATION") view.context.getSystemService(Vibrator::class.java)
    }
    private val primitives = Build.VERSION.SDK_INT >= 30 &&
        vibrator?.hasVibrator() == true &&
        vibrator.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
        )
    private val oneStep = stepForSpeed(1.0f)
    private var lastAt = 0L

    fun step(step: Int) {
        val kind = when {
            step == 0 || step == LAST_SPEED_STEP -> Kind.End
            step == oneStep -> Kind.One
            step % 5 == 0 -> Kind.Quarter
            else -> Kind.Step
        }
        val now = SystemClock.uptimeMillis()
        if (kind == Kind.Step && now - lastAt < 22L) return
        lastAt = now
        if (primitives && Build.VERSION.SDK_INT >= 30) play(kind) else fallback(kind)
    }

    @RequiresApi(30)
    private fun play(kind: Kind) {
        val (primitive, scale) = when (kind) {
            Kind.Step -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK to 0.45f
            Kind.Quarter -> VibrationEffect.Composition.PRIMITIVE_TICK to 0.55f
            Kind.One -> VibrationEffect.Composition.PRIMITIVE_CLICK to 0.6f
            Kind.End -> VibrationEffect.Composition.PRIMITIVE_CLICK to 0.9f
        }
        val effect = VibrationEffect.startComposition().addPrimitive(primitive, scale).compose()
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            v.vibrate(effect)
        }
    }

    private fun fallback(kind: Kind) {
        val constant = when {
            Build.VERSION.SDK_INT >= 34 -> when (kind) {
                Kind.Step -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                Kind.Quarter, Kind.One -> HapticFeedbackConstants.SEGMENT_TICK
                Kind.End -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            }
            kind == Kind.End -> HapticFeedbackConstants.CONTEXT_CLICK
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }
}
