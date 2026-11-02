package dev.xboyrex.darkplayer.core.data

import dev.xboyrex.darkplayer.core.data.repository.NetworkConnectionRepository
import dev.xboyrex.darkplayer.core.database.DatabaseModule
import dev.xboyrex.darkplayer.core.datastore.di.DataStoreModule
import dev.xboyrex.darkplayer.core.media.MediaModule
import dev.xboyrex.darkplayer.core.media.network.NetworkConnectionResolver
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [DatabaseModule::class, DataStoreModule::class, MediaModule::class])
@ComponentScan
class DataModule {
    /** Lets `core:media` resolve a playback uri's connection id without depending on `core:data`. */
    @Single
    fun providesNetworkConnectionResolver(
        repository: NetworkConnectionRepository,
    ): NetworkConnectionResolver = NetworkConnectionResolver { id ->
        repository.getConnection(id = id)
    }
}
