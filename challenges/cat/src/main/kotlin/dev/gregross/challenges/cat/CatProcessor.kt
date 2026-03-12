package dev.gregross.challenges.cat

import java.io.InputStream
import java.io.PrintStream

class CatProcessor(
    private val numberAll: Boolean = false,
    private val numberNonBlank: Boolean = false,
) {
    private var lineNumber = 0

    fun process(input: InputStream, output: PrintStream) {
        input.bufferedReader().forEachLine { line ->
            if (numberNonBlank) {
                if (line.isNotEmpty()) {
                    lineNumber++
                    output.println(String.format("%6d\t%s", lineNumber, line))
                } else {
                    output.println()
                }
            } else if (numberAll) {
                lineNumber++
                output.println(String.format("%6d\t%s", lineNumber, line))
            } else {
                output.println(line)
            }
        }
    }
}
