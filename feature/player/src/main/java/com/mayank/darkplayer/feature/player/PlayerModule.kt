package com.mayank.darkplayer.feature.player

import com.mayank.darkplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class PlayerModule
