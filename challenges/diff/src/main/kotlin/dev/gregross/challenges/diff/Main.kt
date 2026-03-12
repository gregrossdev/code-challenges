package dev.gregross.challenges.diff

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Usage: gig-diff <original> <new>")
        exitProcess(2)
    }

    val originalFile = File(args[0])
    val modifiedFile = File(args[1])

    if (!originalFile.exists()) {
        System.err.println("gig-diff: ${args[0]}: No such file")
        exitProcess(2)
    }
    if (!modifiedFile.exists()) {
        System.err.println("gig-diff: ${args[1]}: No such file")
        exitProcess(2)
    }

    val original = originalFile.readLines()
    val modified = modifiedFile.readLines()

    val generator = DiffGenerator()
    val formatter = DiffFormatter()

    val entries = generator.generate(original, modified)
    val output = formatter.format(entries)

    if (output.isEmpty()) {
        exitProcess(0)
    } else {
        print(output)
        exitProcess(1)
    }
}
