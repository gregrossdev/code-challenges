package dev.gregross.challenges.wc

import java.io.InputStream

data class Counts(
    val lines: Long = 0,
    val words: Long = 0,
    val bytes: Long = 0,
    val chars: Long = 0,
)

fun countFromStream(input: InputStream): Counts {
    val content = input.readAllBytes()
    val text = String(content, Charsets.UTF_8)

    return Counts(
        bytes = content.size.toLong(),
        lines = text.count { it == '\n' }.toLong(),
        words = text.trim().split(Regex("\\s+")).let { if (it == listOf("")) 0L else it.size.toLong() },
        chars = text.length.toLong(),
    )
}
