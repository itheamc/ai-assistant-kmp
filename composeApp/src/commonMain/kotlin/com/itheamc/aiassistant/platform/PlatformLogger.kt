package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object PlatformLogger {
    fun logMessage(message: String?)
    fun logSuccess(message: String?)
    fun logError(error: String?)
    fun logError(throwable: Throwable?)
    fun logInfo(info: String?)
}