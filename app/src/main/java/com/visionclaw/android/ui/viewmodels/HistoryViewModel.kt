package com.visionclaw.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionclaw.android.data.model.ConversationSession
import com.visionclaw.android.data.repository.ConversationRepository
import com.visionclaw.android.domain.usecase.ExportConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val exportConversationUseCase: ExportConversationUseCase
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<ConversationSession>>(emptyList())
    val sessions: StateFlow<List<ConversationSession>> = _sessions.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = conversationRepository.getAllSessions()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            conversationRepository.deleteSession(sessionId)
            loadSessions()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            conversationRepository.clearAllSessions()
            loadSessions()
        }
    }

    suspend fun getExportText(sessionId: String): String? {
        return exportConversationUseCase.formatForExport(sessionId)
    }
}
