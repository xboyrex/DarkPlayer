package dev.xboyrex.darkplayer.feature.player.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

@Composable
fun BoxScope.SleepTimerSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedMinutes by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedMinutes) {
        if (selectedMinutes > 0) {
            remainingSeconds = selectedMinutes * 60
            while (remainingSeconds > 0) {
                delay(1_000L)
                remainingSeconds -= 1
            }
            if (selectedMinutes > 0) {
                player.pause()
                selectedMinutes = 0
                Toast.makeText(context, "Sleep timer paused playback", Toast.LENGTH_SHORT).show()
            }
        } else {
            remainingSeconds = 0
        }
    }

    val presets = listOf(0, 10, 15, 30, 45, 60, 90, 120)

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Sleep timer",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (remainingSeconds > 0) {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Auto-pause active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            FlowRow(
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { minutes ->
                    val isSelected = selectedMinutes == minutes
                    val pillShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.06f),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.42f)
                                else Color.White.copy(alpha = 0.12f),
                                shape = pillShape,
                            )
                            .clickable {
                                selectedMinutes = minutes
                                val msg = if (minutes == 0) "Sleep timer turned off" else "Sleep timer set for $minutes min"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (minutes == 0) "Off" else "${minutes}m",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
