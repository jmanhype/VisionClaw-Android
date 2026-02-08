package com.visionclaw.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionclaw.android.audio.AudioCaptureManager
import com.visionclaw.android.audio.AudioPlaybackManager
import com.visionclaw.android.camera.PhoneCameraManager
import com.visionclaw.android.gemini.GeminiLiveService
import com.visionclaw.android.openclaw.ToolCallRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel managing the AI session lifecycle.
 *
 * Wires together:
 * - GeminiLiveService (WebSocket)
 * - AudioCaptureManager (mic -> Gemini)
 * - AudioPlaybackManager (Gemini -> speaker)
 * - PhoneCameraManager (camera -> Gemini)
 * - ToolCallRouter (Gemini tool calls -> OpenClaw -> responses)
 *
 * TODO: Implement session lifecycle:
 * 1. startSession(): connect Gemini, start audio capture + playback, start camera
 * 2. Wire audio capture onChunk -> geminiService.sendAudio()
 * 3. Wire camera onFrame -> geminiService.sendVideoFrame()
 * 4. Wire audioOutputChannel -> audioPlaybackManager
 * 5. Wire toolCallChannel -> toolCallRouter
 * 6. Mute mic during AI speech (playback callback)
 * 7. stopSession(): disconnect everything
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val geminiService: GeminiLiveService,
    private val audioCaptureManager: AudioCaptureManager,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val phoneCameraManager: PhoneCameraManager,
    private val toolCallRouter: ToolCallRouter
) : ViewModel() {

    enum class SessionMode { PHONE, GLASSES }
    enum class SessionState { IDLE, CONNECTING, ACTIVE, ERROR }

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _sessionMode = MutableStateFlow(SessionMode.PHONE)
    val sessionMode: StateFlow<SessionMode> = _sessionMode

    val connectionState = geminiService.connectionState

    fun startSession(mode: SessionMode) {
        TODO("Wire all components together and start the session")
    }

    fun stopSession() {
        TODO("Disconnect all components")
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
