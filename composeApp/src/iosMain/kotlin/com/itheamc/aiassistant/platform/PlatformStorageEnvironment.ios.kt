package com.itheamc.aiassistant.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformStorageEnvironment() {
    @OptIn(ExperimentalForeignApi::class)
    actual fun cacheDir(): String =
        NSFileManager.defaultManager.URLForDirectory(
            NSCachesDirectory,
            NSUserDomainMask,
            null,
            true,
            null
        )?.path ?: ""

    @OptIn(ExperimentalForeignApi::class)
    actual fun dataDir(): String =
        NSFileManager.defaultManager.URLForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask,
            null,
            true,
            null
        )?.path ?: ""

    @OptIn(ExperimentalForeignApi::class)
    actual fun filesDir(): String =
        NSFileManager.defaultManager.URLForDirectory(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            null,
            true,
            null
        )?.path ?: ""

    actual fun externalCacheDir(): String = cacheDir() // iOS does not have external storage
}