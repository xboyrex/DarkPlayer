package dev.xboyrex.darkplayer.feature.player.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextOutlinedTextField
import dev.xboyrex.darkplayer.feature.player.extensions.getName
import dev.xboyrex.darkplayer.feature.player.state.SubtitleOptionsEvent
import dev.xboyrex.darkplayer.feature.player.state.rememberSubtitleOptionsState
import dev.xboyrex.darkplayer.feature.player.state.rememberTracksState
import kotlin.math.roundToLong
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.SubtitleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onSelectSubtitleClick: () -> Unit,
    onEvent: (SubtitleOptionsEvent) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val subtitleTracksState = rememberTracksState(
        player = player,
        C.TRACK_TYPE_TEXT,
    )
    val subtitleOptionsState = rememberSubtitleOptionsState(
        player = player,
        onEvent = onEvent,
    )

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Subtitle",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Subtitle tracks list inside iOS rounded card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                subtitleTracksState.tracks.forEachIndexed { index, track ->
                    RadioButtonRow(
                        selected = track.isSelected,
                        text = track.mediaTrackGroup.getName(
                            C.TRACK_TYPE_TEXT,
                            index = index,
                        ),
                        onClick = {
                            subtitleTracksState.switchTrack(index = index)
                            onDismiss()
                        },
                    )
                }
                RadioButtonRow(
                    selected = subtitleTracksState.tracks.none { it.isSelected },
                    text = stringResource(R.string.disable),
                    onClick = {
                        subtitleTracksState.switchTrack(index = -1)
                        onDismiss()
                    },
                )
            }

            // 2. Open & Online Subtitles iOS rows (matching 24075_c798dc.png)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectSubtitleClick()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Open",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(19.dp),
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val rawTitle = player.mediaMetadata.displayTitle?.toString()
                                ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
                                ?: ""
                            val cleanQuery = rawTitle
                                .substringBeforeLast(".")
                                .replace(Regex("[._\\-]+"), " ")
                                .trim()
                            val searchUrl = "https://www.opensubtitles.org/en/search2/moviename-" +
                                Uri.encode(cleanQuery.ifBlank { "movie" })
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Online subtitles",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "Search & download matching subtitles online",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            // 3. Subtitle Delay & Speed controls inside iOS card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DelayInput(
                    value = subtitleOptionsState.delayMilliseconds,
                    onValueChange = { subtitleOptionsState.setDelay(delayMillis = it) },
                )
                SpeedInput(
                    value = subtitleOptionsState.speedMultiplier,
                    onValueChange = { subtitleOptionsState.setSpeed(speed = it) },
                )
            }
        }
    }
}

@Composable
private fun DelayInput(value: Long, onValueChange: (Long) -> Unit) {
    var valueString by remember {
        mutableStateOf(value = if (value == 0L) "0" else "%.2f".format(value / 1000.0))
    }

    LaunchedEffect(key1 = value) {
        val currentValue = valueString.toDoubleOrNull() ?: 0.0
        if (currentValue == (value / 1000.0)) return@LaunchedEffect
        valueString = if (value == 0L) "0" else "%.2f".format(value / 1000.0)
    }

    NumberChooserInput(
        title = stringResource(R.string.delay),
        value = valueString,
        suffix = { Text(text = "sec") },
        onValueChange = { newValue ->
            if (newValue.isBlank()) {
                valueString = ""
                onValueChange(0)
                return@NumberChooserInput
            }

            val cleanedValue = newValue.trimStart()

            if (cleanedValue == "-" || cleanedValue == ".") {
                valueString = cleanedValue
                return@NumberChooserInput
            }

            val decimalPattern = "^-?\\d*\\.?\\d{0,2}$".toRegex()
            if (!cleanedValue.matches(regex = decimalPattern)) {
                return@NumberChooserInput
            }

            valueString = cleanedValue

            runCatching {
                val doubleValue = cleanedValue.toDoubleOrNull() ?: 0.0
                val milliseconds = (doubleValue * 1000).roundToLong()
                onValueChange(milliseconds)
            }
        },
        onIncrement = { onValueChange(value + 100) },
        onDecrement = { onValueChange(value - 100) },
    )
}

@Composable
private fun SpeedInput(value: Float, onValueChange: (Float) -> Unit) {
    var valueString by remember {
        mutableStateOf(value = if (value == 1f) "1" else "%.2f".format(value))
    }

    LaunchedEffect(key1 = value) {
        val currentValue = valueString.toFloatOrNull() ?: 0.0
        if (currentValue == value) return@LaunchedEffect
        valueString = if (value == 1f) "1" else "%.2f".format(value)
    }

    NumberChooserInput(
        title = stringResource(R.string.speed),
        value = valueString,
        suffix = { Text(text = "x") },
        onValueChange = { newValue ->
            if (newValue.isBlank()) {
                valueString = ""
                onValueChange(1f)
                return@NumberChooserInput
            }

            val cleanedValue = newValue.trimStart()

            if (cleanedValue == ".") {
                valueString = cleanedValue
                return@NumberChooserInput
            }

            val decimalPattern = "^\\d*\\.?\\d{0,2}$".toRegex()
            if (!cleanedValue.matches(regex = decimalPattern)) {
                return@NumberChooserInput
            }

            valueString = cleanedValue

            runCatching {
                val floatValue = cleanedValue.toFloatOrNull() ?: 1f
                onValueChange(floatValue)
            }
        },
        onIncrement = { onValueChange(value + 0.1f) },
        onDecrement = { onValueChange(value - 0.1f) },
    )
}

@Composable
private fun NumberChooserInput(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    suffix: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilledTonalIconButton(
            onClick = {},
            modifier = Modifier.repeatingClickable(onClick = onDecrement),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = null,
            )
        }
        NextOutlinedTextField(
            label = { Text(text = title) },
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(weight = 1f),
            suffix = suffix,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
        )
        FilledTonalIconButton(
            onClick = {},
            modifier = Modifier.repeatingClickable(onClick = onIncrement),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            )
        }
    }
}

private fun Modifier.repeatingClickable(
    enabled: Boolean = true,
    maxDelayMillis: Long = 200,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .20f,
    onClick: () -> Unit,
): Modifier = composed {
    val updatedOnClick by rememberUpdatedState(newValue = onClick)

    this.pointerInput(key1 = enabled) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val heldButtonJob = launch {
                    var currentDelayMillis = maxDelayMillis
                    while (enabled && down.pressed) {
                        updatedOnClick()
                        delay(timeMillis = currentDelayMillis)
                        val nextMillis =
                            currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                        currentDelayMillis =
                            nextMillis.toLong().coerceAtLeast(minimumValue = minDelayMillis)
                    }
                }
                waitForUpOrCancellation()
                heldButtonJob.cancel()
            }
        }
    }
}
