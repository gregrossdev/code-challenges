package dev.gregross.challenges.lb

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest as JHttpRequest
import java.net.http.HttpResponse as JHttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ProxyServer(
    private val port: Int,
    private val backendSelector: () -> Backend?,
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null

    fun start() {
        running.set(true)
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val socket = ServerSocket(port)
        serverSocket = socket
        println("Load balancer listening on port $port")

        executor.submit {
            while (running.get()) {
                try {
                    val clientSocket = socket.accept()
                    executor.submit { handleConnection(clientSocket) }
                } catch (_: IOException) {
                    if (!running.get()) break
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        serverSocket?.close()
    }

    private fun handleConnection(clientSocket: Socket) {
        clientSocket.use { socket ->
            try {
                val request = HttpRequest.parse(socket.getInputStream())
                val clientIp = socket.inetAddress.hostAddress
                println("Received ${request.method} ${request.path} from $clientIp")

                val backend = backendSelector()
                if (backend == null) {
                    val response = HttpResponse.error(503, "Service Unavailable", "No healthy backends available")
                    response.writeTo(socket.getOutputStream())
                    return
                }

                println("Forwarding to ${backend.url}")
                val response = forwardRequest(request, backend, clientIp)
                response.writeTo(socket.getOutputStream())
            } catch (e: Exception) {
                try {
                    val response = HttpResponse.error(502, "Bad Gateway", "Error: ${e.message}")
                    response.writeTo(socket.getOutputStream())
                } catch (_: IOException) {
                    // Client already disconnected
                }
            }
        }
    }

    private fun forwardRequest(request: HttpRequest, backend: Backend, clientIp: String): HttpResponse {
        val uri = URI.create("${backend.url}${request.path}")
        val builder = JHttpRequest.newBuilder().uri(uri)

        // Forward headers, skip hop-by-hop
        val hopByHop = setOf("connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade")
        for ((name, value) in request.headers) {
            if (name.lowercase() !in hopByHop && name.lowercase() != "host") {
                try {
                    builder.header(name, value)
                } catch (_: IllegalArgumentException) {
                    // Skip restricted headers
                }
            }
        }
        builder.header("X-Forwarded-For", clientIp)

        val backendResponse = client.send(builder.GET().build(), JHttpResponse.BodyHandlers.ofByteArray())

        val responseHeaders = mutableMapOf<String, String>()
        for ((name, values) in backendResponse.headers().map()) {
            if (name.lowercase() !in hopByHop && !name.startsWith(":")) {
                responseHeaders[name] = values.joinToString(", ")
            }
        }

        val statusText = when (backendResponse.statusCode()) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            301 -> "Moved Permanently"
            302 -> "Found"
            304 -> "Not Modified"
            400 -> "Bad Request"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            else -> "Unknown"
        }

        return HttpResponse(
            statusCode = backendResponse.statusCode(),
            statusText = statusText,
            headers = responseHeaders,
            body = backendResponse.body(),
        )
    }
}
