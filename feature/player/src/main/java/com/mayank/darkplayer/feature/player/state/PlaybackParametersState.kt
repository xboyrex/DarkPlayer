package com.mayank.darkplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.mayank.darkplayer.feature.player.service.getSkipSilenceEnabled
import com.mayank.darkplayer.feature.player.service.setSkipSilenceEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberPlaybackParametersState(player: Player): PlaybackParametersState {
    val scope = rememberCoroutineScope()
    val playbackParametersState = remember(player) {
        PlaybackParametersState(
            player = player,
            scope = scope,
        )
    }
    LaunchedEffect(key1 = player) { playbackParametersState.observe() }
    return playbackParametersState
}

@UnstableApi
class PlaybackParametersState(private val player: Player, private val scope: CoroutineScope) {
    var speed: Float by mutableFloatStateOf(value = 1f)
        private set

    var skipSilenceEnabled: Boolean by mutableStateOf(value = false)
        private set

    /**
     * Applies playback speed while strictly locking audio pitch to 1.0f so 2.0x
     * and high-speed playback never introduces chipmunk/pitch distortion.
     */
    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(safeSpeed, 1.0f)
        this.speed = safeSpeed
    }

    fun toggle2xSpeed() {
        val target = if (speed >= 1.95f) 1.0f else 2.0f
        setPlaybackSpeed(target)
    }

    fun setIsSkipSilenceEnabled(enabled: Boolean) {
        scope.launch {
            when (player) {
                is MediaController -> player.setSkipSilenceEnabled(enabled = enabled)
                is ExoPlayer -> player.skipSilenceEnabled = enabled
                else -> return@launch
            }
            updateSkipSilenceEnabled()
        }
    }

    suspend fun observe() {
        updateSpeed()
        updateSkipSilenceEnabled()

        player.listen { events ->
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                updateSpeed()
            }
        }
    }

    private fun updateSpeed() {
        val params = player.playbackParameters
        speed = params.speed
        if (params.pitch != 1.0f) {
            player.playbackParameters = PlaybackParameters(params.speed, 1.0f)
        }
    }

    private fun updateSkipSilenceEnabled() {
        scope.launch {
            skipSilenceEnabled = when (player) {
                is MediaController -> player.getSkipSilenceEnabled()
                is ExoPlayer -> player.skipSilenceEnabled
                else -> return@launch
            }
        }
    }
}
