package com.itheamc.aiassistant.core.di

import com.itheamc.aiassistant.platform.PlatformClipboardClipEntry
import com.itheamc.aiassistant.platform.PlatformDataStore
import com.itheamc.aiassistant.platform.PlatformFileDownloader
import com.itheamc.aiassistant.platform.PlatformStorageEnvironment
import com.itheamc.aiassistant.platform.PlatformToast
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlatformDataStore> { PlatformDataStore(androidApplication()) }
    single<PlatformClipboardClipEntry> { PlatformClipboardClipEntry() }
    single<PlatformToast> { PlatformToast(androidApplication()) }
    single<PlatformStorageEnvironment> { PlatformStorageEnvironment(androidApplication()) }
    single<PlatformFileDownloader> { PlatformFileDownloader(androidApplication()) }
}