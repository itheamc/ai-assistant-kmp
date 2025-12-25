package com.itheamc.aiassistant.platform

import android.util.Log

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object PlatformLogger {
    actual fun logMessage(message: String?) {
        Log.d("PlatformLogger", "➡️➡️➡️ $message ⬅️⬅️⬅️")
    }

    actual fun logSuccess(message: String?) {
        Log.d("PlatformLogger success", "✅✅✅ $message ✅✅✅")
    }

    actual fun logError(error: String?) {
        Log.d("PlatformLogger", "❌❌❌ $error ❌❌❌")
    }

    actual fun logError(throwable: Throwable?) {
        logError(throwable?.message)
    }

    actual fun logInfo(info: String?) {
        Log.d("PlatformLogger", "⚠️⚠️⚠️ $info ⚠️⚠️⚠️")
    }
}