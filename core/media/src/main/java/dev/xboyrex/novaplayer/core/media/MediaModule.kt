package dev.xboyrex.novaplayer.core.media

import dev.xboyrex.novaplayer.core.common.di.DispatchersModule
import dev.xboyrex.novaplayer.core.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabaseModule::class, DispatchersModule::class])
@ComponentScan
class MediaModule
