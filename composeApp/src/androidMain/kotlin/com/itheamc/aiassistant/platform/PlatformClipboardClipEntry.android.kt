package com.itheamc.aiassistant.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformClipboardClipEntry {
    actual fun get(text: String): ClipEntry {
        return ClipEntry(ClipData.newPlainText(text, text))
    }
}