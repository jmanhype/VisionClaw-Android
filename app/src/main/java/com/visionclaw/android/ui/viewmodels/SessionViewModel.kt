package com.visionclaw.android.ui.viewmodels

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionclaw.android.audio.AudioCaptureManager
import com.visionclaw.android.audio.AudioPlaybackManager
import com.visionclaw.android.camera.GlassesCameraManager
import com.visionclaw.android.camera.PhoneCameraManager
import com.visionclaw.android.data.model.ConversationMessage
import com.visionclaw.android.data.model.MessageType
import com.visionclaw.android.data.model.Speaker
import com.visionclaw.android.data.repository.ConversationRepository
import com.visionclaw.android.domain.usecase.ExportConversationUseCase
import com.visionclaw.android.gemini.GeminiLiveService
import com.visionclaw.android.openclaw.ToolCallRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the AI session lifecycle.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val geminiService: GeminiLiveService,
    private val audioCaptureManager: AudioCaptureManager,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val phoneCameraManager: PhoneCameraManager,
    private val glassesCameraManager: GlassesCameraManager,
    private val toolCallRouter: ToolCallRouter,
    private val conversationRepository: ConversationRepository,
    private val exportConversationUseCase: ExportConversationUseCase
) : ViewModel() {

    enum class SessionMode { PHONE, GLASSES }
    enum class SessionState { IDLE, CONNECTING, ACTIVE, ERROR }

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _sessionMode = MutableStateFlow(SessionMode.PHONE)
    val sessionMode: StateFlow<SessionMode> = _sessionMode

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    val connectionState = geminiService.connectionState
    val conversationMessages = conversationRepository.currentSessionMessages

    init {
        // Monitor connection state to update session state
        viewModelScope.launch {
            geminiService.connectionState.collect { state ->
                when (state) {
                    GeminiLiveService.ConnectionState.CONNECTED -> {
                        _sessionState.value = SessionState.ACTIVE
                    }
                    GeminiLiveService.ConnectionState.DISCONNECTED -> {
                        if (_sessionState.value != SessionState.IDLE) {
                             // Only go to idle if we were running
                             _sessionState.value = SessionState.IDLE
                        }
                    }
                    GeminiLiveService.ConnectionState.ERROR -> {
                        _sessionState.value = SessionState.ERROR
                    }
                    GeminiLiveService.ConnectionState.CONNECTING -> {
                        _sessionState.value = SessionState.CONNECTING
                    }
                }
            }
        }
    }

    fun startSession(mode: SessionMode) {
        if (_sessionState.value == SessionState.ACTIVE || _sessionState.value == SessionState.CONNECTING) return

        _sessionMode.value = mode
        _sessionState.value = SessionState.CONNECTING
        val sessionId = "sess_${System.currentTimeMillis()}"

        try {
            // 1. Connect WebSocket
            geminiService.connect()

            // 2. Start conversation session then collect Gemini text and tool calls
            viewModelScope.launch(Dispatchers.IO) {
                conversationRepository.startSession(sessionId, mode.name)
                _currentSessionId.value = sessionId

                launch {
                    geminiService.geminiTextFlow.collect { text ->
                        val sid = _currentSessionId.value ?: return@collect
                        conversationRepository.addMessage(
                            ConversationMessage(
                                id = "msg_${System.currentTimeMillis()}_${(0..9999).random()}",
                                sessionId = sid,
                                timestamp = System.currentTimeMillis(),
                                speaker = Speaker.GEMINI,
                                content = text,
                                type = MessageType.TEXT
                            )
                        )
                    }
                }
                launch {
                    for (call in geminiService.toolCallChannel) {
                        val sid = _currentSessionId.value
                        if (sid != null && call.name == "execute") {
                            val task = call.args["task"] ?: ""
                            conversationRepository.addMessage(
                                ConversationMessage(
                                    id = "msg_${System.currentTimeMillis()}_tc",
                                    sessionId = sid,
                                    timestamp = System.currentTimeMillis(),
                                    speaker = Speaker.GEMINI,
                                    content = "execute: $task",
                                    type = MessageType.TOOL_CALL
                                )
                            )
                        }
                        toolCallRouter.handleToolCall(call)
                    }
                }
            }

            // 3. Start Audio Pipeline
            try {
                audioCaptureManager.startCapture(viewModelScope) { chunk ->
                    geminiService.sendAudio(chunk)
                }
            } catch (e: SecurityException) {
                _sessionState.value = SessionState.ERROR
                return
            }
            
            audioPlaybackManager.startPlayback(viewModelScope, geminiService.audioOutputChannel, geminiService.turnCompleteChannel)
            audioPlaybackManager.onPlaybackStateChanged = { isPlaying ->
                audioCaptureManager.setMuted(isPlaying)
            }

            // 4. Start Camera
            if (mode == SessionMode.GLASSES) {
                glassesCameraManager.startStreaming { jpegBytes ->
                    geminiService.sendVideoFrame(jpegBytes)
                }
            }
            // For PHONE mode, camera is started via bindCamera from UI

        } catch (e: Exception) {
            _sessionState.value = SessionState.ERROR
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        if (_sessionMode.value == SessionMode.PHONE) {
            phoneCameraManager.startCapture(lifecycleOwner, surfaceProvider) { jpegBytes ->
                geminiService.sendVideoFrame(jpegBytes)
            }
        }
    }

    fun stopSession() {
        geminiService.disconnect()
        audioCaptureManager.stopCapture()
        audioPlaybackManager.stopPlayback()
        phoneCameraManager.stopCapture()
        glassesCameraManager.stopStreaming()
        conversationRepository.endSession()
        // Keep _currentSessionId so Export/Copy still work for this session after stop
        _sessionState.value = SessionState.IDLE
    }

    /** Returns formatted export text for the current session, or null if none. */
    suspend fun getExportTextForCurrentSession(): String? {
        val sid = _currentSessionId.value ?: return null
        return exportConversationUseCase.formatForExport(sid)
    }

    /** Returns formatted export text for a given session (e.g. from history). */
    suspend fun getExportTextForSession(sessionId: String): String? {
        return exportConversationUseCase.formatForExport(sessionId)
    }

    fun getAllSessions() = conversationRepository.getAllSessions()
    suspend fun deleteSession(sessionId: String) = conversationRepository.deleteSession(sessionId)
    suspend fun clearAllHistory() = conversationRepository.clearAllSessions()

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
