package dev.gregross.challenges.httpd

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private lateinit var server: WebServer
    private lateinit var serverThread: Thread
    private var port: Int = 0

    @BeforeTest fun setUp() {
        port = ServerSocket(0).use { it.localPort }
        val docRoot = File(javaClass.classLoader.getResource("www")!!.toURI())
        server = WebServer(port, docRoot)
        serverThread = Thread { server.start() }
        serverThread.start()
        Thread.sleep(200)
    }

    @AfterTest fun tearDown() {
        server.stop()
        serverThread.join(2000)
    }

    private fun sendRequest(request: String): String {
        return Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 5000
            socket.getOutputStream().write(request.toByteArray())
            socket.getOutputStream().flush()
            socket.getInputStream().readBytes().let { String(it) }
        }
    }

    @Test fun `GET root returns 200 with index html`() {
        val response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
        assertTrue(response.contains("Hello World"))
        assertTrue(response.contains("Content-Type: text/html"))
    }

    @Test fun `GET index html returns 200`() {
        val response = sendRequest("GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
    }

    @Test fun `GET css returns correct content type`() {
        val response = sendRequest("GET /style.css HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
        assertTrue(response.contains("Content-Type: text/css"))
    }

    @Test fun `GET missing file returns 404`() {
        val response = sendRequest("GET /missing.html HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 404 Not Found"))
    }

    @Test fun `path traversal returns 403`() {
        val response = sendRequest("GET /../../../etc/passwd HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 403 Forbidden"))
    }

    @Test fun `POST method returns 405`() {
        val response = sendRequest("POST / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 405 Method Not Allowed"))
    }

    @Test fun `malformed request returns 400`() {
        val response = sendRequest("GARBAGE\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 400 Bad Request"))
    }

    @Test fun `subdirectory file served`() {
        val response = sendRequest("GET /sub/page.html HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
        assertTrue(response.contains("Sub Page"))
    }

    @Test fun `concurrent requests all succeed`() {
        val threads = (1..10).map {
            Thread {
                val response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
                assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }
    }

    @Test fun `response includes connection close`() {
        val response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.contains("Connection: close"))
    }

    @Test fun `response includes server header`() {
        val response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertTrue(response.contains("Server: gig-httpd"))
    }
}
