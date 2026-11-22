package com.mayank.darkplayer.core.media.sync

interface MediaSynchronizer {
    suspend fun refresh(path: String? = null): Boolean
    fun startSync()
    fun stopSync()
}
