package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformLlmInferenceSession {

    fun sizeInTokens(text: String): Int

    fun generateResponse(text: String): String?

    fun generateResponseAsync(
        text: String,
        listener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit
    )

    fun cancelGenerateResponseAsync()

    fun close()

    companion object Companion {
        fun createFromOptions(
            llmInference: PlatformLlmInference,
            options: PlatformLlmInferenceSessionOptions
        ): PlatformLlmInferenceSession
    }

    class PlatformLlmInferenceSessionOptions {
        val topK: Int?
        val topP: Float?
        val temperature: Float?
        val randomSeed: Int?
        val constraintHandle: Long?
        val promptTemplates: PlatformLlmInferencePromptTemplates?
        val loraPath: String?
        val graphOptions: PlatformLlmInferenceGraphOptions?

        class Builder private constructor() {
            fun setTopK(topK: Int): Builder
            fun setTopP(topP: Float): Builder
            fun setTemperature(temperature: Float): Builder
            fun setRandomSeed(randomSeed: Int): Builder
            fun setConstraintHandle(constraintHandle: Long): Builder
            fun setPromptTemplates(promptTemplates: PlatformLlmInferencePromptTemplates): Builder
            fun setLoraPath(loraPath: String): Builder
            fun setGraphOptions(graphOptions: PlatformLlmInferenceGraphOptions?): Builder
            fun build(): PlatformLlmInferenceSessionOptions
        }

        companion object {
            fun builder(): Builder
        }
    }

    class PlatformLlmInferencePromptTemplates {
        val userPrefix: String?
        val userSuffix: String?
        val modelPrefix: String?
        val modelSuffix: String?
        val systemPrefix: String?
        val systemSuffix: String?

        class Builder private constructor() {
            fun setUserPrefix(userPrefix: String): Builder
            fun setUserSuffix(userSuffix: String): Builder
            fun setModelPrefix(modelPrefix: String): Builder
            fun setModelSuffix(modelSuffix: String): Builder
            fun setSystemPrefix(systemPrefix: String): Builder
            fun setSystemSuffix(systemSuffix: String): Builder
            fun build(): PlatformLlmInferencePromptTemplates
        }

        companion object {
            fun builder(): Builder
        }
    }

    class PlatformLlmInferenceGraphOptions {
        val enableAudioModality: Boolean?
        val enableVisionModality: Boolean?
        val includeTokenCostCalculator: Boolean?

        class Builder private constructor() {
            fun setEnableAudioModality(enableAudioModality: Boolean): Builder
            fun setEnableVisionModality(enableVisionModality: Boolean): Builder
            fun setIncludeTokenCostCalculator(includeTokenCostCalculator: Boolean): Builder
            fun build(): PlatformLlmInferenceGraphOptions
        }

        companion object {
            fun builder(): Builder
        }
    }
}