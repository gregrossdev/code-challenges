package dev.gregross.challenges.shell

import java.io.BufferedReader
import java.io.File
import java.io.PrintStream

class Shell(
    private val input: BufferedReader = System.`in`.bufferedReader(),
    private val output: PrintStream = System.out,
    private val error: PrintStream = System.err,
) {
    val parser = InputParser()
    val executor = Executor()
    var workingDir: File = File(System.getProperty("user.dir"))
    @Volatile var running = true

    fun run() {
        while (running) {
            output.print("gig-sh> ")
            output.flush()
            val line = input.readLine() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            processInput(trimmed)
        }
    }

    fun processInput(input: String) {
        val pipeline = parser.parsePipeline(input)
        if (pipeline.isEmpty()) return
        executor.execute(pipeline, workingDir)
    }
}
