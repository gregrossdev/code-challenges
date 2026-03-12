package dev.gregross.challenges.lb

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpParsingTest {

    private fun requestFrom(raw: String): HttpRequest {
        return HttpRequest.parse(ByteArrayInputStream(raw.toByteArray()))
    }

    @Test fun `parses GET request line`() {
        val req = requestFrom("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertEquals("GET", req.method)
        assertEquals("/", req.path)
        assertEquals("HTTP/1.1", req.version)
    }

    @Test fun `parses path with segments`() {
        val req = requestFrom("GET /api/health HTTP/1.1\r\n\r\n")
        assertEquals("/api/health", req.path)
    }

    @Test fun `parses multiple headers`() {
        val raw = "GET / HTTP/1.1\r\nHost: localhost\r\nAccept: text/html\r\nUser-Agent: test\r\n\r\n"
        val req = requestFrom(raw)
        assertEquals("localhost", req.headers["Host"])
        assertEquals("text/html", req.headers["Accept"])
        assertEquals("test", req.headers["User-Agent"])
    }

    @Test fun `handles request with no headers`() {
        val req = requestFrom("GET / HTTP/1.1\r\n\r\n")
        assertEquals("GET", req.method)
        assertTrue(req.headers.isEmpty())
    }

    @Test fun `rejects empty input`() {
        assertFailsWith<IllegalArgumentException> {
            requestFrom("")
        }
    }

    @Test fun `rejects malformed request line`() {
        assertFailsWith<IllegalArgumentException> {
            requestFrom("INVALID\r\n\r\n")
        }
    }

    @Test fun `writes HTTP response`() {
        val response = HttpResponse(
            statusCode = 200,
            statusText = "OK",
            headers = mapOf("X-Custom" to "value"),
            body = "Hello".toByteArray(),
        )
        val out = ByteArrayOutputStream()
        response.writeTo(out)
        val result = out.toString()
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"))
        assertTrue(result.contains("X-Custom: value\r\n"))
        assertTrue(result.contains("Connection: close\r\n"))
        assertTrue(result.contains("Content-Length: 5\r\n"))
        assertTrue(result.endsWith("Hello"))
    }

    @Test fun `writes error response`() {
        val response = HttpResponse.error(503, "Service Unavailable", "No backends available")
        val out = ByteArrayOutputStream()
        response.writeTo(out)
        val result = out.toString()
        assertTrue(result.startsWith("HTTP/1.1 503 Service Unavailable\r\n"))
        assertTrue(result.endsWith("No backends available"))
    }
}
