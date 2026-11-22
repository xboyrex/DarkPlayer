package com.mayank.darkplayer.core.media.network

import com.mayank.darkplayer.core.model.NetworkConnection

fun interface NetworkClientFactory {
    fun create(connection: NetworkConnection): NetworkClient
}
