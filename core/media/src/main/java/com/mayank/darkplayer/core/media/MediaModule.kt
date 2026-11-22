package com.mayank.darkplayer.core.media

import com.mayank.darkplayer.core.common.di.DispatchersModule
import com.mayank.darkplayer.core.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabaseModule::class, DispatchersModule::class])
@ComponentScan
class MediaModule
