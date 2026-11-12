package dev.xboyrex.novaplayer

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dev.xboyrex.novaplayer.core.common.di.DispatchersModule
import dev.xboyrex.novaplayer.core.data.DataModule
import dev.xboyrex.novaplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.novaplayer.core.model.ThumbnailGenerationStrategy
import dev.xboyrex.novaplayer.feature.network.NetworkModule
import dev.xboyrex.novaplayer.feature.player.PlayerModule
import dev.xboyrex.novaplayer.feature.playlist.PlaylistModule
import dev.xboyrex.novaplayer.feature.videopicker.VideoPickerModule
import dev.xboyrex.novaplayer.settings.SettingsModule
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
@ComponentScan("dev.xboyrex.novaplayer.feature.more")
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
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache(
            DiskCache.Builder()
                .fileSystem(FileSystem.SYSTEM)
                .directory(context.filesDir.resolve(relative = "thumbnails"))
                .maxSizePercent(percent = 1.0)
                .build(),
        )
        .crossfade(enable = true)
        .build()
}
