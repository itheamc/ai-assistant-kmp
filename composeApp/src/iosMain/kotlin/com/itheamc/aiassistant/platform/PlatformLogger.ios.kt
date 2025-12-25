package com.itheamc.aiassistant.platform

import platform.Foundation.NSLog

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object PlatformLogger {
    actual fun logMessage(message: String?) {
        NSLog("➡️➡️➡️ %s ⬅️⬅️⬅️", message ?: "null")
    }

    actual fun logSuccess(message: String?) {
        NSLog("✅✅✅ %s ✅✅✅", message ?: "null")
    }

    actual fun logError(error: String?) {
        NSLog("❌❌❌ %s ❌❌❌", error ?: "null")
    }

    actual fun logError(throwable: Throwable?) {
        logError(throwable?.message)
    }

    actual fun logInfo(info: String?) {
        NSLog("⚠️⚠️⚠️ %s ⚠️⚠️⚠️", info ?: "null")
    }
}