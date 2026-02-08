package com.visionclaw.android.openclaw

import android.util.Log
import com.visionclaw.android.gemini.FunctionCall
import com.visionclaw.android.gemini.GeminiConfig
import com.visionclaw.android.gemini.GeminiLiveService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes Gemini tool calls to OpenClaw and sends responses back.
 *
 * Flow:
 * 1. Receive FunctionCall from GeminiLiveService.toolCallChannel
 * 2. Extract task from args["task"]
 * 3. Send to OpenClawBridge.executeTask()
 * 4. Send result back via GeminiLiveService.sendToolResponse()
 *
 * TODO: Implement the routing loop.
 * - Launch coroutine that consumes toolCallChannel
 * - For each call: extract task, call OpenClaw, send response back
 * - Handle errors gracefully (send error text as tool response)
 */
@Singleton
class ToolCallRouter @Inject constructor(
    private val openClawBridge: OpenClawBridge,
    private val geminiLiveService: GeminiLiveService
) {
    companion object {
        private const val TAG = "ToolCallRouter"
    }

    suspend fun handleToolCall(call: FunctionCall) {
        TODO("Implement: extract task, call OpenClaw, send response to Gemini")
    }
}
