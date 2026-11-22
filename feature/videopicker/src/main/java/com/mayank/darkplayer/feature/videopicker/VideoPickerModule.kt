package com.mayank.darkplayer.feature.videopicker

import com.mayank.darkplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class VideoPickerModule
