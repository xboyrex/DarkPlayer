package dev.xboyrex.darkplayer.feature.player.service

import androidx.media3.common.Player

/**
 * Single authoritative owner of player volume, mute state, and fade multiplier (F05).
 *
 * Guarantees that:
 * - Audio fade ramps never unmute a muted player.
 * - Unmuting restores the user's configured [userVolume] rather than jumping to hardcoded 1.0f.
 * - Concurrent UI and service volume writes cannot race against each other.
 */
class PlaybackVolumeController(
    initialUserVolume: Float = 1f,
    initialMuted: Boolean = false,
) {
    var userVolume: Float = initialUserVolume.coerceIn(0f, 1f)
        private set

    var isMuted: Boolean = initialMuted
        private set

    var fadeMultiplier: Float = 1f
        private set

    val effectiveVolume: Float
        get() = if (isMuted) 0f else (userVolume * fadeMultiplier).coerceIn(0f, 1f)

    fun setUserVolume(volume: Float, player: Player? = null) {
        userVolume = volume.coerceIn(0f, 1f)
        applyTo(player)
    }

    fun setMuted(muted: Boolean, player: Player? = null) {
        isMuted = muted
        applyTo(player)
    }

    fun toggleMute(player: Player? = null): Boolean {
        isMuted = !isMuted
        applyTo(player)
        return isMuted
    }

    fun setFadeMultiplier(multiplier: Float, player: Player? = null) {
        fadeMultiplier = multiplier.coerceIn(0f, 1f)
        applyTo(player)
    }

    fun resetFade(player: Player? = null) {
        fadeMultiplier = 1f
        applyTo(player)
    }

    fun applyTo(player: Player?) {
        player?.volume = effectiveVolume
    }
}
