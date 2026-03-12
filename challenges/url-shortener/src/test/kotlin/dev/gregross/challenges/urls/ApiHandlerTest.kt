package dev.gregross.challenges.urls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiHandlerTest {

    private fun createHandler(): ApiHandler {
        return ApiHandler(UrlStore(), CodeGenerator(), "http://localhost:8080")
    }

    private fun post(handler: ApiHandler, body: String): HttpResponse {
        return handler.handle(HttpRequest("POST", "/", "HTTP/1.1", body = body))
    }

    private fun get(handler: ApiHandler, path: String): HttpResponse {
        return handler.handle(HttpRequest("GET", path, "HTTP/1.1"))
    }

    private fun delete(handler: ApiHandler, path: String): HttpResponse {
        return handler.handle(HttpRequest("DELETE", path, "HTTP/1.1"))
    }

    @Test fun `create returns 201 with JSON`() {
        val handler = createHandler()
        val response = post(handler, """{"url":"https://example.com"}""")
        assertEquals(201, response.statusCode)
        val body = String(response.body)
        assertTrue(body.contains("\"key\""))
        assertTrue(body.contains("\"long_url\":\"https://example.com\""))
        assertTrue(body.contains("\"short_url\":\"http://localhost:8080/"))
    }

    @Test fun `create is idempotent`() {
        val handler = createHandler()
        val r1 = post(handler, """{"url":"https://example.com"}""")
        val r2 = post(handler, """{"url":"https://example.com"}""")
        assertEquals(String(r1.body), String(r2.body))
    }

    @Test fun `create with missing url returns 400`() {
        val handler = createHandler()
        val response = post(handler, """{"other":"value"}""")
        assertEquals(400, response.statusCode)
    }

    @Test fun `create with empty body returns 400`() {
        val handler = createHandler()
        val response = post(handler, "")
        assertEquals(400, response.statusCode)
    }

    @Test fun `create with invalid url returns 400`() {
        val handler = createHandler()
        val response = post(handler, """{"url":"not-a-url"}""")
        assertEquals(400, response.statusCode)
    }

    @Test fun `redirect returns 302 with location`() {
        val handler = createHandler()
        val createResponse = post(handler, """{"url":"https://example.com"}""")
        val body = String(createResponse.body)
        val key = """"key":"([^"]+)"""".toRegex().find(body)!!.groupValues[1]

        val response = get(handler, "/$key")
        assertEquals(302, response.statusCode)
        assertEquals("https://example.com", response.headers["Location"])
    }

    @Test fun `redirect unknown code returns 404`() {
        val handler = createHandler()
        val response = get(handler, "/unknown")
        assertEquals(404, response.statusCode)
    }

    @Test fun `delete returns 200`() {
        val handler = createHandler()
        val createResponse = post(handler, """{"url":"https://example.com"}""")
        val body = String(createResponse.body)
        val key = """"key":"([^"]+)"""".toRegex().find(body)!!.groupValues[1]

        val response = delete(handler, "/$key")
        assertEquals(200, response.statusCode)
    }

    @Test fun `delete unknown returns 200 idempotent`() {
        val handler = createHandler()
        val response = delete(handler, "/unknown")
        assertEquals(200, response.statusCode)
    }

    @Test fun `redirect after delete returns 404`() {
        val handler = createHandler()
        val createResponse = post(handler, """{"url":"https://example.com"}""")
        val body = String(createResponse.body)
        val key = """"key":"([^"]+)"""".toRegex().find(body)!!.groupValues[1]

        delete(handler, "/$key")
        val response = get(handler, "/$key")
        assertEquals(404, response.statusCode)
    }

    @Test fun `different urls get different codes`() {
        val handler = createHandler()
        val r1 = post(handler, """{"url":"https://example.com"}""")
        val r2 = post(handler, """{"url":"https://other.com"}""")
        val key1 = """"key":"([^"]+)"""".toRegex().find(String(r1.body))!!.groupValues[1]
        val key2 = """"key":"([^"]+)"""".toRegex().find(String(r2.body))!!.groupValues[1]
        assertTrue(key1 != key2)
    }
}
