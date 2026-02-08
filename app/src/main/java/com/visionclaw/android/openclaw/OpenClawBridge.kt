package com.visionclaw.android.openclaw

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.visionclaw.android.gemini.GeminiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP client for OpenClaw gateway.
 *
 * Sends task execution requests to OpenClaw's /v1/chat/completions endpoint.
 *
 * Implementation details:
 * - URL: {OPENCLAW_HOST}:{OPENCLAW_PORT}/v1/chat/completions
 * - Auth: Bearer {OPENCLAW_GATEWAY_TOKEN}
 * - Body: Chat completions format with the task as user message
 * - Parse response and extract result text
 */
@Singleton
class OpenClawBridge @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "OpenClawBridge"
    }

    /**
     * Execute a task via OpenClaw gateway.
     *
     * @param task The task description from Gemini's tool call
     * @return Result text to send back to Gemini as tool response
     */
    suspend fun executeTask(task: String): Result<String> = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isOpenClawConfigured) {
            return@withContext Result.failure(IllegalStateException("OpenClaw is not configured."))
        }

        val url = "${GeminiConfig.OPENCLAW_HOST}:${GeminiConfig.OPENCLAW_PORT}/v1/chat/completions"

        val requestBodyMap = mapOf(
            "messages" to listOf(
                mapOf("role" to "user", "content" to task)
            ),
            "model" to "openclaw"
        )

        val requestBodyJson = moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        ).toJson(requestBodyMap)

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer ${GeminiConfig.OPENCLAW_GATEWAY_TOKEN}")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("OpenClaw API error: ${response.code} ${response.message}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            
            // Parse response: {"choices": [{"message": {"content": "..."}}]}
            // Using raw map parsing for simplicity since we don't have response models
            val responseAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            
            val responseMap = responseAdapter.fromJson(responseBody)
            
            val choices = responseMap?.get("choices") as? List<*>
            val firstChoice = choices?.firstOrNull() as? Map<*, *>
            val message = firstChoice?.get("message") as? Map<*, *>
            val content = message?.get("content") as? String

            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Could not extract content from response"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "OpenClaw request failed", e)
            Result.failure(e)
        }
    }
}
