package dev.gregross.challenges.json

sealed interface JsonValue {
    data class JsonObject(val members: LinkedHashMap<String, JsonValue>) : JsonValue
    data class JsonArray(val elements: List<JsonValue>) : JsonValue
    data class JsonString(val value: String) : JsonValue
    data class JsonNumber(val value: Double) : JsonValue
    data class JsonBool(val value: Boolean) : JsonValue
    data object JsonNull : JsonValue
}
