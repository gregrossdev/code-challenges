package dev.gregross.challenges.urls

import java.io.OutputStream

data class HttpResponse(
    val statusCode: Int,
    val reason: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
) {

    fun writeTo(output: OutputStream) {
        val headerBlock = buildString {
            append("HTTP/1.1 $statusCode $reason\r\n")
            for ((name, value) in headers) {
                append("$name: $value\r\n")
            }
            append("\r\n")
        }
        output.write(headerBlock.toByteArray())
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }

    companion object {
        fun json(statusCode: Int, reason: String, json: String): HttpResponse {
            val body = json.toByteArray()
            return HttpResponse(
                statusCode, reason,
                mapOf(
                    "Content-Type" to "application/json",
                    "Content-Length" to body.size.toString(),
                    "Connection" to "close",
                    "Server" to "gig-urls",
                ),
                body,
            )
        }

        fun redirect(location: String): HttpResponse {
            return HttpResponse(
                302, "Found",
                mapOf(
                    "Location" to location,
                    "Connection" to "close",
                    "Server" to "gig-urls",
                ),
            )
        }

        fun error(statusCode: Int, reason: String, message: String): HttpResponse {
            return json(statusCode, reason, """{"error":"$message"}""")
        }
    }
}
