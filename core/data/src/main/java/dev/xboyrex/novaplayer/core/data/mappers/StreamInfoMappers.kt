package dev.xboyrex.novaplayer.core.data.mappers

import dev.xboyrex.novaplayer.core.model.AudioStreamInfo
import dev.xboyrex.novaplayer.core.model.SubtitleStreamInfo
import dev.xboyrex.novaplayer.core.model.VideoStreamInfo
import io.github.anilbeesetti.nextlib.mediainfo.AudioStream
import io.github.anilbeesetti.nextlib.mediainfo.SubtitleStream
import io.github.anilbeesetti.nextlib.mediainfo.VideoStream

internal fun VideoStream.toVideoStreamInfo() = VideoStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
)

internal fun AudioStream.toAudioStreamInfo() = AudioStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout,
)

internal fun SubtitleStream.toSubtitleStreamInfo() = SubtitleStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
)
