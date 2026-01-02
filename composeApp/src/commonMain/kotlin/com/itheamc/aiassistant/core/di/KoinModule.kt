package com.itheamc.aiassistant.core.di

import com.itheamc.aiassistant.core.storage.StorageService
import com.itheamc.aiassistant.ui.features.ai.viewmodels.AgentShowcaseViewModel
import com.itheamc.aiassistant.ui.features.ai.viewmodels.AiAssistantViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

expect val platformModule: Module

val commonModules = module {
    singleOf(::StorageService)
    viewModelOf(::AiAssistantViewModel)
    viewModelOf(::AgentShowcaseViewModel)
}

val koinConfig: KoinConfiguration = koinConfiguration {
    modules(commonModules + platformModule)
}
