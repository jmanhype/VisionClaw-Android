package com.visionclaw.android.ui.viewmodels

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionclaw.android.audio.AudioCaptureManager
import com.visionclaw.android.audio.AudioPlaybackManager
import com.visionclaw.android.camera.GlassesCameraManager
import com.visionclaw.android.camera.PhoneCameraManager
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
    private val toolCallRouter: ToolCallRouter
) : ViewModel() {

    enum class SessionMode { PHONE, GLASSES }
    enum class SessionState { IDLE, CONNECTING, ACTIVE, ERROR }

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _sessionMode = MutableStateFlow(SessionMode.PHONE)
    val sessionMode: StateFlow<SessionMode> = _sessionMode

    val connectionState = geminiService.connectionState

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

        try {
            // 1. Connect WebSocket
            geminiService.connect()

            // 2. Start Audio Pipeline
            try {
                audioCaptureManager.startCapture(viewModelScope) { chunk ->
                    geminiService.sendAudio(chunk)
                }
            } catch (e: SecurityException) {
                _sessionState.value = SessionState.ERROR
                return
            }
            
            audioPlaybackManager.startPlayback(viewModelScope, geminiService.audioOutputChannel)
            audioPlaybackManager.onPlaybackStateChanged = { isPlaying ->
                audioCaptureManager.setMuted(isPlaying)
            }

            // 3. Start Camera
            if (mode == SessionMode.GLASSES) {
                glassesCameraManager.startStreaming { jpegBytes ->
                    geminiService.sendVideoFrame(jpegBytes)
                }
            }
            // For PHONE mode, camera is started via bindCamera from UI

            // 4. Handle Tool Calls
            viewModelScope.launch(Dispatchers.IO) {
                for (call in geminiService.toolCallChannel) {
                    toolCallRouter.handleToolCall(call)
                }
            }

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
        _sessionState.value = SessionState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
