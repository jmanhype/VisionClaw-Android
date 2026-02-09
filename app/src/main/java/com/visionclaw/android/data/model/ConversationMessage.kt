package com.visionclaw.android.data.model

/**
 * A single message in a conversation session.
 */
data class ConversationMessage(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val speaker: Speaker,
    val content: String,
    val type: MessageType
)

enum class Speaker { USER, GEMINI }

enum class MessageType { TEXT, TOOL_CALL, SYSTEM }
