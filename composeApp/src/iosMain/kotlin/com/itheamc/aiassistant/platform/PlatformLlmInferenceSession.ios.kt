package com.itheamc.aiassistant.platform

import cocoapods.MediaPipeTasksGenAI.MPPLLMInferenceSession
import cocoapods.MediaPipeTasksGenAI.MPPLLMInferenceSessionOptions
import kotlinx.cinterop.ExperimentalForeignApi

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformLlmInferenceSession private constructor(
    llmInference: PlatformLlmInference,
    options: PlatformLlmInferenceSessionOptions
) {
    @OptIn(ExperimentalForeignApi::class)
    private val llmInferenceSession: MPPLLMInferenceSession by lazy {
        MPPLLMInferenceSession(
            llmInference = llmInference.llmInference,
            options = options.toMPPLLMInferenceSessionOptions(),
            error = null
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun sizeInTokens(text: String): Int {
        return 0
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun generateResponse(text: String): String? {
        return try {
            llmInferenceSession.addQueryChunkWithInputText(text, error = null)
            llmInferenceSession.generateResponseAndReturnError(error = null)
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
            llmInferenceSession.addQueryChunkWithInputText(text, error = null)

            llmInferenceSession.generateResponseAsyncAndReturnError(
                error = null,
                progress = { partialResult, error ->
                    if (error != null) {
                        onError(error.localizedDescription)
                        return@generateResponseAsyncAndReturnError
                    }
                    if (partialResult != null) {
                        listener(partialResult, false)
                    }
                },
                completion = {
                    listener("", true)
                }
            )
        } catch (e: Exception) {
            onError(e.message ?: "Something went wrong")
        }
    }

    actual fun cancelGenerateResponseAsync() {
        // Don't have this method in MPPLLMInferenceSession on iOS
    }

    actual fun close() {
        // No explicit close required for MPPLLMInferenceSession on iOS
    }

    actual companion object {
        actual fun createFromOptions(
            llmInference: PlatformLlmInference,
            options: PlatformLlmInferenceSessionOptions
        ): PlatformLlmInferenceSession {
            return PlatformLlmInferenceSession(llmInference, options)
        }
    }

    actual class PlatformLlmInferenceSessionOptions(
        actual val topK: Int?,
        actual val topP: Float?,
        actual val temperature: Float?,
        actual val randomSeed: Int?,
        actual val constraintHandle: Long?,
        actual val promptTemplates: PlatformLlmInferencePromptTemplates?,
        actual val loraPath: String?,
        actual val graphOptions: PlatformLlmInferenceGraphOptions?
    ) {
        actual class Builder actual constructor() {
            private var topK: Int? = null
            private var topP: Float? = null
            private var temperature: Float? = null
            private var randomSeed: Int? = null
            private var constraintHandle: Long? = null
            private var promptTemplates: PlatformLlmInferencePromptTemplates? = null
            private var loraPath: String? = null
            private var graphOptions: PlatformLlmInferenceGraphOptions? = null

            actual fun setTopK(topK: Int): Builder = apply {
                this.topK = topK
            }

            actual fun setTopP(topP: Float): Builder = apply {
                this.topP = topP
            }

            actual fun setTemperature(temperature: Float): Builder = apply {
                this.temperature = temperature
            }

            actual fun setRandomSeed(randomSeed: Int): Builder = apply {
                this.randomSeed = randomSeed
            }

            actual fun setConstraintHandle(constraintHandle: Long): Builder = apply {
                this.constraintHandle = constraintHandle
            }

            actual fun setPromptTemplates(promptTemplates: PlatformLlmInferencePromptTemplates): Builder =
                apply {
                    this.promptTemplates = promptTemplates
                }

            actual fun setLoraPath(loraPath: String): Builder = apply { this.loraPath = loraPath }

            actual fun setGraphOptions(graphOptions: PlatformLlmInferenceGraphOptions?): Builder =
                apply {
                    this.graphOptions = graphOptions
                }

            actual fun build(): PlatformLlmInferenceSessionOptions {
                return PlatformLlmInferenceSessionOptions(
                    topK,
                    topP,
                    temperature,
                    randomSeed,
                    constraintHandle,
                    promptTemplates,
                    loraPath,
                    graphOptions,
                )
            }
        }

        actual companion object {
            actual fun builder(): Builder = Builder()
        }
    }

    actual class PlatformLlmInferencePromptTemplates(
        actual val userPrefix: String?,
        actual val userSuffix: String?,
        actual val modelPrefix: String?,
        actual val modelSuffix: String?,
        actual val systemPrefix: String?,
        actual val systemSuffix: String?,
    ) {
        actual class Builder actual constructor() {
            private var userPrefix: String? = null
            private var userSuffix: String? = null
            private var modelPrefix: String? = null
            private var modelSuffix: String? = null
            private var systemPrefix: String? = null
            private var systemSuffix: String? = null

            actual fun setUserPrefix(userPrefix: String): Builder = apply {
                this.userPrefix = userPrefix
            }

            actual fun setUserSuffix(userSuffix: String): Builder = apply {
                this.userSuffix = userSuffix
            }

            actual fun setModelPrefix(modelPrefix: String): Builder = apply {
                this.modelPrefix = modelPrefix
            }

            actual fun setModelSuffix(modelSuffix: String): Builder = apply {
                this.modelSuffix = modelSuffix
            }

            actual fun setSystemPrefix(systemPrefix: String): Builder = apply {
                this.systemPrefix = systemPrefix
            }

            actual fun setSystemSuffix(systemSuffix: String): Builder = apply {
                this.systemSuffix = systemSuffix
            }

            actual fun build(): PlatformLlmInferencePromptTemplates {
                return PlatformLlmInferencePromptTemplates(
                    userPrefix,
                    userSuffix,
                    modelPrefix,
                    modelSuffix,
                    systemPrefix,
                    systemSuffix,
                )
            }
        }

        actual companion object {
            actual fun builder(): Builder = Builder()
        }
    }

    actual class PlatformLlmInferenceGraphOptions(
        actual val enableAudioModality: Boolean?,
        actual val enableVisionModality: Boolean?,
        actual val includeTokenCostCalculator: Boolean?,
    ) {
        actual class Builder internal actual constructor() {
            private var enableAudioModality: Boolean? = null
            private var enableVisionModality: Boolean? = null
            private var includeTokenCostCalculator: Boolean? = null

            actual fun setEnableAudioModality(enableAudioModality: Boolean): Builder = apply {
                this.enableAudioModality = enableAudioModality
            }

            actual fun setEnableVisionModality(enableVisionModality: Boolean): Builder = apply {
                this.enableVisionModality = enableVisionModality
            }

            actual fun setIncludeTokenCostCalculator(includeTokenCostCalculator: Boolean): Builder =
                apply {
                    this.includeTokenCostCalculator = includeTokenCostCalculator
                }

            actual fun build(): PlatformLlmInferenceGraphOptions {
                return PlatformLlmInferenceGraphOptions(
                    enableAudioModality,
                    enableVisionModality,
                    includeTokenCostCalculator,
                )
            }
        }

        actual companion object {
            actual fun builder(): Builder = Builder()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun PlatformLlmInferenceSession.PlatformLlmInferenceSessionOptions.toMPPLLMInferenceSessionOptions(): MPPLLMInferenceSessionOptions {
    val options = MPPLLMInferenceSessionOptions()
    topK?.let { options.setTopk(it.toLong()) }
    topP?.let { options.setTopp(it) }
    temperature?.let { options.setTemperature(it) }
    randomSeed?.let { options.setRandomSeed(it.toLong()) }
    return options
}