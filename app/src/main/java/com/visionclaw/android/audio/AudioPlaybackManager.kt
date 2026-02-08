package com.visionclaw.android.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.visionclaw.android.gemini.GeminiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays PCM audio received from Gemini Live API.
 *
 * Input: PCM Int16, 24kHz mono chunks from WebSocket
 *
 * Implementation details:
 * - Creates AudioTrack in streaming mode (24kHz, mono, PCM 16-bit)
 * - Drains audio channel and writes to AudioTrack
 * - Signals AudioCaptureManager to mute mic during playback
 */
@Singleton
class AudioPlaybackManager @Inject constructor() {
    companion object {
        private const val TAG = "AudioPlayback"
        const val SAMPLE_RATE = GeminiConfig.OUTPUT_SAMPLE_RATE
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    /** Callback to mute/unmute mic during playback */
    var onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null

    /**
     * Start the playback loop, draining from the audio channel.
     * Also monitors turnComplete to unmute mic when AI finishes speaking.
     *
     * @param audioChannel Channel that receives PCM 24kHz chunks from Gemini
     * @param turnCompleteChannel Channel that signals when AI turn is complete
     */
    fun startPlayback(
        scope: CoroutineScope,
        audioChannel: Channel<ByteArray>,
        turnCompleteChannel: Channel<Unit>? = null
    ) {
        if (audioTrack != null) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBufferSize, SAMPLE_RATE * 2)) // ~1 sec buffer
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        Log.d(TAG, "AudioTrack started")

        val isPlaying = AtomicBoolean(false)

        // Audio drain loop
        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                for (chunk in audioChannel) {
                    // Only notify on transition from not-playing to playing
                    if (isPlaying.compareAndSet(false, true)) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            onPlaybackStateChanged?.invoke(true)
                        }
                    }
                    audioTrack?.write(chunk, 0, chunk.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in playback loop: ${e.message}", e)
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    isPlaying.set(false)
                    onPlaybackStateChanged?.invoke(false)
                }
            }
        }

        // Turn complete listener — unmutes mic when AI finishes speaking
        if (turnCompleteChannel != null) {
            scope.launch(Dispatchers.IO) {
                for (signal in turnCompleteChannel) {
                    if (isPlaying.compareAndSet(true, false)) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            onPlaybackStateChanged?.invoke(false)
                        }
                    }
                }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack: ${e.message}")
        }
        audioTrack = null
        onPlaybackStateChanged?.invoke(false)
        Log.d(TAG, "AudioTrack stopped")
    }
}
