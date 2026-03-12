package dev.gregross.challenges.cat

import java.io.File
import java.io.FileInputStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var numberAll = false
    var numberNonBlank = false
    val files = mutableListOf<String>()

    for (arg in args) {
        when (arg) {
            "-n" -> numberAll = true
            "-b" -> numberNonBlank = true
            else -> files.add(arg)
        }
    }

    // -b overrides -n
    if (numberNonBlank) numberAll = false

    // No files means read stdin
    if (files.isEmpty()) files.add("-")

    val processor = CatProcessor(numberAll, numberNonBlank)
    var hasError = false

    for (file in files) {
        if (file == "-") {
            processor.process(System.`in`, System.out)
        } else {
            val f = File(file)
            if (!f.exists()) {
                System.err.println("gig-cat: $file: No such file or directory")
                hasError = true
                continue
            }
            FileInputStream(f).use { input ->
                processor.process(input, System.out)
            }
        }
    }

    exitProcess(if (hasError) 1 else 0)
}
