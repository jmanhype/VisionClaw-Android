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
 * TODO: Implement AudioTrack streaming playback.
 * - Create AudioTrack in streaming mode (24kHz, mono, PCM 16-bit)
 * - Drain audio channel and write to AudioTrack
 * - Signal AudioCaptureManager to mute mic during playback
 * - Signal when playback completes (turn complete)
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
     *
     * TODO: Implement:
     * 1. Create AudioTrack (STREAM_MUSIC, 24kHz, MONO, PCM_16BIT, STREAM mode)
     * 2. Launch coroutine to receive from audioChannel
     * 3. Write chunks to AudioTrack
     * 4. Call onPlaybackStateChanged(true) when starting, (false) when idle
     */
    fun startPlayback(scope: CoroutineScope, audioChannel: Channel<ByteArray>) {
        TODO("Implement AudioTrack streaming playback — see ARCHITECTURE.md")
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        onPlaybackStateChanged?.invoke(false)
    }
}
