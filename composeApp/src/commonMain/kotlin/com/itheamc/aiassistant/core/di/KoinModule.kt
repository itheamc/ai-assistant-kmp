package com.itheamc.aiassistant.core.di

import org.koin.core.module.Module
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

expect val platformModule: Module

val commonModules = module {

}

val koinConfig: KoinConfiguration = koinConfiguration {
    modules(commonModules + platformModule)
}
