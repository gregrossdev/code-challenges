package dev.gregross.challenges.lb

import java.io.OutputStream

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    fun writeTo(output: OutputStream) {
        val writer = output.bufferedWriter()
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        for ((name, value) in headers) {
            writer.write("$name: $value\r\n")
        }
        writer.write("Connection: close\r\n")
        writer.write("Content-Length: ${body.size}\r\n")
        writer.write("\r\n")
        writer.flush()
        output.write(body)
        output.flush()
    }

    companion object {
        fun error(statusCode: Int, statusText: String, message: String): HttpResponse {
            val body = message.toByteArray()
            return HttpResponse(statusCode, statusText, emptyMap(), body)
        }
    }
}
