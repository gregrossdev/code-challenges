package dev.gregross.challenges.irc

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class MockIrcServer : AutoCloseable {
    private val serverSocket = ServerSocket(0)
    val port: Int = serverSocket.localPort
    val receivedCommands = mutableListOf<String>()

    private var clientSocket: Socket? = null
    private var clientReader: BufferedReader? = null
    private var clientWriter: PrintWriter? = null
    private var acceptThread: Thread? = null

    @Volatile
    var clientConnected = false
        private set

    fun start() {
        acceptThread = Thread.startVirtualThread {
            try {
                val socket = serverSocket.accept()
                clientSocket = socket
                clientReader = BufferedReader(InputStreamReader(socket.getInputStream()))
                clientWriter = PrintWriter(socket.getOutputStream(), true)
                clientConnected = true
            } catch (_: Exception) {}
        }
    }

    fun waitForClient(timeoutMs: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!clientConnected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        if (!clientConnected) throw AssertionError("Client did not connect within ${timeoutMs}ms")
    }

    fun readCommand(): String? {
        val line = clientReader?.readLine() ?: return null
        receivedCommands.add(line)
        return line
    }

    fun sendToClient(message: String) {
        clientWriter?.println(message)
        clientWriter?.flush()
    }

    fun sendWelcome(nick: String) {
        sendToClient(":testserver 001 $nick :Welcome to the Test IRC Network")
        sendToClient(":testserver 002 $nick :Your host is testserver")
        sendToClient(":testserver 003 $nick :This server was created today")
        sendToClient(":testserver 004 $nick testserver 1.0 o o")
    }

    fun sendPing(token: String = "testserver") {
        sendToClient("PING :$token")
    }

    fun sendJoinConfirmation(nick: String, channel: String) {
        sendToClient(":$nick!$nick@localhost JOIN $channel")
        sendToClient(":testserver 353 $nick = $channel :$nick")
        sendToClient(":testserver 366 $nick $channel :End of /NAMES list")
    }

    fun sendPrivmsg(fromNick: String, channel: String, text: String) {
        sendToClient(":$fromNick!$fromNick@localhost PRIVMSG $channel :$text")
    }

    override fun close() {
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket.close() } catch (_: Exception) {}
    }
}
