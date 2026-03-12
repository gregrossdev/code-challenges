package dev.gregross.challenges.diff

class DiffFormatter {

    fun format(entries: List<DiffEntry>): String {
        val sb = StringBuilder()
        var i = 0

        while (i < entries.size) {
            when (entries[i]) {
                is DiffEntry.Common -> i++
                is DiffEntry.Removed, is DiffEntry.Added -> {
                    if (sb.isNotEmpty()) sb.appendLine()
                    i = formatChangeGroup(entries, i, sb)
                }
            }
        }

        return sb.toString()
    }

    private fun formatChangeGroup(entries: List<DiffEntry>, start: Int, sb: StringBuilder): Int {
        var i = start
        val removed = mutableListOf<String>()
        val added = mutableListOf<String>()

        // Collect contiguous removed and added lines
        while (i < entries.size && entries[i] !is DiffEntry.Common) {
            when (val entry = entries[i]) {
                is DiffEntry.Removed -> removed.add(entry.line)
                is DiffEntry.Added -> added.add(entry.line)
                is DiffEntry.Common -> break
            }
            i++
        }

        for (line in removed) {
            sb.appendLine("< $line")
        }
        if (removed.isNotEmpty() && added.isNotEmpty()) {
            sb.appendLine("---")
        }
        for (line in added) {
            sb.appendLine("> $line")
        }

        return i
    }
}
