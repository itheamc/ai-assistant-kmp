package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformToast {
    fun show(message: String, duration: Int = LENGTH_SHORT)

    companion object {
        val LENGTH_LONG: Int
        val LENGTH_SHORT: Int
    }
}