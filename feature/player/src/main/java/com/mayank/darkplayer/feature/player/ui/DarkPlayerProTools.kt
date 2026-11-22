package com.mayank.darkplayer.feature.player.ui

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.ui.theme.NavyBorder
import com.mayank.darkplayer.core.ui.theme.NavySurface
import com.mayank.darkplayer.core.ui.theme.NavySurfaceSecondary
import com.mayank.darkplayer.core.ui.theme.NavyTextPrimary
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon
import kotlinx.coroutines.delay

@Stable
class DarkPlayerProState {
    var nightModeEnabled by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    var savedVolume by mutableFloatStateOf(1f)

    // A-B Repeat
    var abPointA by mutableStateOf<Long?>(null)
    var abPointB by mutableStateOf<Long?>(null)

    // Sleep Timer (remaining seconds, 0 = off)
    var sleepTimerRemainingSec by mutableIntStateOf(0)
    var showSleepDialog by mutableStateOf(false)

    fun toggleMute(player: Player?) {
        if (player == null) return
        if (isMuted) {
            player.volume = savedVolume.coerceIn(0.1f, 1f)
            isMuted = false
        } else {
            savedVolume = if (player.volume > 0f) player.volume else 1f
            player.volume = 0f
            isMuted = true
        }
    }

    fun cycleAbRepeat(player: Player?): String {
        val pos = player?.currentPosition ?: return "A-B Repeat unavailable"
        return when {
            abPointA == null -> {
                abPointA = pos
                abPointB = null
                "A-B Repeat: Point A set"
            }
            abPointB == null -> {
                val start = abPointA ?: 0L
                if (pos > start + 500L) {
                    abPointB = pos
                    "A-B Repeat: Looping A to B"
                } else {
                    abPointA = null
                    abPointB = null
                    "A-B Repeat: Cleared"
                }
            }
            else -> {
                abPointA = null
                abPointB = null
                "A-B Repeat: Off"
            }
        }
    }
}

@Composable
fun rememberDarkPlayerProState(): DarkPlayerProState = remember { DarkPlayerProState() }

@OptIn(UnstableApi::class)
@Composable
fun DarkPlayerProEffects(
    player: Player?,
    proState: DarkPlayerProState,
) {
    // A-B Repeat loop monitor
    LaunchedEffect(player, proState.abPointA, proState.abPointB) {
        val a = proState.abPointA
        val b = proState.abPointB
        if (player != null && a != null && b != null && b > a) {
            while (true) {
                if (player.currentPosition >= b) {
                    player.seekTo(a)
                }
                delay(200L)
            }
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(player, proState.sleepTimerRemainingSec > 0) {
        while (proState.sleepTimerRemainingSec > 0) {
            delay(1000L)
            proState.sleepTimerRemainingSec = (proState.sleepTimerRemainingSec - 1).coerceAtLeast(0)
            if (proState.sleepTimerRemainingSec == 0) {
                player?.pause()
            }
        }
    }
}

/**
 * Circular Pro Quick-Tools bar (Night, Mute, Sleep, A-B Repeat).
 * Equalizer removed per user request; circular buttons match bottom controls.
 */
@Composable
fun ProQuickToolsStrip(
    player: Player?,
    proState: DarkPlayerProState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sleepMin = (proState.sleepTimerRemainingSec + 59) / 60
    val abActive = proState.abPointA != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProCircleButton(
            icon = Icons.Rounded.Bedtime,
            contentDescription = "Night Mode",
            active = proState.nightModeEnabled,
            badgeText = null,
            onClick = {
                proState.nightModeEnabled = !proState.nightModeEnabled
                Toast.makeText(
                    context,
                    if (proState.nightModeEnabled) "Night Mode ON" else "Night Mode OFF",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        ProCircleButton(
            icon = if (proState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = "Mute",
            active = proState.isMuted,
            badgeText = null,
            onClick = { proState.toggleMute(player) },
        )
        ProCircleButton(
            icon = Icons.Rounded.Timer,
            contentDescription = "Sleep Timer",
            active = proState.sleepTimerRemainingSec > 0,
            badgeText = if (proState.sleepTimerRemainingSec > 0) "${sleepMin}m" else null,
            onClick = { proState.showSleepDialog = true },
        )
        ProCircleButton(
            icon = Icons.Rounded.Contrast,
            contentDescription = "Player Button Theme",
            active = !DarkPlayerUiPrefs.playerButtonsDark,
            badgeText = null,
            onClick = {
                val nowDark = DarkPlayerUiPrefs.togglePlayerButtonsDark(context)
                Toast.makeText(
                    context,
                    if (nowDark) "Player buttons: Dark" else "Player buttons: White",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        ProCircleButton(
            icon = Icons.Rounded.Repeat,
            contentDescription = "A-B Repeat",
            active = abActive,
            badgeText = when {
                proState.abPointA != null && proState.abPointB != null -> "A-B"
                proState.abPointA != null -> "A"
                else -> null
            },
            onClick = {
                val msg = proState.cycleAbRepeat(player)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun ProCircleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
) {
    val isDarkBtn = DarkPlayerUiPrefs.playerButtonsDark
    val bgColor = if (active) MaterialTheme.colorScheme.primary else if (isDarkBtn) Color(0xCC121614) else Color(0xE8F4EFE8)
    val fgColor = if (active) MaterialTheme.colorScheme.onPrimary else if (isDarkBtn) Color(0xFFF4EFE8) else Color(0xFF161B17)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (badgeText != null) {
            Text(
                text = badgeText,
                color = fgColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = fgColor,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
fun DarkPlayerSleepTimerDialog(proState: DarkPlayerProState) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = proState.showSleepDialog,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { proState.showSleepDialog = false },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 360.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavySurface)
                    
                    .clickable(enabled = false) {}
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sleep Timer",
                        color = NavyTextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = NavyTextPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { proState.showSleepDialog = false },
                    )
                }
                Spacer(Modifier.height(14.dp))
                val options = listOf(0 to "Off", 10 to "10 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { (mins, label) ->
                            val isSelected = if (mins == 0) {
                                proState.sleepTimerRemainingSec == 0
                            } else {
                                proState.sleepTimerRemainingSec in ((mins - 1) * 60 + 1)..(mins * 60)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else NavySurfaceSecondary)
                                    .clickable {
                                        proState.sleepTimerRemainingSec = mins * 60
                                        proState.showSleepDialog = false
                                        Toast.makeText(
                                            context,
                                            if (mins == 0) "Sleep Timer Off" else "Sleep Timer set: $mins min",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NavyTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
