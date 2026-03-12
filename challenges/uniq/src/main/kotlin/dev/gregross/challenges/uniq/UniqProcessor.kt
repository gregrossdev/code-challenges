package dev.gregross.challenges.uniq

import java.io.InputStream
import java.io.PrintStream

class UniqProcessor(
    private val count: Boolean = false,
    private val repeated: Boolean = false,
    private val unique: Boolean = false,
    private val ignoreCase: Boolean = false,
) {

    fun process(input: InputStream, output: PrintStream) {
        var currentLine: String? = null
        var currentCount = 0

        input.bufferedReader().forEachLine { line ->
            if (currentLine == null) {
                currentLine = line
                currentCount = 1
            } else if (linesEqual(currentLine!!, line)) {
                currentCount++
            } else {
                emitGroup(currentLine!!, currentCount, output)
                currentLine = line
                currentCount = 1
            }
        }

        if (currentLine != null) {
            emitGroup(currentLine!!, currentCount, output)
        }
    }

    private fun linesEqual(a: String, b: String): Boolean {
        return if (ignoreCase) a.equals(b, ignoreCase = true) else a == b
    }

    private fun emitGroup(line: String, groupCount: Int, output: PrintStream) {
        val isRepeated = groupCount > 1

        if (repeated && !isRepeated) return
        if (unique && isRepeated) return

        if (count) {
            output.println(String.format("%7d %s", groupCount, line))
        } else {
            output.println(line)
        }
    }
}
