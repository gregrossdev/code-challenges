package dev.gregross.challenges.urls

import java.io.InputStream

class RequestParser {

    fun parse(input: InputStream): HttpRequest? {
        val reader = input.bufferedReader()

        val requestLine = reader.readLine() ?: return null
        val parts = requestLine.split(" ")
        if (parts.size != 3) return null

        val method = parts[0]
        val path = parts[1]
        val version = parts[2]

        if (!version.startsWith("HTTP/")) return null

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val name = line.substring(0, colonIndex).trim().lowercase()
                val value = line.substring(colonIndex + 1).trim()
                headers[name] = value
            }
        }

        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body = if (contentLength > 0) {
            val chars = CharArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = reader.read(chars, read, contentLength - read)
                if (n == -1) break
                read += n
            }
            String(chars, 0, read)
        } else {
            ""
        }

        return HttpRequest(method, path, version, headers, body)
    }
}
