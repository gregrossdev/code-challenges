package dev.gregross.challenges.urls

class ApiHandler(
    private val store: UrlStore,
    private val generator: CodeGenerator,
    private val baseUrl: String,
) {

    fun handle(request: HttpRequest): HttpResponse {
        return when {
            request.method == "POST" && request.path == "/" -> handleCreate(request)
            request.method == "GET" && request.path.length > 1 -> handleRedirect(request)
            request.method == "DELETE" && request.path.length > 1 -> handleDelete(request)
            else -> HttpResponse.error(404, "Not Found", "Not Found")
        }
    }

    private fun handleCreate(request: HttpRequest): HttpResponse {
        val url = extractUrl(request.body)
            ?: return HttpResponse.error(400, "Bad Request", "Missing or invalid 'url' field")

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return HttpResponse.error(400, "Bad Request", "URL must start with http:// or https://")
        }

        // Idempotency: check if URL already exists
        val existingCode = store.getCode(url)
        if (existingCode != null) {
            return buildCreateResponse(existingCode, url)
        }

        // Generate code with collision handling
        var attempt = 0
        while (true) {
            val code = generator.generate(url, attempt)
            if (store.create(code, url)) {
                return buildCreateResponse(code, url)
            }
            attempt++
            if (attempt > 10) {
                return HttpResponse.error(500, "Internal Server Error", "Failed to generate unique code")
            }
        }
    }

    private fun handleRedirect(request: HttpRequest): HttpResponse {
        val code = request.path.removePrefix("/")
        val url = store.getUrl(code)
            ?: return HttpResponse.error(404, "Not Found", "Short URL not found")
        return HttpResponse.redirect(url)
    }

    private fun handleDelete(request: HttpRequest): HttpResponse {
        val code = request.path.removePrefix("/")
        store.delete(code)
        return HttpResponse.json(200, "OK", """{"status":"ok"}""")
    }

    private fun buildCreateResponse(code: String, url: String): HttpResponse {
        val shortUrl = "$baseUrl/$code"
        val json = """{"key":"$code","long_url":"$url","short_url":"$shortUrl"}"""
        return HttpResponse.json(201, "Created", json)
    }

    private fun extractUrl(body: String): String? {
        // Minimal JSON parsing: extract "url" field value
        val pattern = """"url"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(body)?.groupValues?.get(1)
    }
}
