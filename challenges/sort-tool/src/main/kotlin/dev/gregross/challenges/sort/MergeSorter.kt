package dev.gregross.challenges.sort

class MergeSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        if (lines.size <= 1) return lines

        val mid = lines.size / 2
        val left = sort(lines.subList(0, mid))
        val right = sort(lines.subList(mid, lines.size))

        return merge(left, right)
    }

    private fun merge(left: List<String>, right: List<String>): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        var j = 0

        while (i < left.size && j < right.size) {
            if (left[i] <= right[j]) {
                result.add(left[i++])
            } else {
                result.add(right[j++])
            }
        }

        while (i < left.size) result.add(left[i++])
        while (j < right.size) result.add(right[j++])

        return result
    }
}
