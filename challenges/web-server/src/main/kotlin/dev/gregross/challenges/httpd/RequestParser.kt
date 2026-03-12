package dev.gregross.challenges.httpd

import java.io.BufferedReader
import java.io.InputStream

class RequestParser {

    fun parse(input: InputStream): HttpRequest? {
        val reader = input.bufferedReader()
        return parse(reader)
    }

    fun parse(reader: BufferedReader): HttpRequest? {
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

        return HttpRequest(method, path, version, headers)
    }
}
