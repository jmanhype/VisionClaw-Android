package com.visionclaw.android.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.visionclaw.android.gemini.GeminiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
     *
     * @param audioChannel Channel that receives PCM 24kHz chunks from Gemini
     */
    fun startPlayback(scope: CoroutineScope, audioChannel: Channel<ByteArray>) {
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

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                // We use a small timeout to detect end of speech if the stream pauses,
                // but primarily we toggle on/off based on channel activity.
                // Since this is a simple implementation, we assume that while we are draining
                // the channel, we are playing.

                for (chunk in audioChannel) {
                    // Notify we are starting to play
                    withContext(Dispatchers.Main) {
                        onPlaybackStateChanged?.invoke(true)
                    }

                    audioTrack?.write(chunk, 0, chunk.size)
                    
                    // Note: We don't immediately set false here because speech comes in chunks.
                    // Ideally we'd use 'turnComplete' from Gemini, but for now we rely on the
                    // flow of data.
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in playback loop: ${e.message}", e)
            } finally {
                // When channel is closed or job cancelled
                withContext(Dispatchers.Main) {
                    onPlaybackStateChanged?.invoke(false)
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
