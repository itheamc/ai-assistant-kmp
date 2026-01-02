package com.itheamc.aiassistant.platform.agent

import androidx.compose.ui.graphics.ImageBitmap
import com.itheamc.aiassistant.platform.PlatformLlmInferenceSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Local Agent that manages the conversation loop with Tool interactions.
 * Wraps PlatformLlmInferenceSession to provide an agentic interface.
 */
class LocalAgent(
    private val session: PlatformLlmInferenceSession,
    private val tools: List<Tool>,
    private val systemInstruction: String = "You are a helpful assistant.",
    private val maxTurns: Int = 10
) {

    private var isInitialized = false

    /**
     * Entry point for sending a message.
     * Returns the final response from the model.
     */
    suspend fun chat(userMessage: String, image: ImageBitmap? = null): String {
        // Prepare the input for this turn
        var inputToSend = userMessage
        
        if (!isInitialized) {
            isInitialized = true
            // Prepend system prompt to the first user message. 
            // We adding it as context before the user query.
            inputToSend = "${buildSystemPrompt()}\n\n$userMessage"
        }

        // Start the reasoning loop
        var currentInput = inputToSend
        var currentImage = image
        var turnCount = 0

        // Keep track of the last tool call to prevent loops
        var lastToolCall: ToolCall? = null

        while (turnCount < maxTurns) {
            // Generate response (suspend)
            val response = generateResponseSuspend(currentInput, currentImage)
            
            // Clear image after first usage to avoid re-sending it in loop
            currentImage = null 

            val toolCalls = parseToolCalls(response)

            if (toolCalls.isNotEmpty()) {
                val call = toolCalls.first() // Handle one at a time for simplicity in this loop
                
                // improved loop detection
                if (call == lastToolCall) {
                     return "Error: The agent got stuck in a loop calling '${call.name}' with the same arguments."
                }
                lastToolCall = call

                // Execute tool
                val results = StringBuilder()
                val tool = tools.find { it.name == call.name }
                if (tool != null) {
                    try {
                        val result = tool.execute(call.args)
                        results.append("Function '${call.name}' result: $result\n")
                    } catch (e: Exception) {
                        results.append("Function '${call.name}' failed: ${e.message}\n")
                    }
                } else {
                    results.append("Error: Function '${call.name}' not found.\n")
                }
                
                // Feed result back to model with explicit instruction to stop if done
                currentInput = "\nObservation:\n$results\n\nUsing this observation, answer the user's question. If you have the answer, output it directly without JSON."

                turnCount++
            } else {
                // No tools called, return the response text
                return response
            }
        }

        return "Error: Max turns exceeded."
    }

    private suspend fun generateResponseSuspend(text: String, image: ImageBitmap?): String {
        return suspendCancellableCoroutine { continuation ->
            val sb = StringBuilder()
            session.generateResponseAsync(
                text = text,
                image = image,
                listener = { partialResult, done ->
                    sb.append(partialResult)
                    if (done) {
                        if (continuation.isActive) {
                            continuation.resume(sb.toString())
                        }
                    }
                },
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(RuntimeException(error))
                    }
                }
            )
        }
    }

    /**
     * Formats the system instructions with tool definitions.
     */
    private fun buildSystemPrompt(): String {
        val toolsJson = SimpleJson.stringify(tools.map {
            mapOf(
                "name" to it.name,
                "description" to it.description,
                "parameters" to it.parameters
            )
        })

        return """
$systemInstruction

You have access to the following tools:
$toolsJson

To use a tool, you MUST respond ONLY with a JSON object in this format:
{ "tool": "tool_name", "parameters": { "param1": "value" } }

If you do not need to use a tool, just respond with your natural language answer.
"""
    }

    /**
     * Parses the response to check for tool calls.
     */
    private fun parseToolCalls(response: String): List<ToolCall> {
        val calls = mutableListOf<ToolCall>()
        
        // Look for JSON object in the response.
        // Logic: Find first '{' and last '}'
        try {
            val start = response.indexOf("{")
            val end = response.lastIndexOf("}")
            
            if (start != -1 && end != -1 && end > start) {
                val potentialJson = response.substring(start, end + 1)
                val parsed = SimpleJson.parse(potentialJson)
                
                if (parsed is Map<*, *>) {
                    val toolName = parsed["tool"] as? String
                    val params = parsed["parameters"] as? Map<*, *>
                    
                    if (toolName != null) {
                        // Cast params safely
                        val safeParams = params?.mapKeys { it.key } ?: emptyMap()
                        calls.add(ToolCall(toolName, safeParams.mapKeys { it.key.toString() }))
                    }
                }
            }
        } catch (_: Exception) {
            // Parse failed, ignore
        }
        
        return calls
    }
}
