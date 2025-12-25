package com.itheamc.aiassistant.core.di

import com.itheamc.aiassistant.core.storage.StorageService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

expect val platformModule: Module

val commonModules = module {
    singleOf(::StorageService)
}

val koinConfig: KoinConfiguration = koinConfiguration {
    modules(commonModules + platformModule)
}
