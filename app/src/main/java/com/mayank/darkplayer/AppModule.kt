package com.mayank.darkplayer

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.mayank.darkplayer.core.common.di.DispatchersModule
import com.mayank.darkplayer.core.data.DataModule
import com.mayank.darkplayer.core.data.repository.PreferencesRepository
import com.mayank.darkplayer.core.model.ThumbnailGenerationStrategy
import com.mayank.darkplayer.feature.network.NetworkModule
import com.mayank.darkplayer.feature.player.PlayerModule
import com.mayank.darkplayer.feature.playlist.PlaylistModule
import com.mayank.darkplayer.feature.videopicker.VideoPickerModule
import com.mayank.darkplayer.settings.SettingsModule
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
@ComponentScan("com.mayank.darkplayer.feature.more")
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
