package dev.gregross.challenges.lb

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var port = 8080
    var backendUrls = emptyList<String>()
    var healthInterval = 10L

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--port" -> {
                i++
                port = args.getOrNull(i)?.toIntOrNull() ?: run {
                    System.err.println("gig-lb: --port requires a number")
                    exitProcess(1)
                }
            }
            "--backends" -> {
                i++
                backendUrls = args.getOrNull(i)?.split(",")?.map { it.trim() } ?: run {
                    System.err.println("gig-lb: --backends requires comma-separated URLs")
                    exitProcess(1)
                }
            }
            "--health-interval" -> {
                i++
                healthInterval = args.getOrNull(i)?.toLongOrNull() ?: run {
                    System.err.println("gig-lb: --health-interval requires a number (seconds)")
                    exitProcess(1)
                }
            }
            else -> {
                System.err.println("gig-lb: unknown argument '${args[i]}'")
                System.err.println("Usage: gig-lb --backends <url1,url2,...> [--port <port>] [--health-interval <seconds>]")
                exitProcess(1)
            }
        }
        i++
    }

    if (backendUrls.isEmpty()) {
        System.err.println("Usage: gig-lb --backends <url1,url2,...> [--port <port>] [--health-interval <seconds>]")
        exitProcess(1)
    }

    val backends = backendUrls.map { Backend(it) }
    val roundRobin = RoundRobin(backends)
    val healthChecker = HealthChecker(backends, healthInterval)
    val server = ProxyServer(port) { roundRobin.next() }

    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down...")
        server.stop()
        healthChecker.stop()
    })

    println("Starting load balancer on port $port")
    println("Backends: ${backendUrls.joinToString(", ")}")
    println("Health check interval: ${healthInterval}s")

    healthChecker.start()
    server.start()

    // Keep main thread alive
    Thread.currentThread().join()
}
