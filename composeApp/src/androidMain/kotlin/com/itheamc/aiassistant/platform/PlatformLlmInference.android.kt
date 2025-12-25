package com.itheamc.aiassistant.platform

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformLlmInference private constructor(options: PlatformLlmInferenceOptions) {

    val llmInference: LlmInference by lazy {
        LlmInference.createFromOptions(
            appContext,
            options.toLlmInference()
        )
    }

    actual val sentencePieceProcessorHandle: Long by lazy { llmInference.sentencePieceProcessorHandle }

    actual fun sizeInTokens(text: String): Int {
        return try {
            llmInference.sizeInTokens(text)
        } catch (_: Exception) {
            0
        }
    }

    actual fun generateResponse(text: String): String? {
        return try {
            llmInference.generateResponse(text)
        } catch (_: Exception) {
            ""
        }
    }

    actual fun generateResponseAsync(
        text: String,
        listener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            llmInference.generateResponseAsync(
                text
            ) { partialResult, done ->
                listener(partialResult, done)
            }
        } catch (e: Exception) {
            onError(e.message ?: "Something went wrong")
        }
    }

    actual fun close() {
        llmInference.close()
    }

    actual companion object {

        @Volatile
        private var appContext: Context? = null

        internal fun setContext(context: Context) {
            appContext = context
        }

        actual fun createFromOptions(options: PlatformLlmInferenceOptions): PlatformLlmInference {
            return PlatformLlmInference(options)
        }
    }

    actual class PlatformLlmInferenceOptions internal constructor(
        actual val modelPath: String?,
        actual val maxTokens: Int?,
        actual val maxTopK: Int?,
        actual val supportedLoraRanks: List<Int>?,
        actual val maxNumImages: Int?,
        actual val backend: Backend
    ) {
        actual class Builder actual constructor() {
            private var modelPath: String? = null
            private var maxTokens: Int? = null
            private var maxTopK: Int? = null
            private var supportedLoraRanks: List<Int>? = null
            private var maxNumImages: Int? = null
            private var backend: Backend = Backend.DEFAULT

            actual fun setModelPath(modelPath: String) = apply {
                this.modelPath = modelPath.removePrefix("file://")
            }

            actual fun setMaxTokens(maxTokens: Int) = apply {
                this.maxTokens = maxTokens
            }

            actual fun setMaxTopK(maxTopK: Int) = apply {
                this.maxTopK = maxTopK
            }

            actual fun setSupportedLoraRanks(supportedLoraRanks: List<Int>) = apply {
                this.supportedLoraRanks = supportedLoraRanks
            }

            actual fun setMaxNumImages(maxNumImages: Int) = apply {
                this.maxNumImages = maxNumImages
            }

            actual fun setPreferredBackend(backend: Backend) = apply { this.backend = backend }

            actual fun build(): PlatformLlmInferenceOptions {
                return PlatformLlmInferenceOptions(
                    modelPath,
                    maxTokens,
                    maxTopK,
                    supportedLoraRanks,
                    maxNumImages,
                    backend
                )
            }
        }

        actual companion object {
            actual fun builder(): Builder = Builder()
        }
    }

    actual enum class Backend {
        DEFAULT, CPU, GPU
    }
}


private fun PlatformLlmInference.PlatformLlmInferenceOptions.toLlmInference(): LlmInference.LlmInferenceOptions {
    val builder = LlmInference.LlmInferenceOptions.builder()

    modelPath?.let { builder.setModelPath(it) }
    maxTokens?.let { builder.setMaxTokens(it) }
    maxTopK?.let { builder.setMaxTopK(it) }
    supportedLoraRanks?.let { builder.setSupportedLoraRanks(it) }
    maxNumImages?.let { builder.setMaxNumImages(it) }

    when (backend) {
        PlatformLlmInference.Backend.DEFAULT -> builder.setPreferredBackend(LlmInference.Backend.DEFAULT)
        PlatformLlmInference.Backend.CPU -> builder.setPreferredBackend(LlmInference.Backend.CPU)
        PlatformLlmInference.Backend.GPU -> builder.setPreferredBackend(LlmInference.Backend.GPU)
    }

    return builder.build()
}