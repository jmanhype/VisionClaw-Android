package com.visionclaw.android.openclaw

import com.visionclaw.android.gemini.*

/**
 * Tool declaration for the "execute" function sent to Gemini during setup.
 *
 * This is the single tool that routes all actionable requests to OpenClaw.
 */
object ToolDeclarations {
    val executeTool = Tool(
        functionDeclarations = listOf(
            FunctionDeclaration(
                name = "execute",
                description = "Execute a task using the connected personal assistant (OpenClaw). " +
                    "Use this for ANY action: sending messages, web search, managing lists, " +
                    "setting reminders, creating notes, controlling smart home devices, etc.",
                parameters = FunctionParameters(
                    type = "object",
                    properties = mapOf(
                        "task" to PropertySchema(
                            type = "string",
                            description = "Detailed description of the task to execute. " +
                                "Include all relevant context: names, content, platforms, quantities."
                        )
                    ),
                    required = listOf("task")
                )
            )
        )
    )
}
