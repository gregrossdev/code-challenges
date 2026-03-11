package dev.gregross.challenges.lb

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthCheckerTest {

    @Test fun `marks running backend as healthy`() {
        val testBackend = TestBackend(19100, "health-test")
        testBackend.start()

        val backend = Backend("http://localhost:19100")
        val checker = HealthChecker(listOf(backend), intervalSeconds = 60)

        try {
            checker.checkAll()
            assertTrue(backend.healthy)
        } finally {
            checker.stop()
            testBackend.stop()
        }
    }

    @Test fun `marks stopped backend as unhealthy`() {
        val backend = Backend("http://localhost:19101") // Nothing running on this port
        val checker = HealthChecker(listOf(backend), intervalSeconds = 60)

        try {
            checker.checkAll()
            assertFalse(backend.healthy)
        } finally {
            checker.stop()
        }
    }

    @Test fun `recovers backend when it comes back`() {
        val backend = Backend("http://localhost:19102")
        val checker = HealthChecker(listOf(backend), intervalSeconds = 60)

        // Initially down
        checker.checkAll()
        assertFalse(backend.healthy)

        // Start the backend
        val testBackend = TestBackend(19102, "recovery-test")
        testBackend.start()
        Thread.sleep(100)

        try {
            checker.checkAll()
            assertTrue(backend.healthy)
        } finally {
            checker.stop()
            testBackend.stop()
        }
    }

    @Test fun `checks multiple backends independently`() {
        val testBackend = TestBackend(19103, "multi-test")
        testBackend.start()

        val healthy = Backend("http://localhost:19103")
        val unhealthy = Backend("http://localhost:19104") // Nothing here
        val checker = HealthChecker(listOf(healthy, unhealthy), intervalSeconds = 60)

        try {
            checker.checkAll()
            assertTrue(healthy.healthy)
            assertFalse(unhealthy.healthy)
        } finally {
            checker.stop()
            testBackend.stop()
        }
    }
}
