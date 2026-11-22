package com.mayank.darkplayer.core.data.mappers

import com.mayank.darkplayer.core.model.AudioStreamInfo
import com.mayank.darkplayer.core.model.SubtitleStreamInfo
import com.mayank.darkplayer.core.model.VideoStreamInfo
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
