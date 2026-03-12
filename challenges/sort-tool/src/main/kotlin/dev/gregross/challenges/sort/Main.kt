package dev.gregross.challenges.sort

import java.io.File

fun main(args: Array<String>) {
    var unique = false
    var algorithm = "merge"
    var randomSort = false
    var filePath: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-u" -> unique = true
            "--random-sort" -> randomSort = true
            "--algorithm" -> {
                if (i + 1 >= args.size) {
                    System.err.println("Error: --algorithm requires a value")
                    System.exit(1)
                }
                algorithm = args[++i]
            }
            else -> {
                if (args[i].startsWith("-")) {
                    System.err.println("Error: unknown option ${args[i]}")
                    System.exit(1)
                }
                filePath = args[i]
            }
        }
        i++
    }

    val input = if (filePath != null) {
        val file = File(filePath)
        if (!file.exists()) {
            System.err.println("Error: file not found: $filePath")
            System.exit(1)
        }
        file.inputStream()
    } else {
        System.`in`
    }

    val processor = SortProcessor(
        algorithm = algorithm,
        unique = unique,
        randomSort = randomSort,
    )
    processor.process(input, System.out)
}
