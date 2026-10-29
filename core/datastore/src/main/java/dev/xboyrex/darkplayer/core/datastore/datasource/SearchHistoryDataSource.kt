package dev.xboyrex.darkplayer.core.datastore.datasource

import androidx.datastore.core.DataStore
import dev.xboyrex.darkplayer.core.common.Logger
import dev.xboyrex.darkplayer.core.common.di.DiQualifiers
import dev.xboyrex.darkplayer.core.model.SearchHistory
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [])
class SearchHistoryDataSource(
    @Named(DiQualifiers.SEARCH_HISTORY) private val searchHistoryDataStore:
    DataStore<SearchHistory>,
) {

    companion object {
        private const val TAG = "SearchHistoryDataSource"
    }

    val searchHistory: Flow<SearchHistory> = searchHistoryDataStore.data

    suspend fun update(transform: suspend (SearchHistory) -> SearchHistory) {
        try {
            searchHistoryDataStore.updateData(transform = transform)
        } catch (ioException: Exception) {
            Logger.logError(
                tag = TAG,
                message = "Failed to update search history: $ioException",
            )
        }
    }
}
