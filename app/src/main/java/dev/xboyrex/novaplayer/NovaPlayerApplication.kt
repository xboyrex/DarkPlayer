package dev.xboyrex.novaplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.xboyrex.novaplayer.core.common.Logger
import dev.xboyrex.novaplayer.core.common.di.DiQualifiers
import dev.xboyrex.novaplayer.core.data.repository.NetworkConnectionRepository
import dev.xboyrex.novaplayer.core.media.network.keys.SshKeyStore
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyController
import dev.xboyrex.novaplayer.core.ui.glass.GlassQuality
import dev.xboyrex.novaplayer.core.ui.glass.GlassSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.core.qualifier.named
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class NovaPlayerApplication :
    Application(),
    SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    private val networkConnectionRepository: NetworkConnectionRepository by inject()

    private val sshKeyStore: SshKeyStore by inject()

    private val applicationScope: CoroutineScope by inject(
        qualifier = named(DiQualifiers.APPLICATION_SCOPE),
    )

    override fun onCreate() {
        super.onCreate()
        GlassSettings.init(this)
        GlassQuality.init(this)
        NovaDolbyController.init(this)

        startKoin<NovaPlayerApplication> {
            allowOverride(override = false)
            androidContext(androidContext = this@NovaPlayerApplication)
        }
        applicationScope.launch {
            runCatching {
                initializeSshKeyStore(
                    repository = networkConnectionRepository,
                    sshKeyStore = sshKeyStore,
                )
            }.onFailure { error ->
                Logger.logError(
                    tag = TAG,
                    message = "Couldn't reconcile SSH keys: ${error.message}",
                )
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    private companion object {
        const val TAG = "NovaPlayerApplication"
    }
}
