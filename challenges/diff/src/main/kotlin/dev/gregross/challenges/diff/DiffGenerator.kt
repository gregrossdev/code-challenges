package dev.gregross.challenges.diff

class DiffGenerator(private val lcs: LcsComputer = LcsComputer()) {

    fun generate(original: List<String>, modified: List<String>): List<DiffEntry> {
        val common = lcs.compute(original, modified)
        val entries = mutableListOf<DiffEntry>()

        var oi = 0
        var mi = 0
        var ci = 0

        while (ci < common.size) {
            // Emit removed lines (in original but before the next common line)
            while (oi < original.size && original[oi] != common[ci]) {
                entries.add(DiffEntry.Removed(original[oi]))
                oi++
            }
            // Emit added lines (in modified but before the next common line)
            while (mi < modified.size && modified[mi] != common[ci]) {
                entries.add(DiffEntry.Added(modified[mi]))
                mi++
            }
            // Emit the common line
            entries.add(DiffEntry.Common(common[ci]))
            oi++
            mi++
            ci++
        }

        // Remaining lines after last common line
        while (oi < original.size) {
            entries.add(DiffEntry.Removed(original[oi]))
            oi++
        }
        while (mi < modified.size) {
            entries.add(DiffEntry.Added(modified[mi]))
            mi++
        }

        return entries
    }
}
