package dev.gregross.challenges.httpd

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var port = 8080
    var docRootPath = "./www"

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--port" -> {
                i++
                port = args.getOrNull(i)?.toIntOrNull() ?: run {
                    System.err.println("gig-httpd: --port requires a number")
                    exitProcess(2)
                }
            }
            "--docroot" -> {
                i++
                docRootPath = args.getOrNull(i) ?: run {
                    System.err.println("gig-httpd: --docroot requires a path")
                    exitProcess(2)
                }
            }
            else -> {
                System.err.println("Usage: gig-httpd [--port PORT] [--docroot PATH]")
                exitProcess(2)
            }
        }
        i++
    }

    val docRoot = File(docRootPath)
    if (!docRoot.isDirectory) {
        System.err.println("gig-httpd: document root '$docRootPath' is not a directory")
        exitProcess(2)
    }

    val server = WebServer(port, docRoot)
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down...")
        server.stop()
    })
    server.start()
}
