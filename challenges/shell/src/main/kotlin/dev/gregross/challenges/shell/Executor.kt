package dev.gregross.challenges.shell

import java.io.File
import java.io.IOException

class Executor {

    fun execute(pipeline: List<List<String>>, workingDir: File): Int {
        if (pipeline.isEmpty()) return 0

        return if (pipeline.size == 1) {
            executeSingle(pipeline[0], workingDir)
        } else {
            executePipeline(pipeline, workingDir)
        }
    }

    private fun executeSingle(command: List<String>, workingDir: File): Int {
        return try {
            val pb = ProcessBuilder(command)
                .directory(workingDir)
                .inheritIO()
            pb.start().waitFor()
        } catch (e: IOException) {
            System.err.println("gig-sh: ${command[0]}: command not found")
            127
        }
    }

    private fun executePipeline(pipeline: List<List<String>>, workingDir: File): Int {
        val processes = mutableListOf<Process>()

        try {
            for ((index, command) in pipeline.withIndex()) {
                val pb = ProcessBuilder(command).directory(workingDir)

                if (index == 0) {
                    pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
                } else {
                    pb.redirectInput(ProcessBuilder.Redirect.PIPE)
                }

                if (index == pipeline.lastIndex) {
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
                }

                pb.redirectError(ProcessBuilder.Redirect.INHERIT)

                val process = pb.start()
                processes.add(process)

                // Wire previous process stdout → current process stdin
                if (index > 0) {
                    val prevProcess = processes[index - 1]
                    Thread.startVirtualThread {
                        prevProcess.inputStream.use { src ->
                            process.outputStream.use { dst ->
                                src.copyTo(dst)
                            }
                        }
                    }
                }
            }

            // Wait for all processes
            var lastExitCode = 0
            for (process in processes) {
                lastExitCode = process.waitFor()
            }
            return lastExitCode
        } catch (e: IOException) {
            System.err.println("gig-sh: ${pipeline[0][0]}: command not found")
            processes.forEach { it.destroyForcibly() }
            return 127
        }
    }
}
