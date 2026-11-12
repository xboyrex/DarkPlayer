package dev.xboyrex.novaplayer.core.data.repository

import dev.xboyrex.novaplayer.core.model.M3UPlaylist
import dev.xboyrex.novaplayer.core.model.M3UPlaylistItem
import dev.xboyrex.novaplayer.core.model.PlaylistRecord
import dev.xboyrex.novaplayer.core.model.PlaylistSummary
import dev.xboyrex.novaplayer.core.model.PlaylistType
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun observePlaylists(): Flow<List<PlaylistSummary>>

    fun observePlaylist(playlistId: Long): Flow<PlaylistRecord?>

    suspend fun getPlaylist(playlistId: Long): PlaylistRecord?

    suspend fun create(name: String, videoUris: List<String> = emptyList()): Long

    suspend fun createM3U(type: PlaylistType, source: String, playlist: M3UPlaylist): Long

    suspend fun replaceM3UItems(playlistId: Long, items: List<M3UPlaylistItem>)

    suspend fun rename(playlistId: Long, name: String)

    suspend fun delete(playlistId: Long)

    suspend fun addVideos(playlistId: Long, videoUris: List<String>): Int

    suspend fun removeVideo(playlistId: Long, videoUri: String)

    suspend fun replaceOrder(playlistId: Long, orderedUris: List<String>)

    suspend fun markVideoPlayed(playlistId: Long, videoUri: String)

    suspend fun countFilePlaylistsBySource(source: String): Int
}
