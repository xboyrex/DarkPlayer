package dev.xboyrex.novaplayer.feature.videopicker

import dev.xboyrex.novaplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class VideoPickerModule
