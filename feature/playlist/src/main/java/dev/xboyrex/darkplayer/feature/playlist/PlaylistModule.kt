package dev.xboyrex.darkplayer.feature.playlist

import dev.xboyrex.darkplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class PlaylistModule
