package dev.xboyrex.novaplayer.core.data.mappers

import dev.xboyrex.novaplayer.core.data.models.VideoState
import dev.xboyrex.novaplayer.core.database.converter.UriListConverter
import dev.xboyrex.novaplayer.core.database.entities.MediumStateEntity

fun MediumStateEntity.toVideoState(): VideoState = VideoState(
    path = uriString,
    position = playbackPosition.takeIf { it != 0L },
    audioTrackIndex = audioTrackIndex,
    subtitleTrackIndex = subtitleTrackIndex,
    externalSubs = UriListConverter.fromStringToList(urlString = externalSubs),
    externalAudio = UriListConverter.fromStringToList(urlString = externalAudio),
    videoScale = videoScale,
    subtitleDelayMilliseconds = subtitleDelayMilliseconds,
    subtitleSpeed = subtitleSpeed,
)
