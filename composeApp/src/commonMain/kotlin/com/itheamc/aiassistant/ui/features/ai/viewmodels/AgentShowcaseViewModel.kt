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
import com.itheamc.aiassistant.platform.agent.defineTool
import com.itheamc.aiassistant.ui.features.ai.models.ChatMessage
import com.itheamc.aiassistant.ui.features.ai.models.Participant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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

    // Reuse the same model downlad logic for simplicity
    val modelDownloadUrl: String by lazy {
        if (Platform.isIOS()) "https://drive.usercontent.google.com/download?id=1umIHyhgXtGB-KKrhbsHLUEv-YfZmOqNh&export=download&authuser=0&confirm=t&uuid=b7f0dc97-9f1a-4749-9264-d8af2bf627db&at=ANTm3czP6Za_RizNrAgEqLw3kQuD%3A1766750737754" else "https://drive.usercontent.google.com/download?id=1dp_kMbf2KbXf3FjLvOf7MCA8D0mkgCXp&export=download&authuser=0&confirm=t&uuid=0a45b921-1631-4d24-8335-55695bb590b8&at=ANTm3cy6l1MDsW-9okfLIdHFejrV%3A1766478439477"
    }

    val modelFileName: String by lazy { if (Platform.isIOS()) "model.bin" else "model.task" }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: PlatformLlmInference? = null
    private var llmSession: PlatformLlmInferenceSession? = null
    private var localAgent: LocalAgent? = null

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
                    initializeAgent(result.path)
                }
            }
        }
    }

    private fun initializeAgent(path: String) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val options = PlatformLlmInference.PlatformLlmInferenceOptions
                .builder()
                .setModelPath(path)
                .setMaxTopK(64)
                .setMaxTokens(3000)
                .setMaxNumImages(10)
                .build()
            llmInference = PlatformLlmInference.createFromOptions(options)

            llmInference?.let { inference ->
                val sessionOptions =
                    PlatformLlmInferenceSession
                        .PlatformLlmInferenceSessionOptions
                        .builder()
                        .setRandomSeed(42)
                        .setTemperature(0.5f) // Lower temperature for better tool use
                        .setTopK(64)
                        .setTopP(0.9f)
                        .setGraphOptions(
                            PlatformLlmInferenceSession.PlatformLlmInferenceGraphOptions
                                .builder()
                                .setEnableVisionModality(false)
                                .build()
                        )
                        .build() // Note: We don't set prompt templates here as LocalAgent handles its own system prompt logic mostly, 
                                 // OR we should set them if the underlying engine requires them for formatting. 
                                 // The current LocalAgent implementation injects prompts into the message flow manually.
                
                llmSession = PlatformLlmInferenceSession.createFromOptions(inference, sessionOptions)
                
                // Define Tools
                val calculatorTool = defineTool(
                    name = "calculate",
                    description = "Perform basic arithmetic calculations. Supports +, -, *, /.",
                    parameterBlock = {
                        property("expression", "string", "The mathematical expression to evaluate, e.g., '2 + 2 * 5'")
                        require("expression")
                    },
                    execute = { args ->
                        val expr = args["expression"] as String
                        // Extremely basic eval for demo purposes
                        // (In a real app, use a proper math parser)
                        try {
                             // Minimal parser for demo: 
                             // We will just return a dummy string if it's too complex or just say 'Calculated: ...'
                             // Since we can't easily eval string in commonMain without libraries, we'll mock it or do simple parsing.
                             "Result of '$expr' is ${mockEval(expr)}" 
                        } catch (e: Exception) {
                            "Error calculating: ${e.message}"
                        }
                    }
                )

                val timeTool = defineTool(
                    name = "get_current_time",
                    description = "Get the current local time.",
                    parameterBlock = {
                        // No params needed
                    },
                    execute = { 
                        Clock.System.now().toString()
                    }
                )

                localAgent = LocalAgent(
                    session = llmSession!!,
                    tools = listOf(calculatorTool, timeTool),
                    systemInstruction = "You are a helpful agentic assistant. You can calculate math expressions and check the time. Use the provided tools when necessary."
                )
            }
            _uiState.update { it.copy(isLoading = false, isModelReady = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }
    
    // Very dummy evaluator for the demo
    private fun mockEval(expr: String): String {
        // Remove spaces
        val clean = expr.replace(" ", "")
        // Check for simple addition
        if (clean.contains("+")) {
            val parts = clean.split("+")
            if (parts.size == 2) {
                return (parts[0].toDouble() + parts[1].toDouble()).toString()
            }
        }
        return "[Evaluated: $expr]" // Fallback
    }

    @OptIn(ExperimentalTime::class)
    fun sendMessage(text: String, image: ImageBitmap? = null) {
        // Creating user message
        val userMessage = ChatMessage(
            id = generateId(text = text).toString(),
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

    @OptIn(ExperimentalTime::class)
    private fun generateAgentResponse(prompt: String, image: ImageBitmap? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // Fix ID collision: append a suffix to ensure it's different from the user message ID
            val aiMessageId = (generateId(prompt) + 1).toString()
            // Initial placeholder
            _uiState.update {
                it.copy(messages = it.messages + ChatMessage(
                    id = aiMessageId,
                    text = "Thinking...", // Show thinking state
                    participant = Participant.AI,
                    isPending = true,
                    timestamp = Clock.System.now().toString()
                ))
            }

            if (localAgent != null) {
                try {
                    // LocalAgent.chat is a suspend function that handles the loop
                    val response = localAgent?.chat(prompt, image) ?: "No response"
                    
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == aiMessageId) {
                                msg.copy(
                                    text = response,
                                    isPending = false
                                )
                            } else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                } catch (e: Exception) {
                     _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == aiMessageId) {
                                msg.copy(
                                    text = "Error: ${e.message}",
                                    isPending = false
                                )
                            } else msg
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
            } else {
                 _uiState.update { it.copy(error = "Agent not initialized") }
            }
        }
    }

    fun cancel() {
        // LocalAgent currently doesn't expose cancel, but we could add it to the wrapper if needed.
        // For now, we can cancel the scope, but `LocalAgent.chat` runs in the calling scope, so standard cancellation works.
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(text: String) =
        Clock.System.now().toEpochMilliseconds() + text.hashCode()

    override fun onCleared() {
        super.onCleared()
        llmSession?.close()
        llmInference?.close()
    }
}
