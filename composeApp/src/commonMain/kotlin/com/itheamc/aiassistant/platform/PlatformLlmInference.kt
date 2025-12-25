package com.itheamc.aiassistant.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformLlmInference {

    val sentencePieceProcessorHandle: Long

    fun sizeInTokens(text: String): Int

    fun generateResponse(text: String): String?

    fun generateResponseAsync(
        text: String,
        listener: (partialResult: String, done: Boolean) -> Unit,
        onError: (String) -> Unit
    )

    fun close()

    companion object {
        fun createFromOptions(options: PlatformLlmInferenceOptions): PlatformLlmInference
    }

    class PlatformLlmInferenceOptions {
        val modelPath: String?
        val maxTokens: Int?
        val maxTopK: Int?
        val supportedLoraRanks: List<Int>?
        val maxNumImages: Int?
        val backend: Backend

        class Builder private constructor() {
            fun setModelPath(modelPath: String): Builder
            fun setMaxTokens(maxTokens: Int): Builder
            fun setMaxTopK(maxTopK: Int): Builder
            fun setSupportedLoraRanks(supportedLoraRanks: List<Int>): Builder
            fun setMaxNumImages(maxNumImages: Int): Builder
            fun setPreferredBackend(backend: Backend): Builder
            fun build(): PlatformLlmInferenceOptions
        }

        companion object {
            fun builder(): Builder
        }
    }

    enum class Backend {
        DEFAULT, CPU, GPU
    }
}