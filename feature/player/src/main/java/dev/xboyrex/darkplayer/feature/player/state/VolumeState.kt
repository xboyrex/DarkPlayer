package dev.xboyrex.darkplayer.feature.player.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.xboyrex.darkplayer.feature.player.service.getLoudnessGain
import dev.xboyrex.darkplayer.feature.player.service.getIsLoudnessGainSupported
import dev.xboyrex.darkplayer.feature.player.service.setLoudnessGain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

@Composable
fun rememberVolumeState(
    player: Player?,
    showVolumePanelIfHeadsetIsOn: Boolean,
    enableVolumeBoost: Boolean = false,
): VolumeState {
    val activity = LocalActivity.current
    val volumeState = remember {
        VolumeState(
            player = player,
            activity = activity!!,
            showVolumePanelIfHeadsetIsOn = showVolumePanelIfHeadsetIsOn,
            enableVolumeBoost = enableVolumeBoost,
        )
    }
    DisposableEffect(key1 = activity) { volumeState.handleListeners(disposableEffectScope = this) }
    LaunchedEffect(
        key1 = player,
        key2 = enableVolumeBoost,
    ) { volumeState.initializeVolumeBoost() }
    return volumeState
}

@Stable
class VolumeState(
    private val player: Player?,
    private val activity: Context,
    private val showVolumePanelIfHeadsetIsOn: Boolean,
    private val enableVolumeBoost: Boolean = false,
) {
    private val updateScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var observedController: MediaController? = null
    private val audioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            observedController?.let { controller ->
                updateScope.launch { updateVolumeBoostState(player = controller) }
            }
        }
    }
    private val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val systemVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private val isHeadsetConnected: Boolean
        get() = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn

    private val systemMaxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var isLoudnessGainSupported: Boolean by mutableStateOf(value = false)

    val maxVolume: Int
        get() = if (isLoudnessGainSupported) systemMaxVolume * 2 else systemMaxVolume

    val maxVolumePercentage: Int
        get() = if (isLoudnessGainSupported) {
            MAX_VOLUME_PERCENTAGE_BOOST
        } else {
            MAX_VOLUME_PERCENTAGE_NORMAL
        }

    var currentVolume: Int by mutableIntStateOf(value = systemVolume)
        private set

    var volumePercentage: Int by mutableIntStateOf(value = calculateVolumePercentage())
        private set

    fun updateVolumePercentage(percentage: Int) {
        val clampedPercentage = percentage.coerceIn(
            minimumValue = 0,
            maximumValue = maxVolumePercentage,
        )
        val newVolume = (clampedPercentage * systemMaxVolume) / 100
        setVolume(volume = newVolume)
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult =
        with(disposableEffectScope) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == VOLUME_CHANGED_ACTION) {
                        val newSystemVolume = systemVolume
                        if (currentVolume <= systemMaxVolume) {
                            currentVolume = newSystemVolume
                            volumePercentage = calculateVolumePercentage()
                        } else if (newSystemVolume < systemMaxVolume) {
                            currentVolume = newSystemVolume
                            volumePercentage = calculateVolumePercentage()
                            applyVolumeBoost(volume = 0)
                        }
                    }
                }
            }

            activity.registerReceiver(
                receiver,
                IntentFilter(VOLUME_CHANGED_ACTION),
            )

            onDispose {
                activity.unregisterReceiver(receiver)
                observedController?.removeListener(audioSessionListener)
                observedController = null
                updateScope.cancel()
            }
        }

    suspend fun initializeVolumeBoost() {
        val player = player as? MediaController ?: return
        observedController?.removeListener(audioSessionListener)
        observedController = player
        player.addListener(audioSessionListener)
        updateVolumeBoostState(player = player)
    }

    private suspend fun updateVolumeBoostState(player: MediaController) {
        if (!enableVolumeBoost) {
            isLoudnessGainSupported = false
            return
        }

        try {
            isLoudnessGainSupported = player.getIsLoudnessGainSupported()
            if (!isLoudnessGainSupported) return

            val loudnessGain = player.getLoudnessGain()

            if (systemVolume < systemMaxVolume) {
                currentVolume = systemVolume
                volumePercentage = calculateVolumePercentage()
                player.setLoudnessGain(gain = 0)
            } else {
                val boostVolume = (loudnessGain * systemMaxVolume) / MAX_BOOST_GAIN_MB
                currentVolume = systemMaxVolume + boostVolume
                volumePercentage = calculateVolumePercentage()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isLoudnessGainSupported = false
        }
    }

    private fun calculateVolumePercentage(): Int =
        ((currentVolume.toFloat() / systemMaxVolume) * 100).toInt()

    private fun setVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(
            minimumValue = 0,
            maximumValue = maxVolume,
        )
        currentVolume = clampedVolume
        volumePercentage = calculateVolumePercentage()

        if (clampedVolume <= systemMaxVolume) {
            setSystemVolume(
                volume = clampedVolume,
                showVolumePanel = showVolumePanelIfHeadsetIsOn && isHeadsetConnected,
            )
            applyVolumeBoost(volume = 0)
        } else {
            setSystemVolume(
                volume = systemMaxVolume,
                showVolumePanel = false,
            )
            applyVolumeBoost(volume = clampedVolume - systemMaxVolume)
        }
    }

    private fun setSystemVolume(volume: Int, showVolumePanel: Boolean) {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume.coerceIn(
                minimumValue = 0,
                maximumValue = systemMaxVolume,
            ),
            if (showVolumePanel) AudioManager.FLAG_SHOW_UI else 0,
        )
    }

    private fun applyVolumeBoost(volume: Int) {
        val player = player as? MediaController ?: return
        val gainMillibels = (volume.toFloat() / systemMaxVolume * MAX_BOOST_GAIN_MB).toInt()

        try {
            player.setLoudnessGain(gain = gainMillibels)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun increaseVolume(showVolumePanel: Boolean = false) {
        setVolume(volume = currentVolume + 1)
        if (showVolumePanel && currentVolume <= systemMaxVolume) {
            setSystemVolume(
                volume = currentVolume,
                showVolumePanel = true,
            )
        }
    }

    fun decreaseVolume(showVolumePanel: Boolean = false) {
        setVolume(volume = currentVolume - 1)
        if (showVolumePanel && currentVolume <= systemMaxVolume) {
            setSystemVolume(
                volume = currentVolume,
                showVolumePanel = true,
            )
        }
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val MAX_VOLUME_PERCENTAGE_NORMAL = 100
        private const val MAX_VOLUME_PERCENTAGE_BOOST = 200
        private const val MAX_BOOST_GAIN_MB = 2000
    }
}
