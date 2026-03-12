package dev.gregross.challenges.shell

import java.io.File

class History {

    private val entries = mutableListOf<String>()

    fun add(command: String) {
        entries.add(command)
    }

    fun entries(): List<String> = entries.toList()

    fun size(): Int = entries.size

    fun load(file: File) {
        if (file.exists()) {
            file.readLines().filter { it.isNotBlank() }.forEach { entries.add(it) }
        }
    }

    fun save(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(entries.joinToString("\n") + if (entries.isNotEmpty()) "\n" else "")
    }
}
