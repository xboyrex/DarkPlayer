package dev.xboyrex.darkplayer.core.media

import dev.xboyrex.darkplayer.core.common.di.DispatchersModule
import dev.xboyrex.darkplayer.core.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabaseModule::class, DispatchersModule::class])
@ComponentScan
class MediaModule
