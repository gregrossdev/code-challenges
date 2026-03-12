package dev.gregross.challenges.grep

import java.io.File
import java.io.InputStream
import java.io.PrintStream

class GrepProcessor(private val matcher: Matcher) {

    fun processStream(
        input: InputStream,
        output: PrintStream,
        filename: String? = null,
        showFilename: Boolean = false,
    ): Boolean {
        var matched = false
        input.bufferedReader().forEachLine { line ->
            if (matcher.matches(line)) {
                matched = true
                if (showFilename && filename != null) {
                    output.println("$filename:$line")
                } else {
                    output.println(line)
                }
            }
        }
        return matched
    }

    fun processFile(
        file: File,
        output: PrintStream,
        showFilename: Boolean = false,
    ): Boolean {
        if (isBinary(file)) {
            return false
        }
        return file.inputStream().use { input ->
            processStream(input, output, file.path, showFilename)
        }
    }

    private fun isBinary(file: File): Boolean {
        val bytes = file.inputStream().use { it.readNBytes(8192) }
        return bytes.any { it == 0.toByte() }
    }
}
