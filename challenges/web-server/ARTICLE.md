# Building My Own Web Server: HTTP/1.1 from Raw Sockets in Kotlin

> An HTTP/1.1 web server built from TCP sockets — request parsing, static file serving with MIME types, virtual thread concurrency, and path traversal defense.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Web Server](https://codingchallenges.fyi/challenges/challenge-web-server)

Every web developer uses HTTP daily, but few have implemented the protocol from scratch. The challenge walks through building a web server step by step: accepting TCP connections, parsing HTTP requests, serving static files, handling concurrent clients, and preventing path traversal attacks.

The goal isn't to build a production web server — it's to understand what happens between a browser sending a request and receiving a response.

## Usage

```bash
gig-httpd [--port PORT] [--docroot PATH]
```

| Flag | Default | Description |
|------|---------|-------------|
| `--port` | `8080` | Port to listen on |
| `--docroot` | `./www` | Directory to serve files from |

```bash
# Serve the ./www directory on port 8080
gig-httpd --docroot www

# Custom port
gig-httpd --port 3000 --docroot /var/www/html

# Test with curl
curl -i http://localhost:8080/
curl -i http://localhost:8080/index.html
```

Stop the server with `Ctrl+C`.

## Approach

Five components, each handling one concern:

1. **HttpRequest** — data class for parsed requests (method, path, version, headers)
2. **HttpResponse** — data class for responses with `writeTo(OutputStream)`
3. **RequestParser** — reads raw bytes into an HttpRequest
4. **StaticFileHandler** — resolves paths to files, serves content with MIME types
5. **WebServer** — TCP accept loop with virtual thread per client

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Protocol | HTTP/1.1, GET only, Connection: close | Challenge scope; close-after-response simplifies without losing value |
| Concurrency | Virtual threads (Java 21) | Proven in Redis challenge; one thread per connection with no thread pool tuning |
| Security | Canonical path prefix check | Standard traversal defense; challenge step 4 requirement |
| MIME types | Extension-based lookup (9 types) | Simple, covers common static assets |
| Port | 8080 default | Avoids root privileges needed for port 80 |

## HTTP Request Parsing

An HTTP request is text over TCP. The format:

```
GET /index.html HTTP/1.1\r\n
Host: localhost\r\n
Accept: text/html\r\n
\r\n
```

Three parts: the request line (method, path, version), headers (name-value pairs), and a blank line marking the end of headers. The parser reads line by line:

```kotlin
fun parse(reader: BufferedReader): HttpRequest? {
    val requestLine = reader.readLine() ?: return null
    val parts = requestLine.split(" ")
    if (parts.size != 3) return null

    val method = parts[0]
    val path = parts[1]
    val version = parts[2]

    if (!version.startsWith("HTTP/")) return null

    val headers = mutableMapOf<String, String>()
    while (true) {
        val line = reader.readLine() ?: break
        if (line.isEmpty()) break
        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
            val name = line.substring(0, colonIndex).trim().lowercase()
            val value = line.substring(colonIndex + 1).trim()
            headers[name] = value
        }
    }

    return HttpRequest(method, path, version, headers)
}
```

Returning `null` for malformed input means the server can respond with 400 Bad Request without crashing. Header names are lowercased for case-insensitive lookup (HTTP headers are case-insensitive per spec).

## HTTP Response Formatting

The response mirrors the request format:

```
HTTP/1.1 200 OK\r\n
Content-Type: text/html\r\n
Content-Length: 169\r\n
Connection: close\r\n
Server: gig-httpd\r\n
\r\n
<html>...</html>
```

Status line, headers, blank line, body. The implementation writes headers as text and the body as raw bytes — this distinction matters for binary files like images:

```kotlin
fun writeTo(output: OutputStream) {
    val headerBlock = buildString {
        append("HTTP/1.1 $statusCode $reason\r\n")
        for ((name, value) in headers) {
            append("$name: $value\r\n")
        }
        append("\r\n")
    }
    output.write(headerBlock.toByteArray())
    if (body.isNotEmpty()) {
        output.write(body)
    }
    output.flush()
}
```

`Content-Length` must be the byte count of the body, not the character count. For ASCII text they're the same, but for multi-byte UTF-8 or binary files they differ. Reading files as `ByteArray` and using `body.size` ensures correctness.

## Static File Serving

The handler maps URL paths to files on disk:

1. `/` maps to `/index.html` (default document)
2. Resolve path against the document root
3. Check for path traversal (security)
4. Read the file as bytes
5. Determine MIME type from extension
6. Build a 200 response

```kotlin
fun handle(request: HttpRequest): HttpResponse {
    if (request.method != "GET") return HttpResponse.METHOD_NOT_ALLOWED

    val path = if (request.path == "/") "/index.html" else request.path
    val requestedFile = File(docRoot, path)

    // Security check
    val canonicalFile = requestedFile.canonicalPath
    if (!canonicalFile.startsWith(docRoot.canonicalPath + File.separator)) {
        return HttpResponse.FORBIDDEN
    }

    if (!requestedFile.exists() || !requestedFile.isFile) {
        return HttpResponse.NOT_FOUND
    }

    val body = requestedFile.readBytes()
    val contentType = mimeTypeFor(requestedFile.name)
    return HttpResponse.ok(body, contentType)
}
```

Five possible responses: 200 (file found), 400 (bad request), 403 (traversal blocked), 404 (not found), 405 (wrong method). Each maps to an HTTP status code that tells the client exactly what happened.

### MIME Type Detection

Browsers need the `Content-Type` header to know how to render content. An extension-based lookup covers the common cases:

| Extension | Content-Type |
|-----------|-------------|
| `.html` | `text/html` |
| `.css` | `text/css` |
| `.js` | `text/javascript` |
| `.json` | `application/json` |
| `.png` | `image/png` |
| `.jpg` | `image/jpeg` |
| `.txt` | `text/plain` |

Anything unrecognized falls back to `application/octet-stream`, which tells the browser to treat it as a generic binary download.

## Path Traversal Defense

The most important security consideration for a file-serving web server. A request for `GET /../../../etc/passwd` attempts to escape the document root and read arbitrary files from the system.

The defense uses Java's canonical path resolution:

```kotlin
val canonicalRoot = docRoot.canonicalPath
val canonicalFile = requestedFile.canonicalPath

if (!canonicalFile.startsWith(canonicalRoot + File.separator)) {
    return HttpResponse.FORBIDDEN
}
```

`canonicalPath` resolves `..`, `.`, and symlinks to an absolute path. If the resolved path doesn't start with the document root, the request is trying to escape — return 403 Forbidden.

This handles all traversal variants: `/../`, `/./../../`, symlink chains, and encoded sequences. The check happens before any file read, so even attempting traversal can't leak information.

## Virtual Thread Concurrency

The server needs to handle multiple clients simultaneously. A sequential server would block all other clients while serving one request. Virtual threads (Java 21) make this trivial:

```kotlin
while (running) {
    val client = serverSocket.accept()
    Thread.startVirtualThread { handleClient(client) }
}
```

Each connection gets its own virtual thread. Unlike platform threads, virtual threads are cheap — the JVM can run millions of them. No thread pool sizing, no executor configuration, no async callbacks. The code reads sequentially but runs concurrently.

This is the same pattern used in the Redis server challenge. Virtual threads are particularly well-suited to I/O-bound work like serving HTTP responses, where each thread spends most of its time waiting on socket reads and writes.

## What I Learned

**HTTP is just formatted text over TCP.** The protocol is surprisingly simple at its core — a text request line, text headers, a blank line, and a body. Everything a web browser does starts with this format.

**Security is a first-class concern, not an afterthought.** Path traversal is the obvious attack vector for any file-serving server. Canonical path resolution is a clean, reliable defense that handles all edge cases in one check.

**Content-Length must be bytes, not characters.** This distinction doesn't matter for ASCII but breaks binary files and multi-byte encodings. Reading files as `ByteArray` and using `body.size` is the correct approach.

**Virtual threads eliminate concurrency complexity.** The server handles concurrent requests with zero thread pool configuration. One line — `Thread.startVirtualThread` — turns a sequential server into a concurrent one.

**Connection: close simplifies everything.** HTTP/1.1 defaults to persistent connections (keep-alive), which means the server needs to detect request boundaries and manage connection lifecycle. Sending `Connection: close` opts out of this complexity while still being fully HTTP/1.1 compliant.
