package com.itheamc.aiassistant.ui.features.ai.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itheamc.aiassistant.core.storage.StorageKey.AI_MODEL_PATH
import com.itheamc.aiassistant.core.storage.StorageService
import com.itheamc.aiassistant.platform.FileDownloadState
import com.itheamc.aiassistant.platform.Platform
import com.itheamc.aiassistant.platform.PlatformFileDownloader
import com.itheamc.aiassistant.platform.PlatformLlmInference
import com.itheamc.aiassistant.platform.PlatformLlmInferenceSession
import com.itheamc.aiassistant.platform.agent.LocalAgent
import com.itheamc.aiassistant.platform.agent.SessionException
import com.itheamc.aiassistant.platform.agent.defineTool
import com.itheamc.aiassistant.ui.features.ai.models.ChatMessage
import com.itheamc.aiassistant.ui.features.ai.models.Participant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AgentShowcaseViewModel(
    private val storageService: StorageService,
    private val downloadManager: PlatformFileDownloader,
) : ViewModel() {

    // Model download configuration
    val modelDownloadUrl: String by lazy {
        if (Platform.isIOS())
            "https://drive.usercontent.google.com/download?id=1umIHyhgXtGB-KKrhbsHLUEv-YfZmOqNh&export=download&authuser=0&confirm=t&uuid=b7f0dc97-9f1a-4749-9264-d8af2bf627db&at=ANTm3czP6Za_RizNrAgEqLw3kQuD%3A1766750737754"
        else
            "https://drive.usercontent.google.com/download?id=1dp_kMbf2KbXf3FjLvOf7MCA8D0mkgCXp&export=download&authuser=0&confirm=t&uuid=0a45b921-1631-4d24-8335-55695bb590b8&at=ANTm3cy6l1MDsW-9okfLIdHFejrV%3A1766478439477"
    }

    val modelFileName: String by lazy {
        if (Platform.isIOS()) "model.bin" else "model.task"
    }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: PlatformLlmInference? = null
    private var localAgent: LocalAgent? = null
    private var currentResponseJob: Job? = null
    private var sessionRecreationCount = 0
    private val maxSessionRecreations = 5

    // Store model path for session recreation
    private var currentModelPath: String? = null

    init {
        checkIfAiModelIsDownloaded()
    }

    private fun checkIfAiModelIsDownloaded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = storageService.get(
                    stringPreferencesKey(AI_MODEL_PATH),
                    defaultValue = null
                )

                if (path == null || !path.endsWith(modelFileName)) {
                    _uiState.update { it.copy(isDownloadRequired = true) }
                } else {
                    currentModelPath = path
                    initializeAgent(path)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun startDownload() {
        _uiState.update { it.copy(isDownloadRequired = false, isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            downloadAndInitLlm()
        }
    }

    private suspend fun downloadAndInitLlm() {
        downloadManager.download(
            modelDownloadUrl,
            modelFileName
        ).collect { result ->
            when (result) {
                is FileDownloadState.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }

                is FileDownloadState.InProgress -> {
                    _uiState.update { it.copy(downloadProgress = result.progress) }
                }

                is FileDownloadState.Success -> {
                    storageService.set(
                        stringPreferencesKey(AI_MODEL_PATH),
                        result.path
                    )
                    currentModelPath = result.path
                    initializeAgent(result.path)
                }
            }
        }
    }

    /**
     * Initialize the agent with session factory pattern.
     */
    private fun initializeAgent(path: String) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            // Create LLM inference instance (reusable)
            if (llmInference == null) {
                val options = PlatformLlmInference.PlatformLlmInferenceOptions
                    .builder()
                    .setModelPath(path)
                    .setMaxTopK(64)
                    .setMaxTokens(2048) // Reduced for better memory management
                    .setMaxNumImages(10)
                    .build()
                llmInference = PlatformLlmInference.createFromOptions(options)
            }

            // Define tools
            val tools = listOf(
                createCalculatorTool(),
                createTimeTool(),
                createWeatherTool()
            )

            // Create agent with session factory
            localAgent = LocalAgent(
                sessionFactory = { createNewSession() },
                tools = tools,
                systemInstruction = "You are a helpful assistant with access to calculation, time, and weather tools. Use tools when appropriate to answer questions accurately.",
                responseTimeoutMs = 30000L
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isModelReady = true,
                    error = null
                )
            }

            sessionRecreationCount = 0

        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = "Failed to initialize agent: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Session factory function - creates new sessions as needed.
     */
    private fun createNewSession(): PlatformLlmInferenceSession {
        val inference = llmInference
            ?: throw IllegalStateException("LLM Inference not initialized")

        sessionRecreationCount++

        // Log session recreation for monitoring
        if (sessionRecreationCount > 1) {
            println("Session recreated: $sessionRecreationCount times")
        }

        // Alert if too many recreations (possible deeper issue)
        if (sessionRecreationCount > maxSessionRecreations) {
            _uiState.update {
                it.copy(
                    error = "Warning: Session has been recreated $sessionRecreationCount times. Consider restarting the app."
                )
            }
        }

        val sessionOptions = PlatformLlmInferenceSession
            .PlatformLlmInferenceSessionOptions
            .builder()
            .setRandomSeed((10..70).random()) // Different seed each time
            .setTemperature(0.3f) // Lower for better tool following
            .setTopK(40)
            .setTopP(0.9f)
            .setGraphOptions(
                PlatformLlmInferenceSession.PlatformLlmInferenceGraphOptions
                    .builder()
                    .setEnableVisionModality(false)
                    .build()
            )
            .build()

        return PlatformLlmInferenceSession.createFromOptions(inference, sessionOptions)
    }

    /**
     * Calculator tool with improved expression evaluation.
     */
    private fun createCalculatorTool() = defineTool(
        name = "calculate",
        description = "Perform basic arithmetic calculations",
        parameterBlock = {
            property("expression", "string", "Mathematical expression")
            require("expression")
        },
        execute = { args ->
            val expr = args["expression"] as? String
            if (expr.isNullOrBlank()) {
                return@defineTool "No calculation needed"
            }
            try {
                val result = evaluateExpression(expr)
                "The result of $expr is $result"
            } catch (e: Exception) {
                "Sorry, I couldn't calculate that: ${e.message}"
            }
        }
    )

    /**
     * Time tool.
     */
    @OptIn(ExperimentalTime::class)
    private fun createTimeTool() = defineTool(
        name = "get_current_time",
        description = "Get the current date and time",
        parameterBlock = {
            // No parameters needed
        },
        execute = {
            val now = Clock.System.now()
            "Current time: $now"
        }
    )

    /**
     * Weather tool (mock implementation).
     */
    private fun createWeatherTool() = defineTool(
        name = "get_weather",
        description = "Get current weather information for a city",
        parameterBlock = {
            property("city", "string", "Name of the city")
            require("city")
        },
        execute = { args ->
            val city = args["city"] as? String ?: "Unknown"
            "Weather in $city: 72°F (22°C), Sunny with light clouds"
        }
    )

    /**
     * Improved expression evaluator.
     */
    private fun evaluateExpression(expr: String): Double {
        val clean = expr.replace(" ", "")

        // Handle order of operations properly
        return when {
            clean.isEmpty() -> throw IllegalArgumentException("Empty expression")

            // Multiplication and division first
            "*" in clean || "/" in clean -> {
                // Split by + or - first (lower precedence)
                val addSubParts = clean.split(Regex("(?=[+\\-])"))
                addSubParts.sumOf { part ->
                    val trimmed = part.trim()
                    if (trimmed.isEmpty()) 0.0
                    else evaluateMultDiv(trimmed)
                }
            }

            "+" in clean -> {
                clean.split("+").sumOf { it.trim().toDouble() }
            }

            "-" in clean && !clean.startsWith("-") -> {
                val parts = clean.split("-")
                var result = parts[0].toDouble()
                for (i in 1 until parts.size) {
                    result -= parts[i].toDouble()
                }
                result
            }

            else -> clean.toDouble()
        }
    }

    private fun evaluateMultDiv(expr: String): Double {
        val multDivParts = expr.split(Regex("([*/])"))
        val operators = Regex("[*/]").findAll(expr).map { it.value }.toList()

        var result = multDivParts[0].toDouble()
        for (i in operators.indices) {
            val nextValue = multDivParts[i + 1].toDouble()
            result = if (operators[i] == "*") {
                result * nextValue
            } else {
                result / nextValue
            }
        }
        return result
    }

    /**
     * Send message to agent.
     */
    @OptIn(ExperimentalTime::class)
    fun sendMessage(text: String, image: ImageBitmap? = null) {
        if (text.isBlank()) return

        // Cancel any ongoing response
        currentResponseJob?.cancel()

        // Create user message
        val userMessage = ChatMessage(
            id = generateId(text).toString(),
            text = text,
            image = image,
            participant = Participant.USER,
            timestamp = Clock.System.now().toString()
        )

        _uiState.update {
            it.copy(messages = it.messages + userMessage)
        }

        generateAgentResponse(text, image)
    }

    /**
     * Generate agent response with proper error handling.
     */
    @OptIn(ExperimentalTime::class)
    private fun generateAgentResponse(prompt: String, image: ImageBitmap? = null) {
        currentResponseJob = viewModelScope.launch(Dispatchers.IO) {
            val aiMessageId = (generateId(prompt) + 1).toString()

            // Add thinking message
            _uiState.update {
                it.copy(messages = it.messages + ChatMessage(
                    id = aiMessageId,
                    text = "🤔 Thinking...",
                    participant = Participant.AI,
                    isPending = true,
                    timestamp = Clock.System.now().toString()
                ))
            }

            try {
                val agent = localAgent
                if (agent == null) {
                    updateAiMessage(aiMessageId, "Error: Agent not initialized", false)
                    return@launch
                }

                // Monitor token usage
                val tokensBefore = agent.getTokenCount()

                // Call agent (handles session recreation internally)
                val response = agent.chat(prompt, image)

                val tokensAfter = agent.getTokenCount()
                println("Token usage: $tokensBefore -> $tokensAfter")

                // Update with final response
                updateAiMessage(aiMessageId, response, false)

            } catch (e: CancellationException) {
                // Message was cancelled by user
                updateAiMessage(aiMessageId, "Cancelled", false)
                throw e

            } catch (e: SessionException) {
                // Session error - agent should have handled it, but if it reaches here, it's critical
                updateAiMessage(
                    aiMessageId,
                    "I'm having trouble processing your request. The session encountered an error. Please try again.",
                    false
                )

                // Try to reinitialize agent if we have the model path
                currentModelPath?.let { path ->
                    try {
                        initializeAgent(path)
                    } catch (initError: Exception) {
                        _uiState.update { it.copy(error = "Failed to reinitialize: ${initError.message}") }
                    }
                }

            } catch (e: Exception) {
                updateAiMessage(
                    aiMessageId,
                    "Sorry, I encountered an error: ${e.message ?: "Unknown error"}",
                    false
                )
            }
        }
    }

    /**
     * Update AI message in the state.
     */
    private fun updateAiMessage(messageId: String, text: String, isPending: Boolean) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(text = text, isPending = isPending)
                } else {
                    msg
                }
            }
            state.copy(messages = updatedMessages)
        }
    }

    /**
     * Cancel current response generation.
     */
    fun cancel() {
        currentResponseJob?.cancel()
        currentResponseJob = null
    }

    /**
     * Clear chat history.
     */
    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
        localAgent?.reset()
        sessionRecreationCount = 0
    }

    /**
     * Reset agent (useful if it gets into a bad state).
     */
    fun resetAgent() {
        currentModelPath?.let { path ->
            localAgent?.close()
            localAgent = null
            sessionRecreationCount = 0
            initializeAgent(path)
        }
    }

    /**
     * Get agent statistics for debugging.
     */
    fun getAgentStats(): String {
        val agent = localAgent ?: return "Agent not initialized"
        return """
            Token count: ${agent.getTokenCount()}
            Session recreations: $sessionRecreationCount
            Messages: ${_uiState.value.messages.size}
        """.trimIndent()
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(text: String) =
        Clock.System.now().toEpochMilliseconds() + text.hashCode()

    override fun onCleared() {
        super.onCleared()
        currentResponseJob?.cancel()
        localAgent?.close()
        llmInference?.close()
    }
}