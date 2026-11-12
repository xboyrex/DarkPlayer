package dev.xboyrex.darkplayer.feature.network

import dev.xboyrex.darkplayer.core.data.DataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataModule::class])
@ComponentScan
class NetworkModule
