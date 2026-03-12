package dev.gregross.challenges.redis

import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RedisServer(
    val port: Int,
    private val store: DataStore = DataStore(),
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val handler = CommandHandler(store)

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
                                val input = s.getInputStream()
                                val output = s.getOutputStream()
                                val serializer = RespSerializer(output)

                                while (running.get()) {
                                    val parser = RespParser(input)
                                    val request = try {
                                        parser.parse()
                                    } catch (_: RespException) {
                                        break // Client disconnected
                                    } catch (_: IOException) {
                                        break
                                    }

                                    val args = extractCommand(request)
                                    if (args == null) {
                                        serializer.serialize(RespValue.Error("ERR invalid command format"))
                                        continue
                                    }

                                    val response = handler.execute(args)
                                    serializer.serialize(response)
                                }
                            } catch (_: IOException) {
                                // Client disconnected
                            }
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

    private fun extractCommand(request: RespValue): List<String>? {
        if (request !is RespValue.Array) return null
        val elements = request.elements ?: return null
        return elements.map { element ->
            when (element) {
                is RespValue.BulkString -> element.value ?: return null
                is RespValue.SimpleString -> element.value
                else -> return null
            }
        }
    }
}
