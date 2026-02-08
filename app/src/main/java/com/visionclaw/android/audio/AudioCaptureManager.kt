package com.visionclaw.android.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.visionclaw.android.gemini.GeminiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures PCM audio from the microphone.
 *
 * Output: PCM Int16, 16kHz mono, 100ms chunks (3200 bytes per chunk)
 *
 * TODO: Implement AudioRecord capture loop.
 * - Use AudioRecord with VOICE_COMMUNICATION source for echo cancellation
 * - Read in 100ms chunks
 * - Emit chunks via callback/channel to GeminiLiveService
 * - Support muting during AI speech playback
 */
@Singleton
class AudioCaptureManager @Inject constructor() {
    companion object {
        private const val TAG = "AudioCapture"
        const val SAMPLE_RATE = GeminiConfig.INPUT_SAMPLE_RATE
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        val CHUNK_SIZE = SAMPLE_RATE * 2 * GeminiConfig.AUDIO_CHUNK_DURATION_MS / 1000  // 3200 bytes
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    /**
     * Start capturing audio from the microphone.
     *
     * @param onChunk Callback invoked with each PCM chunk (100ms of audio)
     *
     * TODO: Implement:
     * 1. Create AudioRecord with VOICE_COMMUNICATION source
     * 2. Apply AcousticEchoCanceler if available
     * 3. Start recording loop in coroutine
     * 4. Read CHUNK_SIZE bytes per iteration
     * 5. If not muted, invoke onChunk with the PCM data
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCapture(scope: CoroutineScope, onChunk: (ByteArray) -> Unit) {
        TODO("Implement AudioRecord capture loop — see ARCHITECTURE.md audio pipeline")
    }

    fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _isRecording.value = false
    }

    /** Mute mic during AI speech to prevent echo/feedback */
    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }
}
