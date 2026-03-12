package dev.gregross.challenges.urls

data class HttpRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
)
