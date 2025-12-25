package com.itheamc.aiassistant.platform

import android.content.Context
import android.widget.Toast

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformToast(private val context: Context) {
    actual fun show(message: String, duration: Int) {
        try {
            val length = if (duration > 1) LENGTH_LONG else if (duration < 0) LENGTH_SHORT else duration

            Toast.makeText(context, message, length).show()
        } catch (_: Exception) {
            // Do nothing here
        }
    }

    actual companion object {
        actual val LENGTH_LONG: Int = 1
        actual val LENGTH_SHORT: Int = 0
    }
}