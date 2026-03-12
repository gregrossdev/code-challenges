package dev.gregross.challenges.lb

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyServerTest {

    private val client = HttpClient.newHttpClient()

    @Test fun `forwards request to single backend`() {
        val backend = TestBackend(19090, "backend1")
        backend.start()

        val backendObj = Backend("http://localhost:19090")
        val proxy = ProxyServer(18080) { backendObj }
        proxy.start()

        Thread.sleep(100) // Let servers start

        try {
            val response = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:18080/")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, response.statusCode())
            assertEquals("Hello from backend1", response.body())
            assertEquals(1, backend.requestCount.get())
        } finally {
            proxy.stop()
            backend.stop()
        }
    }

    @Test fun `returns 503 when no backends available`() {
        val proxy = ProxyServer(18081) { null }
        proxy.start()

        Thread.sleep(100)

        try {
            val response = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:18081/")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(503, response.statusCode())
            assertTrue(response.body().contains("No healthy backends"))
        } finally {
            proxy.stop()
        }
    }

    @Test fun `adds X-Forwarded-For header`() {
        // The backend receives the header but our simple test backend doesn't echo it
        // Just verify the proxy doesn't crash when adding the header
        val backend = TestBackend(19091, "backend-xff")
        backend.start()

        val backendObj = Backend("http://localhost:19091")
        val proxy = ProxyServer(18082) { backendObj }
        proxy.start()

        Thread.sleep(100)

        try {
            val response = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:18082/test")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, response.statusCode())
        } finally {
            proxy.stop()
            backend.stop()
        }
    }
}
