package dev.gregross.challenges.wc

import java.io.File

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }
    val files = args.filter { !it.startsWith("-") }

    val input = if (files.isNotEmpty()) {
        val file = File(files.first())
        if (!file.exists()) {
            System.err.println("gig-wc: ${files.first()}: No such file or directory")
            return
        }
        file.inputStream()
    } else {
        System.`in`
    }

    val counts = countFromStream(input)
    val fileName = files.firstOrNull() ?: ""

    val output = buildString {
        val activeFlags = flags.ifEmpty { listOf("-l", "-w", "-c") }
        for (flag in activeFlags) {
            when (flag) {
                "-c" -> append("${counts.bytes} ")
                "-l" -> append("${counts.lines} ")
                "-w" -> append("${counts.words} ")
                "-m" -> append("${counts.chars} ")
            }
        }
        if (fileName.isNotEmpty()) append(fileName)
    }.trimEnd()

    println(output)
}
