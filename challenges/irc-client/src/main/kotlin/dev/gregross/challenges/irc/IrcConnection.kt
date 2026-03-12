package dev.gregross.challenges.irc

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class IrcConnection(
    private val host: String,
    private val port: Int,
) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var readerThread: Thread? = null
    private var messageHandler: ((IrcMessage) -> Unit)? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun onMessage(handler: (IrcMessage) -> Unit) {
        messageHandler = handler
    }

    fun connect() {
        val s = Socket(host, port)
        socket = s
        reader = BufferedReader(InputStreamReader(s.getInputStream()))
        writer = PrintWriter(s.getOutputStream(), true)
        isConnected = true
    }

    fun startReading() {
        readerThread = Thread.startVirtualThread {
            try {
                val r = reader ?: return@startVirtualThread
                while (isConnected) {
                    val line = r.readLine() ?: break
                    val msg = IrcParser.parse(line)
                    if (msg.command == "PING") {
                        send(IrcParser.formatPong(msg.params.firstOrNull() ?: ""))
                    } else {
                        messageHandler?.invoke(msg)
                    }
                }
            } catch (_: Exception) {
                // Socket closed or read error
            } finally {
                isConnected = false
            }
        }
    }

    @Synchronized
    fun send(message: String) {
        writer?.print(message)
        writer?.flush()
    }

    fun disconnect() {
        isConnected = false
        try { socket?.close() } catch (_: Exception) {}
        readerThread?.interrupt()
    }
}
