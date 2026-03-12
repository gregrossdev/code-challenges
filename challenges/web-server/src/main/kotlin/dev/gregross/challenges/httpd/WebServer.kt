package dev.gregross.challenges.httpd

import java.io.File
import java.net.ServerSocket
import java.net.Socket

class WebServer(private val port: Int, docRoot: File) {

    private val handler = StaticFileHandler(docRoot)
    private val parser = RequestParser()
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        running = true
        val ss = ServerSocket(port)
        serverSocket = ss
        println("gig-httpd listening on port $port")

        while (running) {
            val client = try {
                ss.accept()
            } catch (_: Exception) {
                if (!running) break
                continue
            }
            Thread.startVirtualThread { handleClient(client) }
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            try {
                val request = parser.parse(socket.getInputStream())
                val response = if (request != null) {
                    handler.handle(request)
                } else {
                    HttpResponse.BAD_REQUEST
                }
                response.writeTo(socket.getOutputStream())
            } catch (_: Exception) {
                // Client disconnected or I/O error — silently close
            }
        }
    }
}
