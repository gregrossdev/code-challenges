package dev.gregross.challenges.redis

fun main(args: Array<String>) {
    var port = 6379

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--port" -> {
                if (i + 1 >= args.size) {
                    System.err.println("Error: --port requires a value")
                    System.exit(1)
                }
                port = args[++i].toIntOrNull() ?: run {
                    System.err.println("Error: invalid port number")
                    System.exit(1)
                    return
                }
            }
            else -> {
                System.err.println("Error: unknown option ${args[i]}")
                System.exit(1)
            }
        }
        i++
    }

    val server = RedisServer(port)
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down...")
        server.stop()
    })

    println("gig-redis listening on port $port")
    server.start()

    // Keep main thread alive
    Thread.currentThread().join()
}
