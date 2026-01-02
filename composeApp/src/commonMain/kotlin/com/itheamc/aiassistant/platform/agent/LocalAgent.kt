package com.itheamc.aiassistant.platform.agent

import androidx.compose.ui.graphics.ImageBitmap
import com.itheamc.aiassistant.platform.PlatformLlmInferenceSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Local Agent with session management and automatic recovery.
 */
class LocalAgent(
    private val sessionFactory: () -> PlatformLlmInferenceSession,
    private val tools: List<Tool>,
    private val systemInstruction: String = "You are a helpful assistant.",
    private val maxTurns: Int = 5,
    private val responseTimeoutMs: Long = 30000L // 30 seconds timeout
) {
    private var session: PlatformLlmInferenceSession = sessionFactory()
    private val conversationHistory = mutableListOf<Message>()
    private var systemPromptSent = false
    private var tokenCount = 0
    private val maxTokensBeforeReset = 2000 // Adjust based on your model's limit

    data class Message(val role: String, val content: String, val tokenEstimate: Int = 0)

    /**
     * Main chat interface with automatic session recovery.
     */
    suspend fun chat(userMessage: String, image: ImageBitmap? = null): String {
        // Estimate tokens (rough: 1 token ≈ 4 chars)
        val userTokens = estimateTokens(userMessage)
        conversationHistory.add(Message("user", userMessage, userTokens))
        tokenCount += userTokens

        var turnCount = 0
        val usedTools = mutableSetOf<String>()
        var lastToolCall: ToolCall? = null
        var repeatCount = 0
        var sessionRecreateAttempts = 0
        val maxSessionRecreateAttempts = 2

        while (turnCount < maxTurns) {
            turnCount++

            // Check if we need to reset due to token limit
            if (tokenCount > maxTokensBeforeReset) {
                recreateSessionWithSummary()
            }

            val prompt = buildPrompt()
            val promptTokens = estimateTokens(prompt)

            // Generate response with retry on session failure
            val response = try {
                generateResponseWithRetry(
                    prompt = prompt,
                    image = if (conversationHistory.size == 1) image else null,
                    maxRetries = 2
                )
            } catch (e: SessionException) {
                // Session is dead, try to recreate
                if (sessionRecreateAttempts < maxSessionRecreateAttempts) {
                    sessionRecreateAttempts++
                    recreateSession()
                    continue // Retry with new session
                } else {
                    conversationHistory.clear()
                    return "I'm having trouble processing your request. Please try again."
                }
            } catch (e: Exception) {
                conversationHistory.clear()
                return "Error: ${e.message ?: "Unknown error occurred"}"
            }

            // Check for empty or invalid response
            if (response.isBlank()) {
                if (sessionRecreateAttempts < maxSessionRecreateAttempts) {
                    sessionRecreateAttempts++
                    recreateSession()
                    continue
                } else {
                    conversationHistory.clear()
                    return "I couldn't generate a response. Please try again."
                }
            }

            // Store response and update token count
            val responseTokens = estimateTokens(response)
            conversationHistory.add(Message("assistant", response, responseTokens))
            tokenCount += responseTokens

            // Parse tool call
            val toolCall = parseToolCall(response)

            if (toolCall != null) {
                // Loop detection
                if (toolCall == lastToolCall) {
                    repeatCount++
                    if (repeatCount >= 2) {
                        conversationHistory.clear()
                        tokenCount = 0
                        return "I encountered an issue with the ${toolCall.name} tool."
                    }
                } else {
                    repeatCount = 0
                }
                lastToolCall = toolCall

                // Check if tool already tried and failed
                if (toolCall.name in usedTools) {
                    conversationHistory.clear()
                    tokenCount = 0
                    return "The ${toolCall.name} tool didn't work as expected. Please try a different approach."
                }

                // Execute tool
                val result = executeTool(toolCall)
                usedTools.add(toolCall.name)

                // Handle tool failure
                if (result.startsWith("Error:")) {
                    conversationHistory.clear()
                    tokenCount = 0
                    return "The ${toolCall.name} tool encountered an error: ${result.removePrefix("Error: ")}"
                }

                // Add result
                val resultMsg = "Result: $result\n\nProvide final answer:"
                val resultTokens = estimateTokens(resultMsg)
                conversationHistory.add(Message("user", resultMsg, resultTokens))
                tokenCount += resultTokens

            } else {
                // Final response
                val cleanResponse = cleanResponse(response)
                conversationHistory.clear()
                tokenCount = 0
                return cleanResponse
            }
        }

        conversationHistory.clear()
        tokenCount = 0
        return "I wasn't able to complete that task."
    }

    /**
     * Generate response with automatic retry on failure.
     */
    private suspend fun generateResponseWithRetry(
        prompt: String,
        image: ImageBitmap?,
        maxRetries: Int = 2
    ): String {
        var attempts = 0
        var lastError: Exception? = null

        while (attempts < maxRetries) {
            attempts++

            try {
                val response = withTimeoutOrNull(responseTimeoutMs) {
                    generateResponseSuspend(prompt, image)
                }

                if (response != null && response.isNotBlank()) {
                    return response
                }

                // Empty response - might be a session issue
                if (attempts < maxRetries) {
                    recreateSession()
                }

            } catch (e: Exception) {
                lastError = e

                // Check if it's a token limit or session error
                val errorMsg = e.message?.lowercase() ?: ""
                if (errorMsg.contains("token") ||
                    errorMsg.contains("limit") ||
                    errorMsg.contains("context") ||
                    errorMsg.contains("session")) {

                    if (attempts < maxRetries) {
                        recreateSession()
                    } else {
                        throw SessionException("Session failed after token limit: ${e.message}", e)
                    }
                } else {
                    throw e
                }
            }
        }

        throw lastError ?: SessionException("Failed to generate response after $maxRetries attempts")
    }

    /**
     * Generate response with proper timeout and error handling.
     */
    private suspend fun generateResponseSuspend(text: String, image: ImageBitmap?): String {
        return suspendCancellableCoroutine { continuation ->
            val sb = StringBuilder()
            var hasResumed = false

            try {
                session.generateResponseAsync(
                    text = text,
                    image = image,
                    listener = { partialResult, done ->
                        sb.append(partialResult)
                        if (done && !hasResumed && continuation.isActive) {
                            hasResumed = true
                            continuation.resume(sb.toString())
                        }
                    },
                    onError = { error ->
                        if (!hasResumed && continuation.isActive) {
                            hasResumed = true
                            continuation.resumeWithException(
                                SessionException("Session error: $error")
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (!hasResumed && continuation.isActive) {
                    hasResumed = true
                    continuation.resumeWithException(e)
                }
            }

            continuation.invokeOnCancellation {
                // Cleanup if needed
            }
        }
    }

    /**
     * Recreate session when it fails or reaches token limit.
     */
    private fun recreateSession() {
        try {
            session.close()
        } catch (e: Exception) {
            // Ignore close errors
        }

        session = sessionFactory()
        systemPromptSent = false
        tokenCount = 0
        conversationHistory.clear()
    }

    /**
     * Recreate session with a summary of conversation history.
     */
    private suspend fun recreateSessionWithSummary() {
        // Save important context before clearing
        val contextSummary = if (conversationHistory.size > 2) {
            "Previous context: ${conversationHistory.takeLast(2).joinToString(" ") { it.content.take(100) }}"
        } else {
            ""
        }

        // Recreate session
        recreateSession()

        // Add summary if we had context
        if (contextSummary.isNotEmpty()) {
            val summaryTokens = estimateTokens(contextSummary)
            conversationHistory.add(Message("user", contextSummary, summaryTokens))
            tokenCount += summaryTokens
        }
    }

    /**
     * Estimate token count (rough approximation).
     */
    private fun estimateTokens(text: String): Int {
        // Rough estimate: 1 token ≈ 4 characters
        // This is a simplification - adjust based on your tokenizer
        return (text.length / 4).coerceAtLeast(1)
    }

    private fun buildPrompt(): String {
        val sb = StringBuilder()

        if (!systemPromptSent) {
            sb.append(buildSystemPrompt()).append("\n\n")
            systemPromptSent = true
        }

        // Keep only recent messages to manage context
        val recentMessages = conversationHistory.takeLast(3)
        for (msg in recentMessages) {
            sb.append("${msg.role.uppercase()}: ${msg.content}\n\n")
        }

        return sb.toString().trim()
    }

    private fun buildSystemPrompt(): String {
        if (tools.isEmpty()) {
            return systemInstruction
        }

        val toolList = tools.joinToString("\n") { tool ->
            "- ${tool.name}: ${tool.description}"
        }

        return """$systemInstruction

Available tools:
$toolList

To use a tool, respond ONLY with:
TOOL: tool_name
ARGS: {"arg1": "value1"}

Otherwise respond normally."""
    }

    private fun parseToolCall(response: String): ToolCall? {
        // Try simple format first
        val toolMatch = Regex("""TOOL:\s*(\w+)""", RegexOption.IGNORE_CASE).find(response)
        val argsMatch = Regex("""ARGS:\s*(\{[^}]*\})""", RegexOption.IGNORE_CASE).find(response)

        if (toolMatch != null) {
            val toolName = toolMatch.groupValues[1]
            val args = if (argsMatch != null) {
                try {
                    val parsed = SimpleJson.parse(argsMatch.groupValues[1])
                    if (parsed is Map<*, *>) {
                        parsed.mapKeys { it.key.toString() }
                    } else {
                        emptyMap()
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
            return ToolCall(toolName, args)
        }

        // Fallback to JSON format
        try {
            val jsonMatch = Regex("""\{[^}]*"tool"[^}]*\}""").find(response)
            if (jsonMatch != null) {
                val parsed = SimpleJson.parse(jsonMatch.value) as? Map<*, *>
                val toolName = parsed?.get("tool") as? String
                val params = parsed?.get("parameters") as? Map<*, *> ?: emptyMap<String, Any?>()

                if (toolName != null) {
                    return ToolCall(toolName, params.mapKeys { it.key.toString() })
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    private suspend fun executeTool(call: ToolCall): String {
        val tool = tools.find { it.name == call.name }
            ?: return "Error: Tool '${call.name}' not found"

        return try {
            tool.execute(call.args)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun cleanResponse(response: String): String {
        var cleaned = response

        cleaned = cleaned.replace(Regex("""TOOL:.*""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""ARGS:.*""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""\{[^}]*"tool"[^}]*\}"""), "")
        cleaned = cleaned.replace(Regex("""^(Final answer:|Answer:|Response:)\s*""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""^["']|["']$"""), "")

        return cleaned.trim().ifEmpty { "I don't have a response." }
    }

    /**
     * Manually close the session.
     */
    fun close() {
        try {
            session.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Clear history and reset.
     */
    fun reset() {
        conversationHistory.clear()
        systemPromptSent = false
        tokenCount = 0
    }

    /**
     * Get current token count estimate.
     */
    fun getTokenCount(): Int = tokenCount
}

/**
 * Exception for session-related errors.
 */
class SessionException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ConversationTurn(
    val role: String,
    val content: String
)