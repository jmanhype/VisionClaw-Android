package com.visionclaw.android.data.model

/**
 * Metadata for a conversation session.
 */
data class ConversationSession(
    val id: String,
    val startTimeMillis: Long,
    val mode: String  // "PHONE" or "GLASSES"
)
