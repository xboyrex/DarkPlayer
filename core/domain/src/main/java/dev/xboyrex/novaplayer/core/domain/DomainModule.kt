package dev.xboyrex.novaplayer.core.domain

import dev.xboyrex.novaplayer.core.data.DataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataModule::class])
@ComponentScan
class DomainModule
