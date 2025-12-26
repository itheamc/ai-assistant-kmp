package com.itheamc.aiassistant.platform

import cocoapods.MediaPipeTasksGenAI.MPPLLMInference
import cocoapods.MediaPipeTasksGenAI.MPPLLMInferenceOptions
import kotlinx.cinterop.ExperimentalForeignApi

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformLlmInference private constructor(private val options: PlatformLlmInferenceOptions) {

    @OptIn(ExperimentalForeignApi::class)
    internal val llmInference: MPPLLMInference by lazy {
        MPPLLMInference(options = options.toMPPLLMInferenceOptions(), error = null)
    }

    actual val sentencePieceProcessorHandle: Long
        get() = 0L

    @OptIn(ExperimentalForeignApi::class)
    actual fun sizeInTokens(text: String): Int {
        return 0
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun generateResponse(text: String): String? {
        return try {
            llmInference.generateResponseWithInputText(text, error = null)
        } catch (_: Exception) {
            ""
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun generateResponseAsync(
        text: String,
        listener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            llmInference.generateResponseAsyncWithInputText(
                inputText = text,
                error = null,
                progress = { partialResult, error ->
                    if (error != null) {
                        onError(error.localizedDescription)
                        return@generateResponseAsyncWithInputText
                    }

                    if (partialResult != null) {
                        listener(partialResult, false)
                    }
                },
                completion = {
                    listener("", true)
                }
            )
        } catch (e: Throwable) {
            onError(e.message ?: "Something went wrong")
        }
    }


    actual fun close() {
        // No explicit close required for MPPLLMInference on iOS
    }

    actual companion object {
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

    actual enum class Backend { DEFAULT, CPU, GPU }
}

@OptIn(ExperimentalForeignApi::class)
private fun PlatformLlmInference.PlatformLlmInferenceOptions.toMPPLLMInferenceOptions(): MPPLLMInferenceOptions {
    val path = modelPath
        ?: error("modelPath is required for MPPLLMInferenceOptions")

    return MPPLLMInferenceOptions(modelPath = path).apply {
        maxTokens?.let { setMaxTokens(it.toLong()) }
        maxTopK?.let { setMaxTopk(it.toLong()) }
        supportedLoraRanks?.let { setSupportedLoraRanks(it) }
    }
}
