package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Platform {
    fun isIOS(): Boolean
    fun isAndroid(): Boolean
}