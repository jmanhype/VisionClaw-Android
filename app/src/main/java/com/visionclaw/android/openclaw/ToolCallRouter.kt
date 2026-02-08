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
        if (call.name != "execute") {
            Log.w(TAG, "Unknown tool: ${call.name}")
            geminiLiveService.sendToolResponse(call.id, "Error: Unknown tool ${call.name}")
            return
        }

        val task = call.args["task"]
        if (task.isNullOrBlank()) {
            Log.w(TAG, "Missing task argument")
            geminiLiveService.sendToolResponse(call.id, "Error: Missing task argument")
            return
        }

        Log.d(TAG, "Executing task: $task")
        
        // Notify Gemini that we are working? No, protocol doesn't support "in progress" yet,
        // it just waits for response.

        val result = openClawBridge.executeTask(task)
        
        result.fold(
            onSuccess = { content ->
                Log.d(TAG, "Task execution successful: $content")
                geminiLiveService.sendToolResponse(call.id, content)
            },
            onFailure = { error ->
                Log.e(TAG, "Task execution failed: ${error.message}")
                geminiLiveService.sendToolResponse(call.id, "Error: ${error.message}")
            }
        )
    }
}
