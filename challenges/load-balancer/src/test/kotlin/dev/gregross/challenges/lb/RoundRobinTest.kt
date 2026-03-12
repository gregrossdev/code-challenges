package dev.gregross.challenges.lb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundRobinTest {

    @Test fun `cycles through backends in order`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"), Backend("http://c"))
        val rr = RoundRobin(backends)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://b", rr.next()?.url)
        assertEquals("http://c", rr.next()?.url)
    }

    @Test fun `wraps around after last backend`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"))
        val rr = RoundRobin(backends)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://b", rr.next()?.url)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://b", rr.next()?.url)
    }

    @Test fun `skips unhealthy backends`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"), Backend("http://c"))
        backends[1].healthy = false
        val rr = RoundRobin(backends)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://c", rr.next()?.url)
        assertEquals("http://a", rr.next()?.url)
    }

    @Test fun `returns null when all unhealthy`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"))
        backends.forEach { it.healthy = false }
        val rr = RoundRobin(backends)
        assertNull(rr.next())
    }

    @Test fun `handles single backend`() {
        val backends = listOf(Backend("http://a"))
        val rr = RoundRobin(backends)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://a", rr.next()?.url)
    }

    @Test fun `re-includes recovered backend`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"))
        backends[1].healthy = false
        val rr = RoundRobin(backends)
        assertEquals("http://a", rr.next()?.url)
        assertEquals("http://a", rr.next()?.url)

        backends[1].healthy = true
        // Now both should be in rotation
        val results = (1..4).map { rr.next()?.url }
        assert(results.contains("http://a"))
        assert(results.contains("http://b"))
    }

    @Test fun `distributes evenly across 3 backends with 6 requests`() {
        val backends = listOf(Backend("http://a"), Backend("http://b"), Backend("http://c"))
        val rr = RoundRobin(backends)
        val counts = mutableMapOf<String, Int>()
        repeat(6) {
            val url = rr.next()!!.url
            counts[url] = (counts[url] ?: 0) + 1
        }
        assertEquals(2, counts["http://a"])
        assertEquals(2, counts["http://b"])
        assertEquals(2, counts["http://c"])
    }
}
