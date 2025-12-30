package com.itheamc.aiassistant.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.PromptTemplates

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformLlmInferenceSession private constructor(
    llmInference: PlatformLlmInference,
    options: PlatformLlmInferenceSessionOptions
) {
    private val llmInferenceSession: LlmInferenceSession by lazy {
        LlmInferenceSession.createFromOptions(
            llmInference.llmInference,
            options.toLlmInferenceSessionOptions()
        )
    }

    actual fun sizeInTokens(text: String): Int {
        return try {
            llmInferenceSession.sizeInTokens(text)
        } catch (_: Exception) {
            0
        }
    }

    actual fun generateResponse(text: String, image: ImageBitmap?): String? {
        return try {
            llmInferenceSession.addQueryChunk(text)

            runCatching {
                image?.let {
                    llmInferenceSession.addImage(
                        BitmapImageBuilder(it.asAndroidBitmap()).build()
                    )
                }
            }

            llmInferenceSession.generateResponse()
        } catch (_: Exception) {
            ""
        }
    }

    actual fun generateResponseAsync(
        text: String,
        image: ImageBitmap?,
        listener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            llmInferenceSession.addQueryChunk(text)

            runCatching {
                image?.let {
                    llmInferenceSession.addImage(
                        BitmapImageBuilder(it.asAndroidBitmap()).build()
                    )
                }
            }

            llmInferenceSession.generateResponseAsync { partialResult, done ->
                listener(partialResult, done)
            }
        } catch (e: Exception) {
            onError(e.message ?: "Something went wrong")
        }
    }

    actual fun cancelGenerateResponseAsync() {
        llmInferenceSession.cancelGenerateResponseAsync()
    }

    actual fun close() {
        llmInferenceSession.close()
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


private fun PlatformLlmInferenceSession.PlatformLlmInferenceSessionOptions.toLlmInferenceSessionOptions(): LlmInferenceSession.LlmInferenceSessionOptions {
    val builder = LlmInferenceSession.LlmInferenceSessionOptions.builder()

    topK?.let { builder.setTopK(it) }
    topP?.let { builder.setTopP(it) }
    temperature?.let { builder.setTemperature(it) }
    randomSeed?.let { builder.setRandomSeed(it) }
    constraintHandle?.let { builder.setConstraintHandle(it) }
    promptTemplates?.let { builder.setPromptTemplates(it.toPromptTemplates()) }
    loraPath?.let { builder.setLoraPath(it) }
    graphOptions?.let { builder.setGraphOptions(it.toGraphOptions()) }

    return builder.build()
}


private fun PlatformLlmInferenceSession.PlatformLlmInferenceGraphOptions.toGraphOptions(): GraphOptions {
    val builder = GraphOptions.builder()

    enableAudioModality?.let { builder.setEnableAudioModality(it) }
    enableVisionModality?.let { builder.setEnableVisionModality(it) }
    includeTokenCostCalculator?.let { builder.setIncludeTokenCostCalculator(it) }

    return builder.build()
}

private fun PlatformLlmInferenceSession.PlatformLlmInferencePromptTemplates.toPromptTemplates(): PromptTemplates {
    val builder = PromptTemplates.builder()

    userPrefix?.let { builder.setUserPrefix(it) }
    userSuffix?.let { builder.setUserSuffix(it) }
    modelPrefix?.let { builder.setModelPrefix(it) }
    modelSuffix?.let { builder.setModelSuffix(it) }
    systemPrefix?.let { builder.setSystemPrefix(it) }
    systemSuffix?.let { builder.setSystemSuffix(it) }

    return builder.build()
}