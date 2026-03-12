package dev.gregross.challenges.urls

import java.net.ServerSocket
import java.net.Socket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private lateinit var server: UrlServer
    private lateinit var serverThread: Thread
    private var port: Int = 0

    @BeforeTest fun setUp() {
        port = ServerSocket(0).use { it.localPort }
        server = UrlServer(port, "http://localhost:$port")
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
            String(socket.getInputStream().readBytes())
        }
    }

    private fun post(body: String): String {
        val request = "POST / HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n\r\n$body"
        return sendRequest(request)
    }

    private fun get(path: String): String {
        return sendRequest("GET $path HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }

    private fun delete(path: String): String {
        return sendRequest("DELETE $path HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }

    private fun extractKey(response: String): String {
        return """"key":"([^"]+)"""".toRegex().find(response)!!.groupValues[1]
    }

    @Test fun `create url returns 201`() {
        val response = post("""{"url":"https://example.com"}""")
        assertTrue(response.startsWith("HTTP/1.1 201 Created"))
        assertTrue(response.contains("\"key\""))
        assertTrue(response.contains("\"long_url\":\"https://example.com\""))
        assertTrue(response.contains("\"short_url\":\"http://localhost:$port/"))
    }

    @Test fun `create is idempotent over tcp`() {
        val r1 = post("""{"url":"https://idempotent.com"}""")
        val r2 = post("""{"url":"https://idempotent.com"}""")
        val key1 = extractKey(r1)
        val key2 = extractKey(r2)
        assertEquals(key1, key2)
    }

    @Test fun `missing url returns 400`() {
        val response = post("""{"other":"value"}""")
        assertTrue(response.startsWith("HTTP/1.1 400 Bad Request"))
    }

    @Test fun `redirect returns 302`() {
        val createResponse = post("""{"url":"https://redirect-test.com"}""")
        val key = extractKey(createResponse)

        val response = get("/$key")
        assertTrue(response.startsWith("HTTP/1.1 302 Found"))
        assertTrue(response.contains("Location: https://redirect-test.com"))
    }

    @Test fun `unknown code returns 404`() {
        val response = get("/nonexistent")
        assertTrue(response.startsWith("HTTP/1.1 404 Not Found"))
    }

    @Test fun `delete returns 200`() {
        val createResponse = post("""{"url":"https://delete-test.com"}""")
        val key = extractKey(createResponse)

        val response = delete("/$key")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
    }

    @Test fun `delete unknown returns 200`() {
        val response = delete("/nonexistent")
        assertTrue(response.startsWith("HTTP/1.1 200 OK"))
    }

    @Test fun `redirect after delete returns 404`() {
        val createResponse = post("""{"url":"https://delete-then-get.com"}""")
        val key = extractKey(createResponse)

        delete("/$key")
        val response = get("/$key")
        assertTrue(response.startsWith("HTTP/1.1 404 Not Found"))
    }

    @Test fun `concurrent creates succeed`() {
        val threads = (1..10).map { i ->
            Thread {
                val response = post("""{"url":"https://concurrent-$i.com"}""")
                assertTrue(response.startsWith("HTTP/1.1 201 Created"))
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }
    }
}
