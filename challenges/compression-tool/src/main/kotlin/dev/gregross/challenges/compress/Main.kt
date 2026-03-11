package dev.gregross.challenges.compress

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") && it != "-o" }
    val decompress = "-d" in flags

    val outputIndex = args.indexOf("-o")
    val outputPath = if (outputIndex >= 0 && outputIndex + 1 < args.size) {
        args[outputIndex + 1]
    } else {
        null
    }

    val inputPath = args.firstOrNull { !it.startsWith("-") && it != outputPath }

    if (inputPath == null || outputPath == null) {
        System.err.println("Usage: gig-compress [-d] <input> -o <output>")
        exitProcess(1)
    }

    val inputFile = File(inputPath)
    if (!inputFile.exists()) {
        System.err.println("gig-compress: $inputPath: No such file or directory")
        exitProcess(1)
    }

    try {
        if (decompress) {
            val compressed = inputFile.readBytes()
            val decompressed = Decoder.decode(compressed)
            File(outputPath).writeText(decompressed)
            println("Decompressed: ${compressed.size} → ${decompressed.length} bytes")
        } else {
            val input = inputFile.readText()
            val compressed = Encoder.encode(input)
            File(outputPath).writeBytes(compressed)
            val ratio = 100 - (compressed.size * 100 / input.toByteArray().size)
            println("Compressed: ${input.toByteArray().size} → ${compressed.size} bytes ($ratio% reduction)")
        }
        exitProcess(0)
    } catch (e: Exception) {
        System.err.println("gig-compress: ${e.message}")
        exitProcess(1)
    }
}
