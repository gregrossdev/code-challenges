package dev.gregross.challenges.urls

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var port = 8080
    var baseUrl = "http://localhost:8080"

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--port" -> {
                i++
                port = args.getOrNull(i)?.toIntOrNull() ?: run {
                    System.err.println("gig-urls: --port requires a number")
                    exitProcess(2)
                }
            }
            "--base-url" -> {
                i++
                baseUrl = args.getOrNull(i) ?: run {
                    System.err.println("gig-urls: --base-url requires a URL")
                    exitProcess(2)
                }
            }
            else -> {
                System.err.println("Usage: gig-urls [--port PORT] [--base-url URL]")
                exitProcess(2)
            }
        }
        i++
    }

    // Sync base-url port if only port was changed
    if (baseUrl == "http://localhost:8080" && port != 8080) {
        baseUrl = "http://localhost:$port"
    }

    val server = UrlServer(port, baseUrl)
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down...")
        server.stop()
    })
    server.start()
}
