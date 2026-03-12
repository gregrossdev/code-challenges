package dev.gregross.challenges.httpd

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
        fun ok(body: ByteArray, contentType: String): HttpResponse {
            return HttpResponse(
                200, "OK",
                mapOf(
                    "Content-Type" to contentType,
                    "Content-Length" to body.size.toString(),
                    "Connection" to "close",
                    "Server" to "gig-httpd",
                ),
                body,
            )
        }

        fun error(statusCode: Int, reason: String): HttpResponse {
            val body = "$statusCode $reason\n".toByteArray()
            return HttpResponse(
                statusCode, reason,
                mapOf(
                    "Content-Type" to "text/plain",
                    "Content-Length" to body.size.toString(),
                    "Connection" to "close",
                    "Server" to "gig-httpd",
                ),
                body,
            )
        }

        val BAD_REQUEST = error(400, "Bad Request")
        val FORBIDDEN = error(403, "Forbidden")
        val NOT_FOUND = error(404, "Not Found")
        val METHOD_NOT_ALLOWED = error(405, "Method Not Allowed")
    }
}
