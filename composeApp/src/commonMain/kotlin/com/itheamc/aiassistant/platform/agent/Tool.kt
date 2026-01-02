package com.itheamc.aiassistant.platform.agent

/**
 * Represents a tool (function) that the agent can execute.
 */
data class Tool(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>, // JSON Schema
    val execute: suspend (Map<String, Any?>) -> String
)

/**
 * Represents a detected function call.
 */
data class ToolCall(
    val name: String,
    val args: Map<String, Any?>
)

/**
 * Definition of function parameter schema builder helper.
 */
class ToolParameterBuilder {
    private val properties = mutableMapOf<String, Any>()
    private val required = mutableListOf<String>()

    fun property(name: String, type: String, description: String? = null, enumValues: List<String>? = null) {
        val prop = mutableMapOf<String, Any>("type" to type)
        if (description != null) prop["description"] = description
        if (enumValues != null) prop["enum"] = enumValues
        properties[name] = prop
    }

    fun require(name: String) {
        if (!required.contains(name)) required.add(name)
    }

    fun build(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to properties,
            "required" to required
        )
    }
}

fun defineTool(
    name: String,
    description: String,
    parameterBlock: ToolParameterBuilder.() -> Unit,
    execute: suspend (Map<String, Any?>) -> String
): Tool {
    val builder = ToolParameterBuilder()
    builder.parameterBlock()
    return Tool(name, description, builder.build(), execute)
}
