package dev.gregross.challenges.lb

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class HttpRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String>,
) {
    companion object {
        fun parse(input: InputStream): HttpRequest {
            val reader = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine()
                ?: throw IllegalArgumentException("Empty request")

            val parts = requestLine.split(" ", limit = 3)
            if (parts.size != 3) {
                throw IllegalArgumentException("Malformed request line: $requestLine")
            }

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val name = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[name] = value
                }
            }

            return HttpRequest(
                method = parts[0],
                path = parts[1],
                version = parts[2],
                headers = headers,
            )
        }
    }
}
