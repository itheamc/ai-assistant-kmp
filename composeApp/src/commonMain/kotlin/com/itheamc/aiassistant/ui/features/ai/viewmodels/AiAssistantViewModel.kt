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


class AiAssistantViewModel(
    private val storageService: StorageService,
    private val downloadManager: PlatformFileDownloader,
) : ViewModel() {

    val modelDownloadUrl: String by lazy {
        if (Platform.isIOS()) "https://drive.usercontent.google.com/download?id=1umIHyhgXtGB-KKrhbsHLUEv-YfZmOqNh&export=download&authuser=0&confirm=t&uuid=b7f0dc97-9f1a-4749-9264-d8af2bf627db&at=ANTm3czP6Za_RizNrAgEqLw3kQuD%3A1766750737754" else "https://drive.usercontent.google.com/download?id=1dp_kMbf2KbXf3FjLvOf7MCA8D0mkgCXp&export=download&authuser=0&confirm=t&uuid=0a45b921-1631-4d24-8335-55695bb590b8&at=ANTm3cy6l1MDsW-9okfLIdHFejrV%3A1766478439477"
    }

    val modelFileName: String by lazy { if (Platform.isIOS()) "model.bin" else "model.task" }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: PlatformLlmInference? = null
    private var llmSession: PlatformLlmInferenceSession? = null

    init {
        checkIfAiModelIsDownloaded()
    }


    /**
     * Checks if the AI model file path is stored in the local data store.
     *
     * If the path exists, it proceeds to initialize the Large Language Model (LLM).
     * If the path is missing, it updates the UI state to indicate that a model download is required.
     * Any errors encountered during the process are captured and reflected in the UI state.
     */
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
                    initializeLlmInference(path)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Initiates the download process for the AI model.
     *
     * This function updates the UI state to clear the download requirement flag and shows
     * a loading indicator. It then launches a coroutine on the IO dispatcher to execute
     * the model download and subsequent LLM initialization.
     */
    fun startDownload() {
        _uiState.update { it.copy(isDownloadRequired = false, isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            downloadAndInitLlm()
        }
    }

    /**
     * Downloads the AI model file from a remote URL and initializes the Large Language Model (LLM).
     *
     * This function uses the [downloadManager] to stream the model file. During the process:
     * - **InProgress**: Updates the UI state with the current download progress.
     * - **Error**: Updates the UI state with the error message and stops the loading indicator.
     * - **Success**: Saves the local file path to [storageService] and calls [initializeLlmInference] to
     *   initialize the inference engine.
     */
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
                    initializeLlmInference(result.path)
                }
            }
        }
    }

    /**
     * Initializes the Large Language Model (LLM) inference engine and session using the provided model path.
     *
     * This function configures the [PlatformLlmInference] with specific options (such as the model path and TopK
     * sampling), creates a new [PlatformLlmInferenceSession], and updates the UI state to reflect whether
     * the model is ready for use or if an error occurred during initialization.
     *
     * @param path The absolute file system path to the downloaded AI model file (.task).
     */
    private fun initializeLlmInference(path: String) {
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
                        .setTemperature(0.7f)
                        .setTopK(64)
                        .setTopP(0.9f)
                        .setPromptTemplates(
                            PlatformLlmInferenceSession
                                .PlatformLlmInferencePromptTemplates
                                .builder()
                                .setSystemPrefix(SYSTEM_TEMPLATE_PREFIX)
                                .setSystemSuffix(SYSTEM_TEMPLATE_SUFFIX)
                                .setUserPrefix(USER_TEMPLATE_PREFIX)
                                .setUserSuffix(USER_TEMPLATE_SUFFIX)
                                .setModelPrefix(MODEL_TEMPLATE_PREFIX)
                                .setModelSuffix(MODEL_TEMPLATE_SUFFIX)
                                .build()
                        )
                        .setGraphOptions(
                            PlatformLlmInferenceSession.PlatformLlmInferenceGraphOptions
                                .builder()
                                .setEnableVisionModality(false) // Make it true to enable image support
                                .build()
                        )
                        .build()
                llmSession =
                    PlatformLlmInferenceSession.createFromOptions(inference, sessionOptions)
            }
            _uiState.update { it.copy(isLoading = false, isModelReady = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    /**
     * Sends a user message, updates the UI state with the new message, and triggers the AI response generation.
     *
     * This function creates a [ChatMessage] for the user, appends it to the current message list in the [_uiState],
     * and then calls [generateAiResponse] to process the user's input through the LLM.
     *
     * @param text The message string entered by the user.
     * @param image Optional bitmap image attached to the message.
     */
    @OptIn(ExperimentalTime::class)
    fun sendMessage(text: String, image: ImageBitmap? = null) {

        // Formatted prompt with user chat history
        val formattedPrompt = buildPrompt(text)

        // Creating user message
        val userMessage = ChatMessage(
            id = generateId(text = text).toString(),
            text = text,
            image = image,
            participant = Participant.USER,
            timestamp = Clock.System.now().toString()
        )

        // Updating state with user message
        _uiState.update {
            it.copy(messages = it.messages + userMessage)
        }

        // Calling LLM to generate response
        generateAiResponse(formattedPrompt, image)
    }

    /**
     * Generates a streaming AI response for the given prompt and updates the UI state in real-time.
     *
     * This function performs the following steps:
     * 1. Creates a placeholder [ChatMessage] with [Participant.AI] and adds it to the message list.
     * 2. Initiates an asynchronous response generation using the active [llmSession].
     * 3. Listens for partial result tokens and appends them to the AI message, updating the `isPending`
     *    status once the response is complete.
     * 4. Handles potential errors by updating the UI state with an error message if the session is
     *    uninitialized or if the inference fails.
     *
     * @param prompt The user-provided text input to which the AI should respond.
     * @param image Optional image input for the model.
     */
    @OptIn(ExperimentalTime::class)
    private fun generateAiResponse(prompt: String, image: ImageBitmap? = null) {
        viewModelScope.launch(Dispatchers.IO) {

            // Creating Ai message which will be updated once generating started
            val aiMessageId = generateId(prompt).toString()
            val aiMessage = ChatMessage(
                id = aiMessageId,
                text = "",
                participant = Participant.AI,
                isPending = true,
                timestamp = Clock.System.now().toString()
            )

            // Updating state with ai message
            _uiState.update {
                it.copy(messages = it.messages + aiMessage)
            }

            // Generating response from LLM
            if (llmSession != null) {
                llmSession?.generateResponseAsync(
                    text = prompt,
                    image = image,
                    listener = { partialResult, done ->
                        _uiState.update { state ->
                            val updatedMessages = state.messages.map { msg ->
                                if (msg.id == aiMessageId && msg.participant == Participant.AI) {
                                    msg.copy(
                                        text = msg.text + partialResult,
                                        isPending = !done
                                    )
                                } else msg
                            }
                            state.copy(messages = updatedMessages)
                        }
                    },
                    onError = { error ->
                        _uiState.update { state ->
                            val updatedMessages = if (error.trim()
                                    .isEmpty()
                            ) state.messages.filter { msg -> msg.id != aiMessageId } else state.messages.map { msg ->
                                if (msg.id == aiMessageId && msg.participant == Participant.AI) {
                                    msg.copy(
                                        text = error,
                                        isPending = false
                                    )
                                } else msg
                            }
                            state.copy(messages = updatedMessages)
                        }
                        checkIfAiModelIsDownloaded()
                    }
                )
            } else {
                _uiState.update { it.copy(error = "AI Session not initialized") }
            }
        }
    }

    /**
     * Cancels the ongoing AI response generation process.
     *
     * This function launches a coroutine on the IO dispatcher to trigger the cancellation
     * of the current asynchronous inference task within the active [llmSession].
     */
    fun cancel() {
        viewModelScope.launch(Dispatchers.IO) {
            llmSession?.cancelGenerateResponseAsync()
        }
    }

    /**
     * Generates a unique identifier for a chat message by combining the current system
     * time in milliseconds with the hash code of the provided text.
     *
     * @param text The content of the message used to contribute to the unique ID.
     * @return A [Long] representing the generated unique identifier.
     */
    @OptIn(ExperimentalTime::class)
    private fun generateId(text: String) =
        Clock.System.now().toEpochMilliseconds() + text.hashCode()

    /**
     * Cleans up resources when the ViewModel is about to be destroyed.
     *
     * This ensures that the [llmSession] and [llmInference] engine are properly closed
     * to release native memory and hardware resources associated with the Large Language Model.
     */
    override fun onCleared() {
        super.onCleared()
        llmSession?.close()
        llmInference?.close()
    }

    /**
     * Constructs a formatted prompt string for the LLM by combining the system instructions,
     * a limited history of the conversation, and the current user input.
     *
     * This method follows a specific template structure (using start/end turn tags) to
     * maintain context and ensure the model adheres to its persona and task requirements.
     *
     * @param message The current text input from the user to be appended to the prompt.
     * @return A complete, formatted string ready to be processed by the inference engine.
     */
    private fun buildPrompt(message: String): String {
        val sb = StringBuilder()

        // System (only once, at top)
        // Since we are adding ai message along with user message before processing
        // Even in first call of this the size is 2 (one user and one ai)
        if (uiState.value.messages.isEmpty()) {
            sb.append(SYSTEM_TEMPLATE_PREFIX)
            sb.append(SYSTEM_PROMPT.trimIndent())
            sb.append(SYSTEM_TEMPLATE_SUFFIX)
        }

        // Chat History
        if (uiState.value.messages.isNotEmpty()) {
            sb.append(SYSTEM_TEMPLATE_PREFIX)
            sb.append("As a user assistant you consider these past chat histories before reply or process user query: \n")

            uiState.value.messages.takeLast(MAX_HISTORY_MESSAGES).forEach { msg ->
                when (msg.participant) {
                    Participant.USER -> {
                        sb.append("User: ${msg.text}\n")
                    }

                    Participant.AI -> {
                        sb.append("You (AI): ${msg.text}")
                    }
                }
            }

            sb.append(SYSTEM_TEMPLATE_SUFFIX)
        }

        // User Latest Message/Prompt
        sb.append(USER_TEMPLATE_PREFIX)
        sb.append(message)
        sb.append(USER_TEMPLATE_SUFFIX)

        return sb.toString()
    }

    companion object Companion {

        private const val SYSTEM_TEMPLATE_PREFIX = "<start_of_turn>system\n"
        private const val SYSTEM_TEMPLATE_SUFFIX = "\n<end_of_turn>\n"

        private const val USER_TEMPLATE_PREFIX = "<start_of_turn>user\n"
        private const val USER_TEMPLATE_SUFFIX = "\n<end_of_turn>\n"

        private const val MODEL_TEMPLATE_PREFIX = "<start_of_turn>model\n"
        private const val MODEL_TEMPLATE_SUFFIX = "\n<end_of_turn>\n"

        private const val MAX_HISTORY_MESSAGES = 4

        private const val SYSTEM_PROMPT =
            "You are an assistant that helps users to solve their query."
    }

}

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val isModelReady: Boolean = false,
    val isDownloadRequired: Boolean = false,
    val downloadProgress: Float = 0f
)
