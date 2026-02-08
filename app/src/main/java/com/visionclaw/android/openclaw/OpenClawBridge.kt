package com.visionclaw.android.openclaw

import android.util.Log
import com.squareup.moshi.Moshi
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
 * TODO: Implement HTTP POST to OpenClaw.
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
     *
     * TODO: Implement:
     * 1. Build request URL from GeminiConfig.OPENCLAW_HOST/PORT
     * 2. Create JSON body: {"messages": [{"role": "user", "content": task}]}
     * 3. Add Authorization header: "Bearer {OPENCLAW_GATEWAY_TOKEN}"
     * 4. POST and parse response
     * 5. Return the assistant message content
     */
    suspend fun executeTask(task: String): Result<String> = withContext(Dispatchers.IO) {
        TODO("Implement OpenClaw HTTP request — see ARCHITECTURE.md")
    }
}
