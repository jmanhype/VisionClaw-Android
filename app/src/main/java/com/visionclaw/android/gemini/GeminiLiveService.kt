package com.visionclaw.android.gemini

import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import com.visionclaw.android.openclaw.ToolDeclarations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket client for Gemini Live API.
 *
 * Handles:
 * - Connection setup with model config + system instruction + tool declarations
 * - Sending PCM audio chunks (16kHz mono Int16, base64-encoded)
 * - Sending JPEG video frames (base64-encoded)
 * - Receiving PCM audio responses (24kHz mono Int16)
 * - Receiving tool calls and sending tool responses
 *
 * Reference: docs/ARCHITECTURE.md for the full protocol spec.
 */
@Singleton
class GeminiLiveService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "GeminiLiveService"
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Channel for received audio data (PCM 24kHz Int16 bytes)
    val audioOutputChannel = Channel<ByteArray>(Channel.BUFFERED)

    // Channel for received tool calls
    val toolCallChannel = Channel<FunctionCall>(Channel.BUFFERED)

    /** Emits Gemini text response parts (from modelTurn.parts) for conversation logging */
    val geminiTextFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    // Signal when AI finishes speaking (turnComplete received)
    val turnCompleteChannel = Channel<Unit>(Channel.CONFLATED)

    private var webSocket: WebSocket? = null
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Connect to Gemini Live API and send setup message.
     */
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        serviceScope.cancel()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val request = Request.Builder()
            .url(GeminiConfig.websocketUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened")
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $code $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}", t)
                _connectionState.value = ConnectionState.ERROR
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val setupMessage = SetupMessage(
            setup = SetupConfig(
                model = GeminiConfig.MODEL,
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = SpeechConfig(
                        voiceConfig = VoiceConfig(
                            prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Aoede")
                        )
                    )
                ),
                systemInstruction = Content(
                    parts = listOf(Part(text = GeminiConfig.SYSTEM_INSTRUCTION))
                ),
                tools = listOf(ToolDeclarations.executeTool)
            )
        )

        val json = moshi.adapter(SetupMessage::class.java).toJson(setupMessage)
        Log.d(TAG, "Sending Setup: $json")
        ws.send(json)
    }

    private fun handleMessage(text: String) {
        try {
            val message = moshi.adapter(ServerContentMessage::class.java).fromJson(text) ?: return

            if (message.setupComplete != null) {
                Log.d(TAG, "Setup Complete")
                _connectionState.value = ConnectionState.CONNECTED
            }

            message.serverContent?.let { serverContent ->
                serverContent.modelTurn?.parts?.forEach { part ->
                    part.text?.let { text ->
                        if (text.isNotBlank()) {
                            serviceScope.launch {
                                geminiTextFlow.emit(text)
                            }
                        }
                    }
                    part.inlineData?.let { inlineData ->
                        if (inlineData.mimeType.startsWith("audio/pcm")) {
                            try {
                                val audioBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                                serviceScope.launch {
                                    audioOutputChannel.send(audioBytes)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding audio: ${e.message}")
                            }
                        }
                    }
                }

                // Handle turn completion — signals end of AI speech
                if (serverContent.turnComplete == true) {
                    Log.d(TAG, "Turn Complete")
                    serviceScope.launch {
                        turnCompleteChannel.send(Unit)
                    }
                }
            }

            message.toolCall?.functionCalls?.forEach { functionCall ->
                Log.d(TAG, "Received Tool Call: ${functionCall.name}")
                serviceScope.launch {
                    toolCallChannel.send(functionCall)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
        }
    }

    /**
     * Send PCM audio chunk to Gemini.
     *
     * @param pcmData Raw PCM Int16 bytes (16kHz mono)
     */
    fun sendAudio(pcmData: ByteArray) {
        if (_connectionState.value != ConnectionState.CONNECTED) return

        val base64Audio = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        val message = RealtimeInputMessage(
            realtimeInput = RealtimeInput(
                mediaChunks = listOf(
                    MediaChunk(
                        mimeType = "audio/pcm;rate=${GeminiConfig.INPUT_SAMPLE_RATE}",
                        data = base64Audio
                    )
                )
            )
        )

        val json = moshi.adapter(RealtimeInputMessage::class.java).toJson(message)
        webSocket?.send(json)
    }

    /**
     * Send JPEG video frame to Gemini.
     *
     * @param jpegData JPEG bytes
     */
    fun sendVideoFrame(jpegData: ByteArray) {
        if (_connectionState.value != ConnectionState.CONNECTED) return

        val base64Image = Base64.encodeToString(jpegData, Base64.NO_WRAP)
        val message = RealtimeInputMessage(
            realtimeInput = RealtimeInput(
                mediaChunks = listOf(
                    MediaChunk(
                        mimeType = "image/jpeg",
                        data = base64Image
                    )
                )
            )
        )

        val json = moshi.adapter(RealtimeInputMessage::class.java).toJson(message)
        webSocket?.send(json)
    }

    /**
     * Send tool response back to Gemini after OpenClaw execution.
     *
     * @param callId The function call ID from the original tool call
     * @param result The result text from OpenClaw
     */
    fun sendToolResponse(callId: String, result: String) {
        if (_connectionState.value != ConnectionState.CONNECTED) return

        val message = ToolResponseMessage(
            toolResponse = ToolResponse(
                functionResponses = listOf(
                    FunctionResponse(
                        id = callId,
                        response = mapOf("result" to result)
                    )
                )
            )
        )

        val json = moshi.adapter(ToolResponseMessage::class.java).toJson(message)
        webSocket?.send(json)
    }

    /**
     * Disconnect from Gemini Live API.
     */
    fun disconnect() {
        serviceScope.cancel()
        webSocket?.close(1000, "User ended session")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
