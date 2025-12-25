package com.itheamc.aiassistant.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformClipboardClipEntry {
    @OptIn(ExperimentalComposeUiApi::class)
    actual fun get(text: String): ClipEntry {
        return ClipEntry.withPlainText(text)
    }
}