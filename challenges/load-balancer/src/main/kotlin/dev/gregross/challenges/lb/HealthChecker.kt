package dev.gregross.challenges.lb

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HealthChecker(
    private val backends: List<Backend>,
    private val intervalSeconds: Long = 10,
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()
    private var scheduler: ScheduledExecutorService? = null

    fun start() {
        // Initial check
        checkAll()

        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
        scheduler?.scheduleAtFixedRate(
            { checkAll() },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS,
        )
    }

    fun stop() {
        scheduler?.shutdown()
    }

    fun checkAll() {
        for (backend in backends) {
            val wasHealthy = backend.healthy
            backend.healthy = checkBackend(backend)
            if (wasHealthy && !backend.healthy) {
                println("Backend ${backend.url} is now UNHEALTHY")
            } else if (!wasHealthy && backend.healthy) {
                println("Backend ${backend.url} is now HEALTHY")
            }
        }
    }

    private fun checkBackend(backend: Backend): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${backend.url}/"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            response.statusCode() == 200
        } catch (_: Exception) {
            false
        }
    }
}
