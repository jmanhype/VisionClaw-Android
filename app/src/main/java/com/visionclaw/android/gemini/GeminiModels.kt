package com.visionclaw.android.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Outbound Messages (App -> Gemini) ---

@JsonClass(generateAdapter = true)
data class SetupMessage(
    val setup: SetupConfig
)

@JsonClass(generateAdapter = true)
data class SetupConfig(
    val model: String,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseModalities") val responseModalities: List<String> = listOf("AUDIO"),
    @Json(name = "speechConfig") val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: VoiceConfig
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String = "Aoede"
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    val data: String  // base64 encoded
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "functionDeclarations") val functionDeclarations: List<FunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

@JsonClass(generateAdapter = true)
data class FunctionParameters(
    val type: String = "object",
    val properties: Map<String, PropertySchema>,
    val required: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PropertySchema(
    val type: String,
    val description: String
)

// Realtime input (audio + video frames)
@JsonClass(generateAdapter = true)
data class RealtimeInputMessage(
    @Json(name = "realtimeInput") val realtimeInput: RealtimeInput
)

@JsonClass(generateAdapter = true)
data class RealtimeInput(
    @Json(name = "mediaChunks") val mediaChunks: List<MediaChunk>
)

@JsonClass(generateAdapter = true)
data class MediaChunk(
    @Json(name = "mimeType") val mimeType: String,
    val data: String  // base64 encoded
)

// Tool response
@JsonClass(generateAdapter = true)
data class ToolResponseMessage(
    @Json(name = "toolResponse") val toolResponse: ToolResponse
)

@JsonClass(generateAdapter = true)
data class ToolResponse(
    @Json(name = "functionResponses") val functionResponses: List<FunctionResponse>
)

@JsonClass(generateAdapter = true)
data class FunctionResponse(
    val id: String,
    val response: Map<String, String>
)

// --- Inbound Messages (Gemini -> App) ---

@JsonClass(generateAdapter = true)
data class ServerContentMessage(
    @Json(name = "serverContent") val serverContent: ServerContent? = null,
    @Json(name = "toolCall") val toolCall: ToolCallMessage? = null,
    @Json(name = "setupComplete") val setupComplete: Any? = null
)

@JsonClass(generateAdapter = true)
data class ServerContent(
    @Json(name = "modelTurn") val modelTurn: ModelTurn? = null,
    @Json(name = "turnComplete") val turnComplete: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ModelTurn(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ToolCallMessage(
    @Json(name = "functionCalls") val functionCalls: List<FunctionCall>
)

@JsonClass(generateAdapter = true)
data class FunctionCall(
    val id: String,
    val name: String,
    val args: Map<String, String>
)
