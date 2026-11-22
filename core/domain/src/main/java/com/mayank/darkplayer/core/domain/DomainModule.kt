package com.mayank.darkplayer.core.domain

import com.mayank.darkplayer.core.data.DataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataModule::class])
@ComponentScan
class DomainModule
