package dev.gregross.challenges.urls

import java.security.MessageDigest

class CodeGenerator {

    private val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun generate(url: String, attempt: Int = 0): String {
        val input = if (attempt == 0) url else "$url#$attempt"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())

        // Take first 6 bytes (48 bits) and encode as base62
        var value = 0L
        for (i in 0 until 6) {
            value = (value shl 8) or (hash[i].toLong() and 0xFF)
        }

        return toBase62(value, 8)
    }

    private fun toBase62(value: Long, length: Int): String {
        var remaining = value
        val chars = CharArray(length)
        for (i in length - 1 downTo 0) {
            chars[i] = alphabet[(remaining % 62).toInt()]
            remaining /= 62
        }
        return String(chars)
    }
}
