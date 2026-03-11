package dev.gregross.challenges.json

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    val files = args.filter { !it.startsWith("--") }
    val validateOnly = "--validate" in flags

    val input = if (files.isNotEmpty()) {
        val file = File(files.first())
        if (!file.exists()) {
            System.err.println("gig-json: ${files.first()}: No such file or directory")
            exitProcess(1)
        }
        file.readText()
    } else {
        System.`in`.bufferedReader().readText()
    }

    try {
        val tokens = Lexer(input).tokenize()
        val value = Parser(tokens).parse()
        if (!validateOnly) {
            println(prettyPrint(value))
        }
        exitProcess(0)
    } catch (e: JsonParseException) {
        System.err.println("gig-json: ${e.message}")
        exitProcess(1)
    }
}
