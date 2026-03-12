package dev.gregross.challenges.uniq

import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var count = false
    var repeated = false
    var unique = false
    var ignoreCase = false
    val positional = mutableListOf<String>()

    for (arg in args) {
        when {
            arg.startsWith("-") && arg.length > 1 && arg[1] != '-' -> {
                for (ch in arg.substring(1)) {
                    when (ch) {
                        'c' -> count = true
                        'd' -> repeated = true
                        'u' -> unique = true
                        'i' -> ignoreCase = true
                        else -> {
                            System.err.println("gig-uniq: unknown option: -$ch")
                            exitProcess(2)
                        }
                    }
                }
            }
            else -> positional.add(arg)
        }
    }

    val inputFile = positional.getOrNull(0)
    val outputFile = positional.getOrNull(1)

    val processor = UniqProcessor(count, repeated, unique, ignoreCase)

    val input = when {
        inputFile == null || inputFile == "-" -> System.`in`
        else -> {
            val file = File(inputFile)
            if (!file.exists()) {
                System.err.println("gig-uniq: $inputFile: No such file or directory")
                exitProcess(2)
            }
            file.inputStream()
        }
    }

    val output = if (outputFile != null) {
        PrintStream(File(outputFile).outputStream())
    } else {
        System.out
    }

    input.use { inp ->
        if (outputFile != null) {
            output.use { out -> processor.process(inp, out) }
        } else {
            processor.process(inp, output)
        }
    }
}
