package dev.gregross.challenges.httpd

import java.io.File

class StaticFileHandler(private val docRoot: File) {

    private val mimeTypes = mapOf(
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "text/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "ico" to "image/x-icon",
        "txt" to "text/plain",
    )

    fun handle(request: HttpRequest): HttpResponse {
        if (request.method != "GET") {
            return HttpResponse.METHOD_NOT_ALLOWED
        }

        val path = if (request.path == "/") "/index.html" else request.path

        if (path.contains('\u0000')) {
            return HttpResponse.FORBIDDEN
        }

        val requestedFile = File(docRoot, path)
        val canonicalRoot = docRoot.canonicalPath
        val canonicalFile = requestedFile.canonicalPath

        if (!canonicalFile.startsWith(canonicalRoot + File.separator) && canonicalFile != canonicalRoot) {
            return HttpResponse.FORBIDDEN
        }

        if (!requestedFile.exists() || !requestedFile.isFile) {
            return HttpResponse.NOT_FOUND
        }

        val body = requestedFile.readBytes()
        val contentType = mimeTypeFor(requestedFile.name)
        return HttpResponse.ok(body, contentType)
    }

    private fun mimeTypeFor(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return mimeTypes[ext] ?: "application/octet-stream"
    }
}
