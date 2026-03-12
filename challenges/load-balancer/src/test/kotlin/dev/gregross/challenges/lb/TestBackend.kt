package dev.gregross.challenges.lb

import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class TestBackend(
    val port: Int,
    val name: String,
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    val requestCount = AtomicInteger(0)

    fun start() {
        running.set(true)
        val socket = ServerSocket(port)
        serverSocket = socket
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        executor.submit {
            while (running.get()) {
                try {
                    val client = socket.accept()
                    executor.submit {
                        client.use { s ->
                            try {
                                // Read request
                                val reader = s.getInputStream().bufferedReader()
                                var line = reader.readLine()
                                while (line != null && line.isNotEmpty()) {
                                    line = reader.readLine()
                                }
                                requestCount.incrementAndGet()

                                // Write response
                                val body = "Hello from $name"
                                val response = buildString {
                                    append("HTTP/1.1 200 OK\r\n")
                                    append("Content-Type: text/plain\r\n")
                                    append("Content-Length: ${body.length}\r\n")
                                    append("Connection: close\r\n")
                                    append("\r\n")
                                    append(body)
                                }
                                s.getOutputStream().write(response.toByteArray())
                                s.getOutputStream().flush()
                            } catch (_: IOException) {}
                        }
                    }
                } catch (_: IOException) {
                    if (!running.get()) return@submit
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        serverSocket?.close()
    }
}
