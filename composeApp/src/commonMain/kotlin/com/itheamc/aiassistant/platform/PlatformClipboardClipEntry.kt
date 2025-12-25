package com.itheamc.aiassistant.platform

import androidx.compose.ui.platform.ClipEntry

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformClipboardClipEntry {
    fun get(text: String): ClipEntry
}