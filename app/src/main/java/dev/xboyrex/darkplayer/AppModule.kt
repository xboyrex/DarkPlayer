package dev.xboyrex.darkplayer

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import dev.xboyrex.darkplayer.core.common.di.DispatchersModule
import dev.xboyrex.darkplayer.core.data.DataModule
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.ThumbnailGenerationStrategy
import dev.xboyrex.darkplayer.feature.network.NetworkModule
import dev.xboyrex.darkplayer.feature.player.PlayerModule
import dev.xboyrex.darkplayer.feature.playlist.PlaylistModule
import dev.xboyrex.darkplayer.feature.videopicker.VideoPickerModule
import dev.xboyrex.darkplayer.settings.SettingsModule
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(
    includes = [
        DispatchersModule::class,
        DataModule::class,
        NetworkModule::class,
        PlaylistModule::class,
        PlayerModule::class,
        SettingsModule::class,
        VideoPickerModule::class,
    ],
)
@ComponentScan("dev.xboyrex.darkplayer.feature.more")
class AppModule {

    @KoinViewModel
    fun provideMainViewModel(preferencesRepository: PreferencesRepository) =
        MainViewModel(preferencesRepository = preferencesRepository)

    @Single
    fun provideImageLoader(
        context: Context,
        preferencesRepository: PreferencesRepository,
    ): ImageLoader = ImageLoader.Builder(context = context)
        .components {
            add(
                VideoThumbnailDecoder.Factory(
                    thumbnailStrategy = {
                        val preferences = preferencesRepository.applicationPreferences.value
                        when (preferences.thumbnailGenerationStrategy) {
                            ThumbnailGenerationStrategy.FIRST_FRAME -> ThumbnailStrategy.FirstFrame

                            ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE ->
                                ThumbnailStrategy.FrameAtPercentage(
                                    preferences.thumbnailFramePosition,
                                )

                            ThumbnailGenerationStrategy.HYBRID -> ThumbnailStrategy.Hybrid(
                                preferences.thumbnailFramePosition,
                            )
                        }
                    },
                ),
            )
        }
        .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
        .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(6))
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.30)
                .build()
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache(
            DiskCache.Builder()
                .fileSystem(FileSystem.SYSTEM)
                .directory(context.filesDir.resolve(relative = "thumbnails"))
                .maxSizeBytes(256L * 1024L * 1024L)
                .build(),
        )
        .crossfade(120)
        .build()
}
