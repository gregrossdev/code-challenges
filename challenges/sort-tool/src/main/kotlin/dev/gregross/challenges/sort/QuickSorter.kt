package dev.gregross.challenges.sort

class QuickSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        if (lines.size <= 1) return lines

        val pivot = lines[lines.size / 2]
        val less = lines.filter { it < pivot }
        val equal = lines.filter { it == pivot }
        val greater = lines.filter { it > pivot }

        return sort(less) + equal + sort(greater)
    }
}
