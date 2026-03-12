package dev.gregross.challenges.httpd

data class HttpRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String> = emptyMap(),
)
