package dev.gregross.challenges.json

fun prettyPrint(value: JsonValue, indent: Int = 0): String {
    val pad = "  ".repeat(indent)
    val padInner = "  ".repeat(indent + 1)
    return when (value) {
        is JsonValue.JsonNull -> "null"
        is JsonValue.JsonBool -> value.value.toString()
        is JsonValue.JsonNumber -> {
            if (value.value == value.value.toLong().toDouble()) {
                value.value.toLong().toString()
            } else {
                value.value.toString()
            }
        }
        is JsonValue.JsonString -> "\"${escapeString(value.value)}\""
        is JsonValue.JsonArray -> {
            if (value.elements.isEmpty()) return "[]"
            val items = value.elements.joinToString(",\n$padInner") { prettyPrint(it, indent + 1) }
            "[\n$padInner$items\n$pad]"
        }
        is JsonValue.JsonObject -> {
            if (value.members.isEmpty()) return "{}"
            val entries = value.members.entries.joinToString(",\n$padInner") { (k, v) ->
                "\"${escapeString(k)}\": ${prettyPrint(v, indent + 1)}"
            }
            "{\n$padInner$entries\n$pad}"
        }
    }
}

private fun escapeString(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) {
                append("\\u${c.code.toString(16).padStart(4, '0')}")
            } else {
                append(c)
            }
        }
    }
}
