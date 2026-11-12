package dev.xboyrex.darkplayer.core.media.network

import dev.xboyrex.darkplayer.core.model.NetworkConnection

fun interface NetworkClientFactory {
    fun create(connection: NetworkConnection): NetworkClient
}
