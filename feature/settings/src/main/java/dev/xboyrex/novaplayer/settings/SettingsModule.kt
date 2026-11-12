package dev.xboyrex.novaplayer.settings

import dev.xboyrex.novaplayer.core.domain.DomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainModule::class])
@ComponentScan
class SettingsModule
