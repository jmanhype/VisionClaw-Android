package com.visionclaw.android.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
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
 * Implementation details:
 * - Uses AudioRecord with VOICE_COMMUNICATION source for echo cancellation
 * - Reads in 100ms chunks
 * - Emits chunks via callback to GeminiLiveService
 * - Supports muting during AI speech playback
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
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCapture(scope: CoroutineScope, onChunk: (ByteArray) -> Unit) {
        if (_isRecording.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            if (AcousticEchoCanceler.isAvailable()) {
                val aec = AcousticEchoCanceler.create(audioRecord!!.audioSessionId)
                if (aec != null) {
                    aec.enabled = true
                    Log.d(TAG, "AcousticEchoCanceler enabled")
                } else {
                    Log.w(TAG, "AcousticEchoCanceler creation failed")
                }
            } else {
                Log.w(TAG, "AcousticEchoCanceler not available")
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            Log.d(TAG, "Recording started")

            captureJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE)
                while (isActive && _isRecording.value) {
                    val readResult = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: 0
                    if (readResult > 0) {
                        if (!_isMuted.value) {
                            onChunk(buffer.copyOf(readResult))
                        }
                    } else {
                        Log.w(TAG, "AudioRecord read error: $readResult")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture: ${e.message}", e)
            stopCapture()
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture: ${e.message}")
        }
        audioRecord = null
        _isRecording.value = false
        Log.d(TAG, "Recording stopped")
    }

    /** Mute mic during AI speech to prevent echo/feedback */
    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }
}
