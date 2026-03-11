package dev.gregross.challenges.cut

import java.io.InputStream
import java.io.PrintStream

class CutProcessor(
    private val delimiter: Char = '\t',
    private val fields: List<Int>,
) {
    fun process(input: InputStream, output: PrintStream) {
        input.bufferedReader().forEachLine { line ->
            if (delimiter !in line) {
                output.println(line)
            } else {
                val parts = line.split(delimiter)
                val selected = fields
                    .filter { it in 1..parts.size }
                    .map { parts[it - 1] }
                output.println(selected.joinToString(delimiter.toString()))
            }
        }
    }
}
