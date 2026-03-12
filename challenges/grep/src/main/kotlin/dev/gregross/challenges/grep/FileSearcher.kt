package dev.gregross.challenges.grep

import java.io.File

class FileSearcher {

    fun findFiles(paths: List<String>, recursive: Boolean): List<File> {
        val files = mutableListOf<File>()
        for (path in paths) {
            val file = File(path)
            if (!file.exists()) {
                System.err.println("gig-grep: $path: No such file or directory")
                continue
            }
            if (file.isFile) {
                files.add(file)
            } else if (file.isDirectory) {
                if (recursive) {
                    file.walkTopDown()
                        .filter { it.isFile }
                        .forEach { files.add(it) }
                } else {
                    System.err.println("gig-grep: $path: Is a directory")
                }
            }
        }
        return files.sortedBy { it.path }
    }
}
