package dev.gregross.challenges.grep

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var ignoreCase = false
    var invert = false
    var recursive = false
    val paths = mutableListOf<String>()
    var pattern: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-i" -> ignoreCase = true
            "-v" -> invert = true
            "-r", "-R" -> recursive = true
            "-iv", "-vi" -> { ignoreCase = true; invert = true }
            "-ri", "-ir", "-riv", "-rvi", "-vri", "-vir", "-ivr", "-irv" -> {
                recursive = true; ignoreCase = true; if (args[i].contains('v')) invert = true
            }
            else -> {
                if (pattern == null) {
                    pattern = args[i]
                } else {
                    paths.add(args[i])
                }
            }
        }
        i++
    }

    if (pattern == null) {
        System.err.println("Usage: gig-grep [-ivr] PATTERN [FILE...]")
        exitProcess(2)
    }

    val matcher = Matcher(pattern, ignoreCase = ignoreCase, invert = invert)
    val processor = GrepProcessor(matcher)
    val searcher = FileSearcher()

    val matched: Boolean
    if (paths.isEmpty()) {
        matched = processor.processStream(System.`in`, System.out)
    } else {
        val files = searcher.findFiles(paths, recursive)
        val showFilename = files.size > 1 || recursive
        matched = files.fold(false) { acc, file ->
            processor.processFile(file, System.out, showFilename) || acc
        }
    }

    exitProcess(if (matched) 0 else 1)
}
