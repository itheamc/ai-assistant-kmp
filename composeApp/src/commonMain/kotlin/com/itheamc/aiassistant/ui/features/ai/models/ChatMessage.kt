package com.itheamc.aiassistant.ui.features.ai.models

data class ChatMessage(
    val id: String,
    val text: String,
    val participant: Participant,
    val isPending: Boolean = false,
    val timestamp: String
) {
    val self: Boolean
        get() = participant == Participant.USER

    val sender: String
        get() = if (self) "You" else "Ai Assistant"
}

enum class Participant {
    USER, AI
}


