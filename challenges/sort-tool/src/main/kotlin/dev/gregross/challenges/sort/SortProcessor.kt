package dev.gregross.challenges.sort

import java.io.InputStream
import java.io.PrintStream

class SortProcessor(
    private val algorithm: String = "merge",
    private val unique: Boolean = false,
    private val randomSort: Boolean = false,
) {
    private val sorters = mapOf(
        "merge" to MergeSorter(),
        "quick" to QuickSorter(),
        "heap" to HeapSorter(),
        "radix" to RadixSorter(),
    )

    fun process(input: InputStream, output: PrintStream) {
        val lines = input.bufferedReader().readLines()

        val sorted = if (randomSort) {
            lines.shuffled()
        } else {
            val sorter = sorters[algorithm]
                ?: error("Unknown algorithm: $algorithm. Available: ${sorters.keys.joinToString()}")
            sorter.sort(lines)
        }

        val result = if (unique) deduplicate(sorted) else sorted

        for (line in result) {
            output.println(line)
        }
    }

    private fun deduplicate(sorted: List<String>): List<String> {
        if (sorted.isEmpty()) return sorted
        val result = mutableListOf(sorted[0])
        for (i in 1 until sorted.size) {
            if (sorted[i] != sorted[i - 1]) {
                result.add(sorted[i])
            }
        }
        return result
    }
}
