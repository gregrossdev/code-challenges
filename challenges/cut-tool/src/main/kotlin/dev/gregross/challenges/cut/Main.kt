package dev.gregross.challenges.cut

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var delimiter = '\t'
    var fields: List<Int>? = null
    val fileArgs = mutableListOf<String>()

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg.startsWith("-f") -> {
                val value = if (arg.length > 2) arg.substring(2) else {
                    i++
                    if (i >= args.size) {
                        System.err.println("gig-cut: -f requires a field list")
                        exitProcess(1)
                    }
                    args[i]
                }
                fields = parseFields(value)
            }
            arg.startsWith("-d") -> {
                val value = if (arg.length > 2) arg.substring(2) else {
                    i++
                    if (i >= args.size) {
                        System.err.println("gig-cut: -d requires a delimiter")
                        exitProcess(1)
                    }
                    args[i]
                }
                if (value.length != 1) {
                    System.err.println("gig-cut: the delimiter must be a single character")
                    exitProcess(1)
                }
                delimiter = value[0]
            }
            else -> fileArgs.add(arg)
        }
        i++
    }

    if (fields == null) {
        System.err.println("Usage: gig-cut -f <fields> [-d <delimiter>] [file ...]")
        exitProcess(1)
    }

    val processor = CutProcessor(delimiter, fields)

    if (fileArgs.isEmpty() || fileArgs == listOf("-")) {
        processor.process(System.`in`, System.out)
    } else {
        for (path in fileArgs) {
            val file = File(path)
            if (!file.exists()) {
                System.err.println("gig-cut: $path: No such file or directory")
                exitProcess(1)
            }
            processor.process(file.inputStream(), System.out)
        }
    }
}

private fun parseFields(value: String): List<Int> {
    return value.split(",", " ")
        .filter { it.isNotBlank() }
        .map { it.trim().toIntOrNull() ?: run {
            System.err.println("gig-cut: invalid field: $it")
            exitProcess(1)
        }}
}
