package dev.gregross.challenges.diff

sealed interface DiffEntry {
    val line: String

    data class Common(override val line: String) : DiffEntry
    data class Added(override val line: String) : DiffEntry
    data class Removed(override val line: String) : DiffEntry
}
