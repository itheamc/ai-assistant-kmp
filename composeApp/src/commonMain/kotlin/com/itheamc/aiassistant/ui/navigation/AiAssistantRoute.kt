package com.itheamc.aiassistant.ui.navigation


enum class AiAssistantRoute {
    Splash,
    Onboarding,
    AiChat;

    companion object Companion {
        fun fromStr(value: String?): AiAssistantRoute? {
            return when {
                value?.contains(Splash.name) == true -> Splash
                value?.contains(Onboarding.name) == true -> Onboarding
                value?.contains(AiChat.name) == true -> AiChat
                else -> null
            }
        }
    }
}
