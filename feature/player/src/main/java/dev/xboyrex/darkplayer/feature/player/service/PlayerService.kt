package dev.xboyrex.darkplayer.feature.player.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.BundleListRetriever
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dev.xboyrex.darkplayer.core.common.Logger
import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.common.extensions.getFilenameFromUri
import dev.xboyrex.darkplayer.core.common.extensions.getLocalSubtitles
import dev.xboyrex.darkplayer.core.common.extensions.getPath
import dev.xboyrex.darkplayer.core.data.repository.MediaRepository
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.media.network.NetworkUri
import dev.xboyrex.darkplayer.core.media.network.datasource.NextDataSourceFactory
import dev.xboyrex.darkplayer.core.model.LoopMode
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.Resume
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.feature.player.PlayerActivity
import dev.xboyrex.darkplayer.feature.player.R
import dev.xboyrex.darkplayer.feature.player.extensions.addAdditionalSubtitleConfiguration
import dev.xboyrex.darkplayer.feature.player.extensions.audioDecoderMode
import dev.xboyrex.darkplayer.feature.player.extensions.audioTrackIndex
import dev.xboyrex.darkplayer.feature.player.extensions.copy
import dev.xboyrex.darkplayer.feature.player.extensions.externalAudio
import dev.xboyrex.darkplayer.feature.player.extensions.externalAudioIndex
import dev.xboyrex.darkplayer.feature.player.extensions.getManuallySelectedTrackIndex
import dev.xboyrex.darkplayer.feature.player.extensions.positionMs
import dev.xboyrex.darkplayer.feature.player.extensions.setExtras
import dev.xboyrex.darkplayer.feature.player.extensions.setIsScrubbingModeEnabled
import dev.xboyrex.darkplayer.feature.player.extensions.subtitleDelayMilliseconds
import dev.xboyrex.darkplayer.feature.player.extensions.subtitleSpeed
import dev.xboyrex.darkplayer.feature.player.extensions.subtitleTrackIndex
import dev.xboyrex.darkplayer.feature.player.extensions.switchTrack
import dev.xboyrex.darkplayer.feature.player.extensions.uriToSubtitleConfiguration
import dev.xboyrex.darkplayer.feature.player.extensions.videoDecoderMode
import dev.xboyrex.darkplayer.feature.player.extensions.videoZoom
import dev.xboyrex.darkplayer.feature.player.extensions.withExternalAudio
import dev.xboyrex.darkplayer.feature.player.model.DecoderTrackType
import dev.xboyrex.darkplayer.feature.player.utils.hasApiPosition
import dev.xboyrex.darkplayer.feature.player.utils.isExplicitPlaylistItem
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderManager
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleSpeed
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

@OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {

    private val serviceScope: CoroutineScope = CoroutineScope(
        context =
            Dispatchers.Main + SupervisorJob(),
    )
    private val applicationScope: CoroutineScope by inject(named(DiQualifiers.APPLICATION_SCOPE))
    private var mediaSession: MediaSession? = null
    private var artworkLoadJob: Job? = null
    private var audioFadeJob: Job? = null

    private val preferencesRepository: PreferencesRepository by inject()

    private val dataSourceFactory: NextDataSourceFactory by inject()

    private val mediaRepository: MediaRepository by inject()

    private val imageLoader: ImageLoader by inject()

    private val playerPreferences: PlayerPreferences
        get() = preferencesRepository.playerPreferences.value

    private val customCommands = CustomCommands.asSessionCommands()

    private var isMediaItemReady = false
    private var playbackRequestGeneration = 0L
    private var pendingMediaItemsRequest:
        ListenableFuture<MediaSession.MediaItemsWithStartPosition>? = null
    private var pendingAudioSelection: Pair<String, Uri>? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioEqualizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var presetReverbEffect: PresetReverb? = null
    private var activeAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var currentVolumeGain: Int = 0

    private lateinit var decoderManager: DecoderManager
    private lateinit var trackSelector: DefaultTrackSelector
    private val decoderRecoveryManager = DecoderRecoveryManager()
    private val volumeController = PlaybackVolumeController()
    private val sessionTimerController = PlaybackSessionTimerController()
    private var sessionTimerWatcherJob: Job? = null

    private val decoderAnalyticsListener = object : AnalyticsListener {
        override fun onEvents(player: Player, events: AnalyticsListener.Events) {
            publishDecoderState()
        }

        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long,
        ) {
            Logger.logInfo(
                tag = DECODER_LOG_TAG,
                message = "Video decoder initialized with requested=${decoderManager.videoMode} " +
                    "as ${decoderManager.activeVideoMode}: $decoderName",
            )
            decoderRecoveryManager.onDecoderInitialized(DecoderTrackType.VIDEO)
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long,
        ) {
            Logger.logInfo(
                tag = DECODER_LOG_TAG,
                message = "Audio decoder initialized with requested=${decoderManager.audioMode} " +
                    "as ${decoderManager.activeAudioMode}: $decoderName",
            )
            decoderRecoveryManager.onDecoderInitialized(DecoderTrackType.AUDIO)
        }

        override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
            val videoTracks = tracks.groups
                .filter { it.type == C.TRACK_TYPE_VIDEO }
                .joinToString { group ->
                    val mimeType = group.mediaTrackGroup.getFormat(0).sampleMimeType
                    "$mimeType(supported=${group.isSupported(true)}, selected=${group.isSelected})"
                }
            Logger.logInfo(
                tag = DECODER_LOG_TAG,
                message = "Video tracks: ${videoTracks.ifEmpty { "none" }}, " +
                    "unmapped=${trackSelector.unmappedTrackCount(C.TRACK_TYPE_VIDEO)}",
            )
        }

        override fun onRenderedFirstFrame(
            eventTime: AnalyticsListener.EventTime,
            output: Any,
            renderTimeMs: Long,
        ) {
            Logger.logInfo(
                tag = DECODER_LOG_TAG,
                message = "Rendered first frame with video=${decoderManager.activeVideoMode} at renderTimeMs=$renderTimeMs",
            )
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long,
        ) {
            if (droppedFrames > 0) {
                Logger.logInfo(
                    tag = DECODER_LOG_TAG,
                    message = "Dropped $droppedFrames video frames over ${elapsedMs}ms (mode=${decoderManager.activeVideoMode})",
                )
            }
        }

        override fun onPlayerError(
            eventTime: AnalyticsListener.EventTime,
            error: PlaybackException,
        ) {
            Logger.logError(
                tag = DECODER_LOG_TAG,
                message = "Player error with requestedVideo=${decoderManager.videoMode}, " +
                    "activeVideo=${decoderManager.activeVideoMode}, " +
                    "requestedAudio=${decoderManager.audioMode}, " +
                    "activeAudio=${decoderManager.activeAudioMode}: ${error.message}",
            )
        }
    }

    private val playbackStateListener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val player = mediaSession?.player as? ExoPlayer ?: return
            // Autoplay controls the implicit folder queue; explicitly chosen playlists play in full.
            player.pauseAtEndOfMediaItems = !playerPreferences.autoplay &&
                player.currentMediaItem?.isExplicitPlaylistItem != true
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(
                mediaItem,
                reason,
            )
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return
            sessionTimerController.clearAbRepeat()
            restoreDecoderChoices(mediaItem = mediaItem)
            isMediaItemReady = false
            loadArtworkForCurrentMediaItem()
            mediaItem?.mediaMetadata?.let { metadata ->
                mediaSession?.player?.run {
                    playerSpecificSubtitleDelayMilliseconds =
                        metadata.subtitleDelayMilliseconds ?: 0L
                    playerSpecificSubtitleSpeed = metadata.subtitleSpeed ?: 1f
                }

                metadata.positionMs?.takeIf {
                    mediaItem.hasApiPosition ||
                        playerPreferences.resume == Resume.YES
                }?.let {
                    mediaSession?.player?.seekTo(it)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            super.onPositionDiscontinuity(
                oldPosition,
                newPosition,
                reason,
            )
            if (reason == DISCONTINUITY_REASON_SEEK && playerPreferences.fadeOnSeek) {
                triggerSmoothAudioRamp()
            }
            val oldMediaItem = oldPosition.mediaItem ?: return

            when (reason) {
                DISCONTINUITY_REASON_SEEK,
                DISCONTINUITY_REASON_AUTO_TRANSITION,
                -> {
                    if (newPosition.mediaItem == null || oldMediaItem == newPosition.mediaItem) {
                        return
                    }

                    val updatedPosition =
                        oldPosition.positionMs.takeIf { reason == DISCONTINUITY_REASON_SEEK }
                            ?: C.TIME_UNSET
                    mediaSession?.player?.replaceMediaItem(
                        oldPosition.mediaItemIndex,
                        oldMediaItem.copy(positionMs = updatedPosition),
                    )
                    serviceScope.launch {
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = updatedPosition,
                        )
                    }
                }

                DISCONTINUITY_REASON_REMOVE -> {
                    serviceScope.launch {
                        val durationMs = oldMediaItem.mediaMetadata.durationMs
                        val isAtEnd =
                            durationMs != null && oldPosition.positionMs >= durationMs - 1000
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = if (isAtEnd) C.TIME_UNSET else oldPosition.positionMs,
                        )
                    }
                }

                else -> return
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            serviceScope.launch {
                mediaSession?.player?.currentTracks?.let(block = ::handleUnsupportedTracks)
            }
            if (!isMediaItemReady && tracks.groups.isNotEmpty()) {
                isMediaItemReady = true

                val player = mediaSession?.player ?: return
                val pending = pendingAudioSelection
                pendingAudioSelection = null
                if (pending != null && pending.first == player.currentMediaItem?.mediaId) {
                    val audioIndex =
                        player.currentMediaItem?.mediaMetadata?.externalAudio.orEmpty().indexOf(
                            pending.second,
                        )
                    val trackIndex = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                        .indexOfFirst { it.mediaTrackGroup.externalAudioIndex == audioIndex }
                    if (audioIndex >= 0 && trackIndex >= 0) {
                        player.switchTrack(
                            C.TRACK_TYPE_AUDIO,
                            trackIndex = trackIndex,
                        )
                    }
                }
                if (!playerPreferences.rememberSelections) return
                mediaSession?.player?.mediaMetadata?.audioTrackIndex?.takeIf {
                    pending == null
                }?.let {
                    mediaSession?.player?.switchTrack(
                        C.TRACK_TYPE_AUDIO,
                        trackIndex = it,
                    )
                }
                mediaSession?.player?.mediaMetadata?.subtitleTrackIndex?.let {
                    mediaSession?.player?.switchTrack(
                        C.TRACK_TYPE_TEXT,
                        trackIndex = it,
                    )
                }
            }
        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            super.onTrackSelectionParametersChanged(parameters)
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return

            val audioTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_AUDIO)
            val subtitleTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_TEXT)

            if (audioTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumAudioTrack(
                        uri = currentMediaItem.mediaId,
                        audioTrackIndex = audioTrackIndex,
                    )
                }
            }

            if (subtitleTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumSubtitleTrack(
                        uri = currentMediaItem.mediaId,
                        subtitleTrackIndex = subtitleTrackIndex,
                    )
                }
            }

            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(
                    audioTrackIndex = audioTrackIndex,
                    subtitleTrackIndex = subtitleTrackIndex,
                ),
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            val player = mediaSession?.player
            val shouldResetTrackSelectionParameters = playbackState == Player.STATE_ENDED ||
                (
                    playbackState == Player.STATE_IDLE &&
                        player?.mediaItemCount == 0
                    )
            if (shouldResetTrackSelectionParameters) {
                mediaSession?.player?.trackSelectionParameters = TrackSelectionParameters.DEFAULT
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!error.isDecoderFailure) {
                decoderRecoveryManager.onNonDecoderError()
                return
            }

            val trackType = error.decoderTrackType() ?: run {
                decoderRecoveryManager.onNonDecoderError()
                return
            }
            handleDecoderFailure(trackType = trackType)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(
                playWhenReady,
                reason,
            )

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                if (mediaSession?.player?.repeatMode != Player.REPEAT_MODE_OFF) {
                    mediaSession?.player?.seekTo(0)
                    mediaSession?.player?.play()
                    return
                }
                mediaSession?.run {
                    player.clearMediaItems()
                    player.stop()
                }
                stopSelf()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            // Consecutive items can reuse the renderer without another first-frame callback.
            if (player.playbackState != Player.STATE_READY ||
                !events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                )
            ) {
                return
            }
            val currentMediaItem = player.currentMediaItem ?: return
            val duration = player.duration.validDurationOrNull()
            // Update the media metadata duration so that it will be used later in position discontinuity handling
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(durationMs = duration ?: 0),
            )

            serviceScope.launch {
                mediaRepository.updateMediumLastPlayedTime(
                    uri = currentMediaItem.mediaId,
                    lastPlayedTime = System.currentTimeMillis(),
                    duration = duration,
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying && (playerPreferences.smoothPlayPauseAudioFade || playerPreferences.fadeOnStart)) {
                triggerSmoothAudioRamp()
            }
            mediaSession?.run {
                serviceScope.launch {
                    mediaRepository.updateMediumPosition(
                        uri = player.currentMediaItem?.mediaId ?: return@launch,
                        position = player.currentPosition,
                    )
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            serviceScope.launch {
                preferencesRepository.updatePlayerPreferences {
                    it.copy(
                        loopMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> LoopMode.OFF
                            Player.REPEAT_MODE_ONE -> LoopMode.ONE
                            Player.REPEAT_MODE_ALL -> LoopMode.ALL
                            else -> LoopMode.OFF
                        },
                    )
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            activeAudioSessionId = audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
            attachAudioEffects(audioSessionId)
        }
    }

    private fun triggerSmoothAudioRamp() {
        val player = mediaSession?.player as? ExoPlayer ?: return
        audioFadeJob?.cancel()
        if (volumeController.isMuted) {
            volumeController.applyTo(player)
            return
        }
        audioFadeJob = serviceScope.launch {
            runCatching {
                val steps = 8
                val stepDelay = (playerPreferences.audioFadeDurationMs / steps).coerceIn(15, 60).toLong()
                volumeController.setFadeMultiplier(0.2f, player)
                for (step in 1..steps) {
                    delay(stepDelay)
                    val mult = (0.2f + (0.8f * step / steps.toFloat())).coerceIn(0f, 1f)
                    volumeController.setFadeMultiplier(mult, player)
                }
                volumeController.resetFade(player)
            }
        }
    }

    private fun ensureSessionTimerWatcherRunning() {
        if (sessionTimerWatcherJob?.isActive == true) return
        sessionTimerWatcherJob = serviceScope.launch(Dispatchers.Main) {
            while (sessionTimerController.isSleepTimerActive || sessionTimerController.isAbLoopActive) {
                val player = mediaSession?.player
                if (player != null) {
                    if (sessionTimerController.pollSleepTimerExpired()) {
                        player.pause()
                    }
                    if (sessionTimerController.isAbLoopActive &&
                        sessionTimerController.shouldLoopBackToPointA(player.currentPosition)
                    ) {
                        player.seekTo(sessionTimerController.pointAMs)
                    }
                }
                delay(250L)
            }
        }
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (playerPreferences.enableVolumeBoost) {
            try {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                if (currentVolumeGain > 0) {
                    setEnhancerTargetGain(gain = currentVolumeGain)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loudnessEnhancer = null
            }
        }
        try {
            audioEqualizer?.release()
            audioEqualizer = Equalizer(0, audioSessionId)
            bassBoostEffect?.release()
            bassBoostEffect = BassBoost(0, audioSessionId)
            virtualizerEffect?.release()
            virtualizerEffect = Virtualizer(0, audioSessionId)
            presetReverbEffect?.release()
            presetReverbEffect = PresetReverb(0, audioSessionId)
            applyDspPreferences(playerPreferences)
        } catch (e: Exception) {
            Logger.logError(DECODER_LOG_TAG, "Audio DSP initialization skipped: ${e.message}")
        }
    }

    private fun applyDspPreferences(prefs: PlayerPreferences) {
        runCatching {
            val eq = audioEqualizer
            if (eq != null) {
                eq.enabled = prefs.enableAudioEqualizer
                val bands = prefs.equalizerBandGainsDb
                val numBands = eq.numberOfBands.toInt().coerceAtMost(bands.size)
                val range = eq.bandLevelRange
                val minLevel = range.getOrNull(0)?.toInt() ?: -1500
                val maxLevel = range.getOrNull(1)?.toInt() ?: 1500
                for (i in 0 until numBands) {
                    val targetMb = (bands[i] * 100)
                        .coerceIn(minLevel, maxLevel)
                        .toShort()
                    eq.setBandLevel(i.toShort(), targetMb)
                }
            }
            bassBoostEffect?.apply {
                val strength = (prefs.bassBoostPercent * 10).coerceIn(0, 1000)
                enabled = prefs.enableAudioEqualizer && strength > 0
                if (strengthSupported) {
                    setStrength(strength.toShort())
                }
            }
            virtualizerEffect?.apply {
                val strength = (prefs.virtualizerPercent * 10).coerceIn(0, 1000)
                enabled = prefs.enableAudioEqualizer && strength > 0
                if (strengthSupported) {
                    setStrength(strength.toShort())
                }
            }
            presetReverbEffect?.apply {
                val reverbCode = when (prefs.reverbPreset.lowercase()) {
                    "smallroom", "small room" -> 1
                    "mediumroom", "medium room" -> 2
                    "largeroom", "large room" -> 3
                    "mediumhall", "medium hall" -> 4
                    "largehall", "large hall" -> 5
                    "plate" -> 6
                    else -> 0
                }
                enabled = prefs.enableAudioEqualizer && reverbCode > 0
                preset = reverbCode.toShort()
            }
        }
    }

    private fun releaseAudioEffects() {
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        runCatching { audioEqualizer?.release() }
        audioEqualizer = null
        runCatching { bassBoostEffect?.release() }
        bassBoostEffect = null
        runCatching { virtualizerEffect?.release() }
        virtualizerEffect = null
        runCatching { presetReverbEffect?.release() }
        presetReverbEffect = null
    }

    private fun setEnhancerTargetGain(gain: Int) {
        val enhancer = loudnessEnhancer ?: return

        try {
            enhancer.setTargetGain(gain)
            enhancer.enabled = gain > 0
            currentVolumeGain = enhancer.targetGain.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.ConnectionResult> = Futures.immediateFuture(
            MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .addSessionCommands(customCommands)
                    .build(),
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            ),
        )

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            cancelPendingMediaItems()
            playbackRequestGeneration++
            val request = serviceScope.future {
                val updatedMediaItems = withContext(Dispatchers.Default) {
                    updatedMediaItemsWithMetadata(mediaItems = mediaItems)
                }
                MediaSession.MediaItemsWithStartPosition(
                    updatedMediaItems,
                    startIndex,
                    startPositionMs,
                )
            }
            pendingMediaItemsRequest = request
            request.addListener(
                {
                    if (pendingMediaItemsRequest === request) pendingMediaItemsRequest = null
                },
                MoreExecutors.directExecutor(),
            )
            return request
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future(Dispatchers.Default) {
            val updatedMediaItems = updatedMediaItemsWithMetadata(mediaItems = mediaItems)
            return@future updatedMediaItems.toMutableList()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = serviceScope.future {
            val command = CustomCommands.fromSessionCommand(sessionCommand = customCommand)
                ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

            when (command) {
                CustomCommands.ADD_SUBTITLE_TRACK -> {
                    val subtitleUri = args.getString(CustomCommands.SUBTITLE_TRACK_URI_KEY)?.toUri()
                        ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

                    val newSubConfiguration = uriToSubtitleConfiguration(
                        uri = subtitleUri,
                        subtitleEncoding = playerPreferences.subtitleTextEncoding,
                    )
                    mediaSession?.player?.let { player ->
                        val currentMediaItem = player.currentMediaItem ?: return@let
                        val textTracks = player.currentTracks.groups.filter {
                            it.type == C.TRACK_TYPE_TEXT && it.isSupported
                        }

                        mediaRepository.updateMediumPosition(
                            uri = currentMediaItem.mediaId,
                            position = player.currentPosition,
                        )
                        mediaRepository.updateMediumSubtitleTrack(
                            uri = currentMediaItem.mediaId,
                            subtitleTrackIndex = textTracks.size,
                        )
                        mediaRepository.addExternalSubtitleToMedium(
                            uri = currentMediaItem.mediaId,
                            subtitleUri = subtitleUri,
                        )
                        player.addAdditionalSubtitleConfiguration(subtitle = newSubConfiguration)
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.ADD_AUDIO_TRACK -> {
                    val uri = args.getString(CustomCommands.AUDIO_TRACK_URI_KEY)?.toUri()
                        ?.takeIf {
                            it.scheme == ContentResolver.SCHEME_CONTENT ||
                                it.scheme == ContentResolver.SCHEME_FILE
                        }
                        ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)
                    val player = session.player
                    val mediaItem = player.currentMediaItem
                        ?: return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                    val hasAudio = withContext(Dispatchers.IO) {
                        runCatching {
                            contentResolver.openFileDescriptor(
                                uri,
                                "r",
                            )?.use { descriptor ->
                                MediaInfoBuilder().from(
                                    descriptor = descriptor,
                                ).build()?.let { info ->
                                    try {
                                        info.audioStreams.isNotEmpty()
                                    } finally {
                                        info.release()
                                    }
                                }
                            } == true
                        }.getOrDefault(defaultValue = false)
                    }
                    if (!hasAudio) return@future SessionResult(SessionError.ERROR_BAD_VALUE)
                    if (player.currentMediaItem?.mediaId != mediaItem.mediaId) {
                        return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                    }
                    mediaRepository.addExternalAudioToMedium(
                        mediaItem.mediaId,
                        audioUri = uri,
                    )
                    val currentItem =
                        player.currentMediaItem?.takeIf { it.mediaId == mediaItem.mediaId }
                            ?: return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                    val audio = (currentItem.mediaMetadata.externalAudio + uri).distinct()
                    pendingAudioSelection = currentItem.mediaId to uri
                    val updatedItem = currentItem.withExternalAudio(
                        uris = audio,
                    ).copy(positionMs = player.currentPosition)
                    val index = player.currentMediaItemIndex
                    player.addMediaItem(
                        index + 1,
                        updatedItem,
                    )
                    player.seekTo(
                        index + 1,
                        player.currentPosition,
                    )
                    player.removeMediaItem(index)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_SKIP_SILENCE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY)
                    mediaSession?.player?.playerSpecificSkipSilenceEnabled = enabled
                    mediaSession?.sessionExtras =
                        Bundle(mediaSession?.sessionExtras ?: Bundle.EMPTY).apply {
                            putBoolean(
                                CustomCommands.SKIP_SILENCE_ENABLED_KEY,
                                enabled,
                            )
                        }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SKIP_SILENCE_ENABLED -> {
                    val enabled = mediaSession?.player?.playerSpecificSkipSilenceEnabled ?: false
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(
                                CustomCommands.SKIP_SILENCE_ENABLED_KEY,
                                enabled,
                            )
                        },
                    )
                }

                CustomCommands.SET_IS_SCRUBBING_MODE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.IS_SCRUBBING_MODE_ENABLED_KEY)
                    mediaSession?.player?.setIsScrubbingModeEnabled(enabled = enabled)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED -> {
                    val isSupported = loudnessEnhancer != null
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(
                                CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED_KEY,
                                isSupported,
                            )
                        },
                    )
                }

                CustomCommands.SET_LOUDNESS_GAIN -> {
                    val gain = args.getInt(
                        CustomCommands.LOUDNESS_GAIN_KEY,
                        0,
                    )
                    setEnhancerTargetGain(gain = gain)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_LOUDNESS_GAIN -> {
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putInt(
                                CustomCommands.LOUDNESS_GAIN_KEY,
                                currentVolumeGain,
                            )
                        },
                    )
                }

                CustomCommands.SET_VIDEO_DECODER_MODE -> {
                    val mode = args.decoderMode(CustomCommands.VIDEO_DECODER_MODE_KEY)
                        ?: return@future SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                    val player = mediaSession?.player as? ExoPlayer
                        ?: return@future SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                    decoderRecoveryManager.onUserSelection(
                        DecoderTrackType.VIDEO,
                        mode = mode,
                    )
                    selectDecoder(
                        DecoderTrackType.VIDEO,
                        mode = mode,
                    )
                    serviceScope.launch {
                        handleUnsupportedTrack(
                            player.currentTracks,
                            DecoderTrackType.VIDEO,
                        )
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_AUDIO_DECODER_MODE -> {
                    val mode = args.decoderMode(CustomCommands.AUDIO_DECODER_MODE_KEY)
                        ?: return@future SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                    val player = mediaSession?.player as? ExoPlayer
                        ?: return@future SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                    decoderRecoveryManager.onUserSelection(
                        DecoderTrackType.AUDIO,
                        mode = mode,
                    )
                    selectDecoder(
                        DecoderTrackType.AUDIO,
                        mode = mode,
                    )
                    serviceScope.launch {
                        handleUnsupportedTrack(
                            player.currentTracks,
                            DecoderTrackType.AUDIO,
                        )
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.TRY_DECODER_FALLBACK -> {
                    val retry = decoderRecoveryManager.confirmFallback()
                        ?: return@future SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                    if (!retryDecoderWith(retry = retry)) {
                        decoderRecoveryManager.onNonDecoderError()
                        return@future SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SUBTITLE_DELAY -> {
                    val subtitleDelay =
                        mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds ?: 0
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putLong(
                                CustomCommands.SUBTITLE_DELAY_KEY,
                                subtitleDelay,
                            )
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_DELAY -> {
                    val subtitleDelay = args.getLong(CustomCommands.SUBTITLE_DELAY_KEY)
                    mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds = subtitleDelay
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = mediaSession?.player?.playerSpecificSubtitleSpeed ?: 0f
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putFloat(
                                CustomCommands.SUBTITLE_SPEED_KEY,
                                subtitleSpeed,
                            )
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = args.getFloat(CustomCommands.SUBTITLE_SPEED_KEY)
                    mediaSession?.player?.playerSpecificSubtitleSpeed = subtitleSpeed
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.START_BACKGROUND_PLAYBACK -> {
                    return@future startBackgroundPlayback(
                        session,
                        args,
                    )
                }

                CustomCommands.STOP_PLAYER_SESSION -> {
                    savePositionAndStopSession()
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_PLAYER_MUTED -> {
                    val muted = args.getBoolean(CustomCommands.PLAYER_MUTED_KEY, false)
                    audioFadeJob?.cancel()
                    volumeController.setMuted(muted, session.player)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_SLEEP_TIMER_MINUTES -> {
                    val mins = args.getInt(CustomCommands.SLEEP_TIMER_MINUTES_KEY, 0)
                    sessionTimerController.scheduleSleepTimer(mins)
                    ensureSessionTimerWatcherRunning()
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_AB_REPEAT_POINTS -> {
                    val startMs = args.getLong(CustomCommands.AB_REPEAT_START_MS_KEY, -1L)
                    val endMs = args.getLong(CustomCommands.AB_REPEAT_END_MS_KEY, -1L)
                    sessionTimerController.setAbRepeatPoints(startMs, endMs)
                    ensureSessionTimerWatcherRunning()
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }
            }
        }
    }

    private suspend fun startBackgroundPlayback(
        session: MediaSession,
        args: Bundle,
    ): SessionResult {
        val requestGeneration = ++playbackRequestGeneration
        val items = try {
            val binder = args.getBinder(CustomCommands.BACKGROUND_MEDIA_ITEMS_KEY)
                ?: return SessionResult(SessionError.ERROR_BAD_VALUE)
            withContext(Dispatchers.Default) {
                BundleListRetriever.getList(binder).map { MediaItem.fromBundle(it) }
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            return SessionResult(SessionError.ERROR_BAD_VALUE)
        }
        val index = args.getInt(
            CustomCommands.BACKGROUND_START_INDEX_KEY,
            0,
        )
        val position = args.getLong(
            CustomCommands.BACKGROUND_START_POSITION_KEY,
            C.TIME_UNSET,
        )

        @Suppress("DEPRECATION")
        val speed = (args.get(CustomCommands.PLAYBACK_SPEED_KEY) as? Number)?.toFloat()
        if (items.isEmpty() || index !in items.indices ||
            items.any { it.localConfiguration?.uri == null } ||
            (position < 0 && position != C.TIME_UNSET) ||
            (
                args.containsKey(CustomCommands.PLAYBACK_SPEED_KEY) &&
                    (speed == null || !speed.isFinite() || speed <= 0)
                )
        ) {
            return SessionResult(SessionError.ERROR_BAD_VALUE)
        }
        if (requestGeneration != playbackRequestGeneration || mediaSession !== session) {
            return SessionResult(SessionError.ERROR_INVALID_STATE)
        }
        cancelPendingMediaItems()
        val generation = ++playbackRequestGeneration
        val updatedItems = withContext(Dispatchers.Default) {
            updatedMediaItemsWithMetadata(items)
        }
        if (generation != playbackRequestGeneration || mediaSession !== session) {
            return SessionResult(SessionError.ERROR_INVALID_STATE)
        }
        return session.player.run {
            setMediaItems(
                updatedItems,
                index,
                position,
            )
            speed?.let(::setPlaybackSpeed)
            prepare()
            play()
            SessionResult(
                SessionResult.RESULT_SUCCESS,
                Bundle().apply {
                    putString(
                        CustomCommands.BACKGROUND_MEDIA_URI_KEY,
                        currentMediaItem?.localConfiguration?.uri?.toString(),
                    )
                    putLong(
                        CustomCommands.BACKGROUND_POSITION_KEY,
                        currentPosition,
                    )
                    putLong(
                        CustomCommands.BACKGROUND_DURATION_KEY,
                        duration,
                    )
                },
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RETAIN_PLAYBACK_GRANTS) {
            // A stop may arrive between the handoff acknowledgement and grant delivery.
            if (mediaSession?.player?.currentMediaItem == null) stopSelf(startId)
            // Keep the grant-bearing start record until stop so Android can revoke its grants.
            return START_REDELIVER_INTENT
        }
        return super.onStartCommand(
            intent,
            flags,
            startId,
        )
    }

    override fun onCreate() {
        super.onCreate()
        decoderManager = DecoderManager()
        val renderersFactory = NextRenderersFactory(
            context = applicationContext,
        ).setDecoderManager(decoderManager = decoderManager)

        trackSelector = DefaultTrackSelector(applicationContext).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(playerPreferences.enableTunneledPlayback)
                    .setPreferredAudioLanguage(playerPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(playerPreferences.preferredSubtitleLanguage),
            )
        }

        val player = ExoPlayer.Builder(applicationContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                ExternalAudioMediaSourceFactory(
                    ApiMediaSourceFactory(
                        applicationContext,
                        dataSourceFactory,
                    ),
                ),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                playerPreferences.requireAudioFocus,
            )
            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)
            .build()
            .also {
                it.setPlaybackSpeed(playerPreferences.defaultPlaybackSpeed)
                it.addListener(playbackStateListener)
                it.pauseAtEndOfMediaItems = !playerPreferences.autoplay
                it.repeatMode = when (playerPreferences.loopMode) {
                    LoopMode.OFF -> Player.REPEAT_MODE_OFF
                    LoopMode.ONE -> Player.REPEAT_MODE_ONE
                    LoopMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }

        decoderManager.attach(player = player)
        player.addAnalyticsListener(decoderAnalyticsListener)

        serviceScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                if (activeAudioSessionId != C.AUDIO_SESSION_ID_UNSET && audioEqualizer == null) {
                    attachAudioEffects(activeAudioSessionId)
                } else {
                    applyDspPreferences(prefs)
                }
            }
        }

        try {
            mediaSession = MediaSession.Builder(
                this,
                player,
            ).apply {
                setSessionActivity(
                    PendingIntent.getActivity(
                        this@PlayerService,
                        0,
                        Intent(
                            this@PlayerService,
                            PlayerActivity::class.java,
                        ),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setCallback(mediaSessionCallback)
                setCustomLayout(
                    listOf(
                        CommandButton.Builder(ICON_UNDEFINED)
                            .setCustomIconResId(coreUiR.drawable.ic_close)
                            .setDisplayName(getString(coreUiR.string.stop_player_session))
                            .setSessionCommand(CustomCommands.STOP_PLAYER_SESSION.sessionCommand)
                            .setEnabled(true)
                            .build(),
                    ),
                )
            }.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            serviceScope.launch { savePositionAndStopSession() }
        }
    }

    private suspend fun savePositionAndStopSession() {
        cancelPendingMediaItems()
        val generation = ++playbackRequestGeneration
        mediaSession?.run {
            val mediaItem = player.currentMediaItem
            val position = player.currentPosition
            player.clearMediaItems()
            player.stop()
            mediaItem?.let {
                mediaRepository.updateMediumPosition(
                    uri = mediaItem.mediaId,
                    position = position,
                )
            }
        }
        if (generation == playbackRequestGeneration) stopSelf()
    }

    private fun cancelPendingMediaItems() {
        // Cancelling drains Media3's queued commands, which may submit another queue.
        while (true) {
            val request = pendingMediaItemsRequest ?: return
            pendingMediaItemsRequest = null
            request.cancel(false)
        }
    }

    override fun onDestroy() {
        cancelPendingMediaItems()
        super.onDestroy()
        artworkLoadJob?.cancel()
        audioFadeJob?.cancel()
        sessionTimerWatcherJob?.cancel()
        releaseAudioEffects()
        mediaSession?.run {
            player.clearMediaItems()
            player.stop()
            player.removeListener(playbackStateListener)
            decoderManager.detach()
            player.release()
            release()
            mediaSession = null
        }
        // Queue release before a new service can open another connection.
        applicationScope.launch(start = CoroutineStart.UNDISPATCHED) { dataSourceFactory.release() }
        serviceScope.cancel()
    }

    private fun Long.validDurationOrNull(): Long? = takeIf { it != C.TIME_UNSET && it >= 0 }

    private suspend fun updatedMediaItemsWithMetadata(
        mediaItems: List<MediaItem>,
    ): List<MediaItem> = supervisorScope {
        val batchedStates = runCatching {
            mediaRepository.getVideosState(mediaItems.map { it.mediaId })
        }.getOrDefault(emptyMap())
        val ioSemaphore = Semaphore(permits = 4)

        mediaItems.map { mediaItem ->
            async {
                ioSemaphore.withPermit {
                    val uri = mediaItem.mediaId.toUri()
                    val isNetwork = mediaItem.isNetworkMediaItem()

                    val video = if (isNetwork) {
                        null
                    } else {
                        mediaRepository.getVideoByUri(
                            uri = mediaItem.mediaId,
                        )
                    }
                    val videoState = batchedStates[mediaItem.mediaId]
                        ?: mediaRepository.getVideoState(uri = mediaItem.mediaId)

                    val externalSubs = videoState?.externalSubs ?: emptyList()
                    val savedAudio = videoState?.externalAudio.orEmpty()
                    val externalAudio = if (savedAudio.isEmpty()) {
                        emptyList()
                    } else {
                        withContext(Dispatchers.IO) {
                            savedAudio.filter { audioUri ->
                                runCatching {
                                    contentResolver.openAssetFileDescriptor(
                                        audioUri,
                                        "r",
                                    )?.use { true } == true
                                }
                                    .getOrDefault(defaultValue = false)
                            }
                        }
                    }
                    val localSubs = if (!isNetwork) {
                        (video?.path ?: videoState?.path ?: getPath(uri = uri))?.let {
                            File(it).getLocalSubtitles(
                                context = this@PlayerService,
                                excludeSubsList = externalSubs,
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }

                    val existingSubConfigurations =
                        mediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
                    val subConfigurations = (localSubs + externalSubs).map { subtitleUri ->
                        uriToSubtitleConfiguration(
                            uri = subtitleUri,
                            subtitleEncoding = playerPreferences.subtitleTextEncoding,
                        )
                    }

                // Local items get a placeholder now and their real artwork in the background;
                // network items have no thumbnail to extract, so keep any supplied artwork.
                val artworkUri = if (isNetwork) {
                    mediaItem.mediaMetadata.artworkUri ?: getDefaultArtworkUri()
                } else {
                    getDefaultArtworkUri()
                }

                val title =
                    mediaItem.mediaMetadata.title ?: video?.nameWithExtension
                        ?: getFilenameFromUri(uri = uri)
                val positionMs = mediaItem.mediaMetadata.positionMs ?: videoState?.position
                val videoScale = mediaItem.mediaMetadata.videoZoom ?: videoState?.videoScale
                val audioTrackIndex =
                    mediaItem.mediaMetadata.audioTrackIndex ?: videoState?.audioTrackIndex
                val subtitleTrackIndex =
                    mediaItem.mediaMetadata.subtitleTrackIndex ?: videoState?.subtitleTrackIndex
                val subtitleDelay =
                    mediaItem.mediaMetadata.subtitleDelayMilliseconds
                        ?: videoState?.subtitleDelayMilliseconds
                val subtitleSpeed =
                    mediaItem.mediaMetadata.subtitleSpeed ?: videoState?.subtitleSpeed

                mediaItem.buildUpon().apply {
                    setSubtitleConfigurations(
                        deduplicateSubtitleConfigurations(
                            existing = existingSubConfigurations,
                            added = subConfigurations,
                        ),
                    )
                    setMediaMetadata(
                        MediaMetadata.Builder().apply {
                            setTitle(title)
                            setArtworkUri(artworkUri)
                            setExtras(
                                positionMs = positionMs,
                                videoScale = videoScale,
                                audioTrackIndex = audioTrackIndex.takeIf {
                                    externalAudio == savedAudio
                                },
                                subtitleTrackIndex = subtitleTrackIndex,
                                subtitleDelayMilliseconds = subtitleDelay,
                                subtitleSpeed = subtitleSpeed,
                                videoDecoderMode = mediaItem.mediaMetadata.videoDecoderMode,
                                audioDecoderMode = mediaItem.mediaMetadata.audioDecoderMode,
                            )
                        }.build(),
                    )
                }.build().withExternalAudio(uris = externalAudio)
                }
            }
        }.awaitAll()
    }

    private fun publishDecoderState() {
        val session = mediaSession ?: return
        val recoveryState = decoderRecoveryManager.state
        session.sessionExtras = Bundle(session.sessionExtras).apply {
            putString(
                CustomCommands.VIDEO_DECODER_MODE_KEY,
                decoderManager.activeVideoMode?.name,
            )
            putString(
                CustomCommands.AUDIO_DECODER_MODE_KEY,
                decoderManager.activeAudioMode?.name,
            )
            putString(
                CustomCommands.DECODER_RECOVERY_STATUS_KEY,
                recoveryState.status.name,
            )
            putString(
                CustomCommands.DECODER_RECOVERY_TRACK_TYPE_KEY,
                recoveryState.trackType?.name,
            )
            putString(
                CustomCommands.UNSUPPORTED_DECODER_MODE_KEY,
                recoveryState.unsupportedMode?.name,
            )
        }
    }

    private fun retryDecoderWith(retry: DecoderRetry): Boolean {
        val player = mediaSession?.player as? ExoPlayer ?: return false
        val shouldPrepare = player.playerError != null
        selectDecoder(
            retry.trackType,
            retry.mode,
        )
        if (shouldPrepare && player.mediaItemCount > 0) {
            player.prepare()
        }
        return true
    }

    private fun handleUnsupportedTracks(tracks: Tracks) {
        handleUnsupportedTrack(
            tracks = tracks,
            DecoderTrackType.VIDEO,
        )
        handleUnsupportedTrack(
            tracks = tracks,
            DecoderTrackType.AUDIO,
        )
    }

    private fun handleUnsupportedTrack(tracks: Tracks, trackType: DecoderTrackType) {
        val mediaTrackType = trackType.mediaTrackType
        val groups = tracks.groups.filter { it.type == mediaTrackType }
        val hasTrack =
            groups.isNotEmpty() || trackSelector.unmappedTrackCount(trackType = mediaTrackType) > 0
        if (!hasTrack || groups.any { it.isSupported(true) }) return

        handleDecoderFailure(trackType = trackType)
    }

    private fun handleDecoderFailure(trackType: DecoderTrackType) {
        val mode = when (trackType) {
            DecoderTrackType.VIDEO -> decoderManager.videoMode
            DecoderTrackType.AUDIO -> decoderManager.audioMode
        }
        val retry = decoderRecoveryManager.onDecoderFailure(
            trackType = trackType,
            mode = mode,
        )
        publishDecoderState()
        if (retry == null) return
        serviceScope.launch {
            if (!retryDecoderWith(retry = retry)) decoderRecoveryManager.onNonDecoderError()
        }
    }

    private fun selectDecoder(trackType: DecoderTrackType, mode: DecoderMode) {
        when (trackType) {
            DecoderTrackType.VIDEO -> decoderManager.selectVideoDecoder(mode = mode)
            DecoderTrackType.AUDIO -> decoderManager.selectAudioDecoder(mode = mode)
        }
        val player = mediaSession?.player
        val mediaItem = player?.currentMediaItem
        if (mediaItem != null) {
            val updatedItem = when (trackType) {
                DecoderTrackType.VIDEO -> mediaItem.copy(videoDecoderMode = mode)
                DecoderTrackType.AUDIO -> mediaItem.copy(audioDecoderMode = mode)
            }
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                updatedItem,
            )
        }
        publishDecoderState()
    }

    private fun restoreDecoderChoices(mediaItem: MediaItem?) {
        val videoMode = mediaItem?.mediaMetadata?.videoDecoderMode ?: DecoderMode.AUTO
        val audioMode = mediaItem?.mediaMetadata?.audioDecoderMode ?: DecoderMode.AUTO
        decoderRecoveryManager.onUserSelection(
            DecoderTrackType.VIDEO,
            mode = videoMode,
        )
        decoderRecoveryManager.onUserSelection(
            DecoderTrackType.AUDIO,
            mode = audioMode,
        )
        selectDecoder(
            DecoderTrackType.VIDEO,
            mode = videoMode,
        )
        selectDecoder(
            DecoderTrackType.AUDIO,
            mode = audioMode,
        )
    }

    private fun DefaultTrackSelector.unmappedTrackCount(trackType: Int): Int {
        val trackGroups = currentMappedTrackInfo?.unmappedTrackGroups ?: return 0
        return (0 until trackGroups.length).count { index -> trackGroups[index].type == trackType }
    }

    private fun PlaybackException.decoderTrackType(): DecoderTrackType? {
        val playbackError = this as? ExoPlaybackException ?: return null
        val formatTrackType = MimeTypes.getTrackType(playbackError.rendererFormat?.sampleMimeType)
        val mediaTrackType = formatTrackType.takeIf {
            it == C.TRACK_TYPE_VIDEO || it == C.TRACK_TYPE_AUDIO
        } ?: trackSelector.currentMappedTrackInfo
            ?.takeIf { playbackError.rendererIndex in 0 until it.rendererCount }
            ?.getRendererType(playbackError.rendererIndex)

        return when (mediaTrackType) {
            C.TRACK_TYPE_VIDEO -> DecoderTrackType.VIDEO
            C.TRACK_TYPE_AUDIO -> DecoderTrackType.AUDIO
            else -> null
        }
    }

    private fun getDefaultArtworkUri(): Uri = Uri.Builder().apply {
        val defaultArtwork = R.drawable.artwork_default
        scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        authority(resources.getResourcePackageName(defaultArtwork))
        appendPath(resources.getResourceTypeName(defaultArtwork))
        appendPath(resources.getResourceEntryName(defaultArtwork))
    }.build()

    private fun loadArtworkForCurrentMediaItem() {
        artworkLoadJob?.cancel()
        artworkLoadJob = serviceScope.launch(Dispatchers.Main) {
            val player = mediaSession?.player ?: return@launch
            val currentMediaItem = player.currentMediaItem ?: return@launch
            if (currentMediaItem.mediaMetadata.artworkData != null) return@launch

            val artworkUri = loadArtworkForMediaItem(mediaItem = currentMediaItem)
                ?: getDefaultArtworkUri()

            val updatedPlayer = mediaSession?.player ?: return@launch
            val updatedMediaItem = updatedPlayer.currentMediaItem ?: return@launch
            if (updatedMediaItem.mediaId != currentMediaItem.mediaId) return@launch

            updatedPlayer.replaceMediaItem(
                updatedPlayer.currentMediaItemIndex,
                updatedMediaItem.withArtwork(uri = artworkUri),
            )
        }
    }
    private suspend fun loadArtworkForMediaItem(mediaItem: MediaItem): Uri? =
        withContext(Dispatchers.IO) {
            val defaultArtwork = getDefaultArtworkUri()
            val uri = mediaItem.mediaMetadata.artworkUri
                ?.takeUnless { it == defaultArtwork }
                ?: if (mediaItem.isNetworkMediaItem()) {
                    return@withContext null
                } else {
                    mediaItem.mediaId.toUri()
                }
            return@withContext try {
                val request = ImageRequest.Builder(context = this@PlayerService)
                    .data(data = uri)
                    .size(
                        width = 512,
                        height = 512,
                    )
                    .build()
                imageLoader.execute(request = request)
                val diskCache = imageLoader.diskCache ?: return@withContext null
                return@withContext diskCache.openSnapshot(uri.toString())?.use { snapshot ->
                    snapshot.data.toFile().toUri()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        }

    private fun MediaItem.isNetworkMediaItem(): Boolean {
        val uri = localConfiguration?.uri ?: return false
        return uri.scheme?.lowercase() in REMOTE_SCHEMES || NetworkUri.isNetworkUri(uri = uri)
    }

    private fun MediaItem.withArtwork(uri: Uri): MediaItem = buildUpon()
        .setMediaMetadata(
            mediaMetadata.buildUpon()
                .setArtworkUri(uri)
                .build(),
        )
        .build()
}

internal val PlaybackException.isDecoderFailure: Boolean
    get() = when (errorCode) {
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
        -> true

        else -> false
    }

private val DecoderTrackType.mediaTrackType: Int
    get() = when (this) {
        DecoderTrackType.VIDEO -> C.TRACK_TYPE_VIDEO
        DecoderTrackType.AUDIO -> C.TRACK_TYPE_AUDIO
    }

private const val DECODER_LOG_TAG = "Decoder"

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSkipSilenceEnabled: Boolean
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.skipSilenceEnabled
        else -> false
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.skipSilenceEnabled = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleDelayMilliseconds: Long
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleDelayMilliseconds
        else -> 0L
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleDelayMilliseconds = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleSpeed: Float
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleSpeed
        else -> 0f
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleSpeed = value
        }
    }

/** Schemes Media3 streams over the network itself, as opposed to reading from local storage. */
private val REMOTE_SCHEMES = setOf(
    "http",
    "https",
    "rtsp",
)

internal fun deduplicateSubtitleConfigurations(
    existing: List<MediaItem.SubtitleConfiguration>,
    added: List<MediaItem.SubtitleConfiguration>,
): List<MediaItem.SubtitleConfiguration> =
    (existing + added).distinctBy { config ->
        config.id ?: "${config.uri}|${config.mimeType.orEmpty()}|${config.language.orEmpty()}"
    }
