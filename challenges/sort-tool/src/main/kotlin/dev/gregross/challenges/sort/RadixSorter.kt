package dev.gregross.challenges.sort

class RadixSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        if (lines.size <= 1) return lines

        val maxLen = lines.maxOf { it.length }

        // LSD radix sort — process from rightmost character to leftmost
        var result = lines.toList()
        for (pos in maxLen - 1 downTo 0) {
            result = countingSortByChar(result, pos)
        }

        return result
    }

    private fun countingSortByChar(lines: List<String>, pos: Int): List<String> {
        // 257 buckets: index 0 for strings shorter than pos, 1-256 for char values
        val buckets = Array(257) { mutableListOf<String>() }

        for (line in lines) {
            val bucketIndex = if (pos < line.length) line[pos].code + 1 else 0
            buckets[bucketIndex].add(line)
        }

        return buckets.flatMap { it }
    }
}
