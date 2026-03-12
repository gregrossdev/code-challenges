package dev.gregross.challenges.httpd

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticFileHandlerTest {

    private val docRoot = File(javaClass.classLoader.getResource("www")!!.toURI())
    private val handler = StaticFileHandler(docRoot)

    private fun get(path: String): HttpResponse {
        return handler.handle(HttpRequest("GET", path, "HTTP/1.1"))
    }

    @Test fun `serve index html`() {
        val response = get("/index.html")
        assertEquals(200, response.statusCode)
        assertTrue(String(response.body).contains("Hello World"))
        assertEquals("text/html", response.headers["Content-Type"])
    }

    @Test fun `default document for root path`() {
        val response = get("/")
        assertEquals(200, response.statusCode)
        assertTrue(String(response.body).contains("Hello World"))
    }

    @Test fun `serve css with correct mime type`() {
        val response = get("/style.css")
        assertEquals(200, response.statusCode)
        assertEquals("text/css", response.headers["Content-Type"])
    }

    @Test fun `serve file in subdirectory`() {
        val response = get("/sub/page.html")
        assertEquals(200, response.statusCode)
        assertTrue(String(response.body).contains("Sub Page"))
    }

    @Test fun `404 for missing file`() {
        val response = get("/missing.html")
        assertEquals(404, response.statusCode)
    }

    @Test fun `path traversal blocked with dotdot`() {
        val response = get("/../../../etc/passwd")
        assertEquals(403, response.statusCode)
    }

    @Test fun `path traversal blocked with encoded dotdot`() {
        val response = get("/sub/../../etc/passwd")
        assertEquals(403, response.statusCode)
    }

    @Test fun `405 for non-GET method`() {
        val response = handler.handle(HttpRequest("POST", "/", "HTTP/1.1"))
        assertEquals(405, response.statusCode)
    }

    @Test fun `405 for PUT method`() {
        val response = handler.handle(HttpRequest("PUT", "/index.html", "HTTP/1.1"))
        assertEquals(405, response.statusCode)
    }

    @Test fun `content length header matches body size`() {
        val response = get("/index.html")
        assertEquals(response.body.size.toString(), response.headers["Content-Length"])
    }

    @Test fun `connection close header present`() {
        val response = get("/index.html")
        assertEquals("close", response.headers["Connection"])
    }

    @Test fun `server header present`() {
        val response = get("/index.html")
        assertEquals("gig-httpd", response.headers["Server"])
    }
}
