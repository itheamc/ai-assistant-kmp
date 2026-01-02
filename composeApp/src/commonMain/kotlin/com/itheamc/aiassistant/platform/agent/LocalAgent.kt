package com.itheamc.aiassistant.platform.agent

import androidx.compose.ui.graphics.ImageBitmap
import com.itheamc.aiassistant.platform.PlatformLlmInferenceSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hybrid Local Agent that combines intent detection with LLM tool selection.
 * Uses a two-phase approach:
 * 1. Model analyzes query and selects tool (structured prompt, not JSON)
 * 2. Code extracts parameters and executes tool
 */
class LocalAgent(
    private val sessionFactory: () -> PlatformLlmInferenceSession,
    private val tools: List<Tool>,
    private val systemInstruction: String = "You are a helpful assistant.",
    private val responseTimeoutMs: Long = 30000L
) {
    private var session: PlatformLlmInferenceSession = sessionFactory()
    private var conversationHistory = mutableListOf<String>()
    private var tokenCount = 0
    private val maxTokensBeforeReset = 2000

    /**
     * Main chat function with dynamic tool selection.
     */
    suspend fun chat(userMessage: String, image: ImageBitmap? = null): String {
        tokenCount += estimateTokens(userMessage)

        if (tokenCount > maxTokensBeforeReset) {
            recreateSession()
        }

        // Phase 1: Ask model to analyze the query and select a tool
        val toolSelection = selectTool(userMessage, image) ?:
        // No tool needed - just chat
        return generateNormalResponse(userMessage, image)

        // Phase 2: Check if we have all required parameters
        val tool = tools.find { it.name == toolSelection.toolName }
        if (tool == null) {
            return "I don't have access to the ${toolSelection.toolName} tool."
        }

        val missingParams = findMissingParameters(tool, toolSelection.extractedParams)

        if (missingParams.isNotEmpty()) {
            // Ask user for missing parameters
            conversationHistory.add("User: $userMessage")
            val question = buildParameterQuestion(tool, missingParams)
            conversationHistory.add("Assistant: $question")
            return question
        }

        // Phase 3: Execute the tool
        val result = executeTool(tool, toolSelection.extractedParams)

        // Phase 4: Format the result naturally
        return formatToolResult(userMessage, tool.name, result, image)
    }

    /**
     * Phase 1: Ask model to select appropriate tool.
     * Uses structured text format instead of JSON (more reliable for small models).
     */
    private suspend fun selectTool(userMessage: String, image: ImageBitmap?): ToolSelection? {
        val toolsList = tools.joinToString("\n") { tool ->
            val params =
                (tool.parameters["properties"] as? Map<*, *>)?.keys?.joinToString(", ") ?: ""
            "- ${tool.name}: ${tool.description} [Parameters: $params]"
        }

        val prompt = """
Analyze this user request and determine if a tool should be used.

Available tools:
$toolsList

User request: "$userMessage"

Instructions:
1. If NO tool is needed, respond with: CHAT
2. If a tool is needed, respond with:
   TOOL: tool_name
   REASON: brief reason
   
Only respond with CHAT or TOOL format above. Be direct.
        """.trim()

        return try {
            val response = generateResponseWithRetry(prompt, image, maxRetries = 1)
            parseToolSelection(response, userMessage)
        } catch (e: Exception) {
            null // Fall back to chat
        }
    }

    /**
     * Parse the model's tool selection response.
     */
    private fun parseToolSelection(response: String, originalMessage: String): ToolSelection? {
        val lines = response.trim().lines()

        // Check if model said to chat
        if (response.uppercase().contains("CHAT")) {
            return null
        }

        // Look for TOOL: pattern
        val toolLine = lines.find { it.trim().uppercase().startsWith("TOOL:") }
        if (toolLine != null) {
            val toolName = toolLine.substringAfter(":", "").trim().lowercase()
            val matchedTool = tools.find { it.name.lowercase() == toolName }

            if (matchedTool != null) {
                // Extract parameters from original message
                val params = extractParametersFromMessage(matchedTool, originalMessage)
                return ToolSelection(matchedTool.name, params)
            }
        }

        return null
    }

    /**
     * Extract parameters from user message using pattern matching.
     * This is dynamic based on the tool's parameter schema.
     */
    private fun extractParametersFromMessage(tool: Tool, message: String): Map<String, Any?> {
        val properties = (tool.parameters["properties"] as? Map<*, *>) ?: return emptyMap()
        val extracted = mutableMapOf<String, Any?>()

        properties.forEach { (paramName, paramSchema) ->
            val schema = paramSchema as? Map<*, *> ?: return@forEach
            val paramType = schema["type"] as? String ?: "string"
            val paramNameStr = paramName.toString()

            val value = when (paramType) {
                "number", "integer" -> extractNumber(message)
                "string" -> extractStringParameter(paramNameStr, message, schema)
                "boolean" -> extractBoolean(message)
                else -> null
            }

            if (value != null) {
                extracted[paramNameStr] = value
            }
        }

        return extracted
    }

    /**
     * Extract string parameters based on common patterns.
     */
    private fun extractStringParameter(
        paramName: String,
        message: String,
        schema: Map<*, *>
    ): String? {
        val description = schema["description"] as? String ?: ""
        val lower = message.lowercase()

        // Try to extract based on parameter context
        return when {
            // Math expression
            paramName.contains("expression") || description.contains("expression") -> {
                extractMathExpression(message)
            }
            // City name
            paramName.contains("city") || description.contains("city") -> {
                extractLocation(message)
            }
            // Generic text extraction - look for quoted text or text after keywords
            else -> {
                // Look for quoted text
                val quoted = Regex("""["']([^"']+)["']""").find(message)
                if (quoted != null) {
                    return quoted.groupValues[1]
                }

                // Look for text after common prepositions
                val afterPrep =
                    Regex("""(?:for|about|on|of|in)\s+(.+?)(?:\?|$)""", RegexOption.IGNORE_CASE)
                        .find(message)
                if (afterPrep != null) {
                    return afterPrep.groupValues[1].trim()
                }

                null
            }
        }
    }

    /**
     * Extract mathematical expressions.
     */
    private fun extractMathExpression(text: String): String? {
        // Direct math expression
        val mathPattern = Regex("""(\d+\.?\d*\s*[+\-*/]\s*\d+\.?\d*(?:\s*[+\-*/]\s*\d+\.?\d*)*)""")
        val directMatch = mathPattern.find(text)
        if (directMatch != null) {
            return directMatch.value.replace(" ", "")
        }

        // After keywords
        val keywordPattern = Regex(
            """(?:calculate|compute|solve|what is|what's)\s+(.+?)(?:\?|$)""",
            RegexOption.IGNORE_CASE
        )
        val match = keywordPattern.find(text)
        if (match != null) {
            val expr = match.groupValues[1].trim()
            if (expr.matches(Regex(""".*\d+.*[+\-*/].*\d+.*"""))) {
                return expr.replace(" ", "")
            }
        }

        return null
    }

    /**
     * Extract location/city names.
     */
    private fun extractLocation(text: String): String? {
        val patterns = listOf(
            Regex("""(?:in|of|for|at)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)"""),
            Regex(
                """([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)\s+(?:weather|temperature|forecast)""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val location = match.groupValues[1].trim()
                if (location.isNotEmpty() && !isCommonWord(location.lowercase())) {
                    return location
                }
            }
        }

        return null
    }

    /**
     * Extract numbers from text.
     */
    private fun extractNumber(text: String): Number? {
        val numberPattern = Regex("""\b(\d+\.?\d*)\b""")
        val match = numberPattern.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * Extract boolean values.
     */
    private fun extractBoolean(text: String): Boolean? {
        val lower = text.lowercase()
        return when {
            lower.contains("yes") || lower.contains("true") || lower.contains("enable") -> true
            lower.contains("no") || lower.contains("false") || lower.contains("disable") -> false
            else -> null
        }
    }

    /**
     * Check for missing required parameters.
     */
    private fun findMissingParameters(
        tool: Tool,
        extractedParams: Map<String, Any?>
    ): List<String> {
        val required =
            (tool.parameters["required"] as? List<*>)?.map { it.toString() } ?: emptyList()
        return required.filter { !extractedParams.containsKey(it) || extractedParams[it] == null }
    }

    /**
     * Build a natural question to ask for missing parameters.
     */
    private fun buildParameterQuestion(tool: Tool, missingParams: List<String>): String {
        val properties = (tool.parameters["properties"] as? Map<*, *>)

        return properties?.let {
            if (missingParams.size == 1) {
                val paramName = missingParams[0]
                val schema = properties[paramName] as? Map<*, *>
                val description = schema?.get("description") as? String
                description?.let { "What $it?" } ?: "What is the $paramName?"
            } else {
                val params = missingParams.joinToString(", ")
                "I need the following information: $params"
            }
        } ?: "No properties"
    }

    /**
     * Execute tool with extracted parameters.
     */
    private suspend fun executeTool(tool: Tool, params: Map<String, Any?>): String {
        return try {
            tool.execute(params)
        } catch (e: Exception) {
            "Error executing ${tool.name}: ${e.message}"
        }
    }

    /**
     * Format tool result naturally using the model.
     */
    private suspend fun formatToolResult(
        originalQuery: String,
        toolName: String,
        result: String,
        image: ImageBitmap?
    ): String {
        val prompt = """
$systemInstruction

User asked: "$originalQuery"

I used the $toolName tool and got this result:
$result

Provide a natural, conversational response based on this information. Be brief and helpful.
        """.trim()

        return try {
            val response = generateResponseWithRetry(prompt, image, maxRetries = 1)
            tokenCount += estimateTokens(response)
            response.trim()
        } catch (e: Exception) {
            // Fallback: return raw result
            result
        }
    }

    /**
     * Generate normal chat response (no tool).
     */
    private suspend fun generateNormalResponse(message: String, image: ImageBitmap?): String {
        val prompt = """
$systemInstruction

User: $message
        """.trim()

        return try {
            val response = generateResponseWithRetry(prompt, image)
            tokenCount += estimateTokens(response)
            response.trim()
        } catch (e: Exception) {
            "I'm having trouble responding. Please try again."
        }
    }

    private suspend fun generateResponseWithRetry(
        prompt: String,
        image: ImageBitmap?,
        maxRetries: Int = 2
    ): String {
        var attempts = 0

        while (attempts < maxRetries) {
            attempts++

            try {
                val response = withTimeoutOrNull(responseTimeoutMs) {
                    generateResponseSuspend(prompt, image)
                }

                if (!response.isNullOrBlank()) {
                    return response
                }

                if (attempts < maxRetries) {
                    recreateSession()
                }

            } catch (e: Exception) {
                val errorMsg = e.message?.lowercase() ?: ""
                if (errorMsg.contains("token") || errorMsg.contains("limit") ||
                    errorMsg.contains("context")
                ) {
                    if (attempts < maxRetries) {
                        recreateSession()
                    } else {
                        throw SessionException("Token limit reached", e)
                    }
                } else {
                    throw e
                }
            }
        }

        throw SessionException("Failed after $maxRetries attempts")
    }

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
                            continuation.resumeWithException(SessionException("Session error: $error"))
                        }
                    }
                )
            } catch (e: Exception) {
                if (!hasResumed && continuation.isActive) {
                    hasResumed = true
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun recreateSession() {
        try {
            session.close()
        } catch (_: Exception) {
        }

        session = sessionFactory()
        tokenCount = 0
        conversationHistory.clear()
    }

    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "the", "what", "how", "when", "where", "which", "who",
            "is", "are", "was", "were", "be", "been", "being",
            "it", "its", "this", "that", "these", "those"
        )
        return word in commonWords
    }

    fun getTokenCount(): Int = tokenCount
    fun reset() {
        tokenCount = 0
        conversationHistory.clear()
    }

    fun close() {
        try {
            session.close()
        } catch (_: Exception) {
        }
    }
}

/**
 * Represents a selected tool with extracted parameters.
 */
data class ToolSelection(
    val toolName: String,
    val extractedParams: Map<String, Any?>
)

class SessionException(message: String, cause: Throwable? = null) : Exception(message, cause)