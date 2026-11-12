package dev.xboyrex.novaplayer.core.media.network

import dev.xboyrex.novaplayer.core.model.NetworkConnection

fun interface NetworkClientFactory {
    fun create(connection: NetworkConnection): NetworkClient
}
