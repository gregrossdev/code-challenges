package dev.gregross.challenges.httpd

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestParserTest {

    private val parser = RequestParser()

    private fun parse(raw: String): HttpRequest? {
        return parser.parse(ByteArrayInputStream(raw.toByteArray()))
    }

    @Test fun `parse valid GET request`() {
        val request = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
        assertNotNull(request)
        assertEquals("GET", request.method)
        assertEquals("/", request.path)
        assertEquals("HTTP/1.1", request.version)
        assertEquals("localhost", request.headers["host"])
    }

    @Test fun `parse GET with path`() {
        val request = parse("GET /index.html HTTP/1.1\r\n\r\n")
        assertNotNull(request)
        assertEquals("/index.html", request.path)
    }

    @Test fun `parse request with multiple headers`() {
        val raw = "GET / HTTP/1.1\r\nHost: localhost\r\nAccept: text/html\r\nUser-Agent: test\r\n\r\n"
        val request = parse(raw)
        assertNotNull(request)
        assertEquals(3, request.headers.size)
        assertEquals("text/html", request.headers["accept"])
        assertEquals("test", request.headers["user-agent"])
    }

    @Test fun `empty input returns null`() {
        assertNull(parse(""))
    }

    @Test fun `malformed request line returns null`() {
        assertNull(parse("GARBAGE\r\n\r\n"))
    }

    @Test fun `missing HTTP version returns null`() {
        assertNull(parse("GET /\r\n\r\n"))
    }

    @Test fun `invalid protocol returns null`() {
        assertNull(parse("GET / FTP/1.0\r\n\r\n"))
    }

    @Test fun `POST method parsed correctly`() {
        val request = parse("POST /api/data HTTP/1.1\r\n\r\n")
        assertNotNull(request)
        assertEquals("POST", request.method)
        assertEquals("/api/data", request.path)
    }

    @Test fun `PUT method parsed correctly`() {
        val request = parse("PUT /resource HTTP/1.1\r\n\r\n")
        assertNotNull(request)
        assertEquals("PUT", request.method)
    }

    @Test fun `deep path parsed correctly`() {
        val request = parse("GET /a/b/c/d.html HTTP/1.1\r\n\r\n")
        assertNotNull(request)
        assertEquals("/a/b/c/d.html", request.path)
    }

    @Test fun `header names are lowercased`() {
        val request = parse("GET / HTTP/1.1\r\nContent-Type: text/html\r\n\r\n")
        assertNotNull(request)
        assertEquals("text/html", request.headers["content-type"])
    }
}
