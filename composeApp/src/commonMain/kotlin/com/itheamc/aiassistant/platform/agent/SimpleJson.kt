package com.itheamc.aiassistant.platform.agent

/**
 * A minimalist JSON parser and stringifier for commonMain.
 * Handles JSON Objects, Arrays, Strings, Numbers, Booleans, and Null.
 */
object SimpleJson {

    fun parse(json: String): Any? {
        val protocol = StringReader(json)
        return parseValue(protocol)
    }

    fun stringify(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"${escape(value)}\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> {
                val items = value.entries.joinToString(",") { (k, v) ->
                    "\"${escape(k.toString())}\":${stringify(v)}"
                }
                "{$items}"
            }
            is List<*> -> {
                val items = value.joinToString(",") { stringify(it) }
                "[$items]"
            }
            is Array<*> -> {
                val items = value.joinToString(",") { stringify(it) }
                "[$items]"
            }
            else -> "\"$value\""
        }
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
//            .replace("\f", "\\f") 
    }

    private class StringReader(val str: String) {
        var pos = 0
        fun peek(): Char? = if (pos < str.length) str[pos] else null
        fun next(): Char? = if (pos < str.length) str[pos++] else null
        fun skipWhitespace() {
            while (pos < str.length && str[pos].isWhitespace()) {
                pos++
            }
        }
    }

    private fun parseValue(reader: StringReader): Any? {
        reader.skipWhitespace()
        val c = reader.peek() ?: return null
        return when (c) {
            '{' -> parseObject(reader)
            '[' -> parseArray(reader)
            '"' -> parseString(reader)
            't', 'f' -> parseBoolean(reader)
            'n' -> parseNull(reader)
            '-', in '0'..'9' -> parseNumber(reader)
            else -> throw IllegalArgumentException("Unexpected char '$c' at ${reader.pos}")
        }
    }

    private fun parseObject(reader: StringReader): Map<String, Any?> {
        reader.next() // skip '{'
        reader.skipWhitespace()
        val map = mutableMapOf<String, Any?>()
        if (reader.peek() == '}') {
            reader.next()
            return map
        }
        while (true) {
            reader.skipWhitespace()
            val key = parseString(reader)
            reader.skipWhitespace()
            if (reader.next() != ':') throw IllegalArgumentException("Expected ':' at ${reader.pos}")
            val value = parseValue(reader)
            map[key] = value
            reader.skipWhitespace()
            val next = reader.next()
            if (next == '}') break
            if (next != ',') throw IllegalArgumentException("Expected ',' or '}' at ${reader.pos}")
        }
        return map
    }

    private fun parseArray(reader: StringReader): List<Any?> {
        reader.next() // skip '['
        reader.skipWhitespace()
        val list = mutableListOf<Any?>()
        if (reader.peek() == ']') {
            reader.next()
            return list
        }
        while (true) {
            val value = parseValue(reader)
            list.add(value)
            reader.skipWhitespace()
            val next = reader.next()
            if (next == ']') break
            if (next != ',') throw IllegalArgumentException("Expected ',' or ']' at ${reader.pos}")
        }
        return list
    }

    private fun parseString(reader: StringReader): String {
        if (reader.next() != '"') throw IllegalArgumentException("Expected '\"' at ${reader.pos}")
        val sb = StringBuilder()
        while (true) {
            val c = reader.next() ?: throw IllegalArgumentException("Unterminated string")
            if (c == '"') break
            if (c == '\\') {
                when (val esc = reader.next()) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    // 'f' -> sb.append('\f') // \f not supported in Kotlin common stdlib char?
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val code = (1..4).map { reader.next() }.joinToString("")
                        sb.append(code.toInt(16).toChar())
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseNumber(reader: StringReader): Number {
        val sb = StringBuilder()
        while (reader.peek()?.let { it.isDigit() || it == '.' || it == '-' || it == 'e' || it == 'E' } == true) {
            sb.append(reader.next())
        }
        val s = sb.toString()
        return if (s.contains(".") || s.contains("e") || s.contains("E")) s.toDouble() else s.toLong()
    }

    private fun parseBoolean(reader: StringReader): Boolean {
        if (reader.peek() == 't') {
            consume(reader, "true")
            return true
        } else {
            consume(reader, "false")
            return false
        }
    }

    private fun parseNull(reader: StringReader): Nothing? {
        consume(reader, "null")
        return null
    }

    private fun consume(reader: StringReader, expected: String) {
        for (c in expected) {
            if (reader.next() != c) throw IllegalArgumentException("Expected '$expected'")
        }
    }
}
