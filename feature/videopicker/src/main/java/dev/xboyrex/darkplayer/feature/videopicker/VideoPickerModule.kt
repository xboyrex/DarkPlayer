package dev.xboyrex.darkplayer.feature.videopicker

import dev.xboyrex.darkplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class VideoPickerModule
