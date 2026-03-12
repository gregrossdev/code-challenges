package dev.gregross.challenges.urls

import java.net.ServerSocket
import java.net.Socket

class UrlServer(private val port: Int, baseUrl: String) {

    private val store = UrlStore()
    private val generator = CodeGenerator()
    private val handler = ApiHandler(store, generator, baseUrl)
    private val parser = RequestParser()
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        running = true
        val ss = ServerSocket(port)
        serverSocket = ss
        println("gig-urls listening on port $port")

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
                    HttpResponse.error(400, "Bad Request", "Malformed request")
                }
                response.writeTo(socket.getOutputStream())
            } catch (_: Exception) {
                // Client disconnected or I/O error
            }
        }
    }
}
