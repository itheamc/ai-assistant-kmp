package com.itheamc.aiassistant.platform

import android.content.Context

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformStorageEnvironment(private val context: Context) {
    actual fun cacheDir(): String = context.cacheDir.absolutePath
    actual fun dataDir(): String = context.applicationInfo.dataDir
    actual fun filesDir(): String = context.filesDir.absolutePath
    actual fun externalCacheDir(): String =
        context.externalCacheDir?.absolutePath ?: context.cacheDir.absolutePath
}