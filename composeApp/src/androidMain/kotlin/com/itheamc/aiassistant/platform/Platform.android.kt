package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Platform {
    actual fun isIOS(): Boolean = false
    actual fun isAndroid(): Boolean = true
}