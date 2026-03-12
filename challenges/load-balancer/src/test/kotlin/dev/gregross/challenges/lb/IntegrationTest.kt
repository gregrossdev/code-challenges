package dev.gregross.challenges.lb

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private val client = HttpClient.newHttpClient()

    @Test fun `round-robin distributes across 3 backends`() {
        val tb1 = TestBackend(19200, "server-A")
        val tb2 = TestBackend(19201, "server-B")
        val tb3 = TestBackend(19202, "server-C")
        tb1.start(); tb2.start(); tb3.start()

        val backends = listOf(
            Backend("http://localhost:19200"),
            Backend("http://localhost:19201"),
            Backend("http://localhost:19202"),
        )
        val rr = RoundRobin(backends)
        val proxy = ProxyServer(18200) { rr.next() }
        proxy.start()

        Thread.sleep(200)

        try {
            val responses = (1..6).map {
                client.send(
                    HttpRequest.newBuilder().uri(URI.create("http://localhost:18200/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString(),
                ).body()
            }

            // Each backend should get exactly 2 requests
            assertEquals(2, responses.count { it == "Hello from server-A" })
            assertEquals(2, responses.count { it == "Hello from server-B" })
            assertEquals(2, responses.count { it == "Hello from server-C" })
        } finally {
            proxy.stop()
            tb1.stop(); tb2.stop(); tb3.stop()
        }
    }

    @Test fun `failover when backend goes down`() {
        val tb1 = TestBackend(19210, "alive")
        val tb2 = TestBackend(19211, "will-die")
        tb1.start(); tb2.start()

        val backends = listOf(
            Backend("http://localhost:19210"),
            Backend("http://localhost:19211"),
        )
        val rr = RoundRobin(backends)
        val healthChecker = HealthChecker(backends, intervalSeconds = 60)
        val proxy = ProxyServer(18210) { rr.next() }

        healthChecker.start()
        proxy.start()
        Thread.sleep(200)

        try {
            // Both should work
            val r1 = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:18210/")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, r1.statusCode())

            // Kill backend 2
            tb2.stop()
            Thread.sleep(100)

            // Trigger health check
            healthChecker.checkAll()

            // All requests should go to alive backend
            val responses = (1..4).map {
                client.send(
                    HttpRequest.newBuilder().uri(URI.create("http://localhost:18210/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString(),
                ).body()
            }
            assertTrue(responses.all { it == "Hello from alive" })
        } finally {
            proxy.stop()
            healthChecker.stop()
            tb1.stop()
        }
    }

    @Test fun `handles concurrent requests`() {
        val tb1 = TestBackend(19220, "concurrent-A")
        val tb2 = TestBackend(19221, "concurrent-B")
        tb1.start(); tb2.start()

        val backends = listOf(
            Backend("http://localhost:19220"),
            Backend("http://localhost:19221"),
        )
        val rr = RoundRobin(backends)
        val proxy = ProxyServer(18220) { rr.next() }
        proxy.start()

        Thread.sleep(200)

        try {
            val executor = Executors.newVirtualThreadPerTaskExecutor()
            val futures = (1..10).map {
                executor.submit<String> {
                    client.send(
                        HttpRequest.newBuilder().uri(URI.create("http://localhost:18220/")).GET().build(),
                        HttpResponse.BodyHandlers.ofString(),
                    ).body()
                }
            }

            val results = futures.map { it.get() }
            assertEquals(10, results.size)
            assertTrue(results.all { it.startsWith("Hello from concurrent-") })
        } finally {
            proxy.stop()
            tb1.stop(); tb2.stop()
        }
    }

    @Test fun `returns 503 when all backends down`() {
        val backends = listOf(
            Backend("http://localhost:19230"),
            Backend("http://localhost:19231"),
        )
        backends.forEach { it.healthy = false }
        val rr = RoundRobin(backends)
        val proxy = ProxyServer(18230) { rr.next() }
        proxy.start()

        Thread.sleep(200)

        try {
            val response = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:18230/")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(503, response.statusCode())
        } finally {
            proxy.stop()
        }
    }
}
