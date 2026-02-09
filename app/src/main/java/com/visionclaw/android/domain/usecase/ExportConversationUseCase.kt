package com.visionclaw.android.domain.usecase

import com.visionclaw.android.data.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Formats a conversation session for export and provides the text for sharing.
 */
class ExportConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    /**
     * Returns formatted export text for the given session, or null if session not found.
     */
    suspend fun formatForExport(sessionId: String): String? = withContext(Dispatchers.IO) {
        conversationRepository.formatSessionForExport(sessionId)
    }
}
