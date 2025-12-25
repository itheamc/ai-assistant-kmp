package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformStorageEnvironment {
    fun cacheDir(): String
    fun dataDir(): String
    fun filesDir(): String
    fun externalCacheDir(): String
}