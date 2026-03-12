# Building My Own URL Shortener: SHA-256 Hashing & REST API from Raw Sockets in Kotlin

> A URL shortener built from TCP sockets — SHA-256 hash-based code generation, base62 encoding, dual-map idempotent storage, REST API routing, and virtual thread concurrency.

## The Challenge

**Source:** [Coding Challenges - Build Your Own URL Shortener](https://codingchallenges.fyi/challenges/challenge-url-shortener)

URL shorteners are deceptively simple from the outside: paste a long URL, get a short one back. But under the surface, they raise real design questions — how do you generate short codes? How do you handle collisions? How do you make creation idempotent so the same URL always returns the same short link?

The challenge asks you to build a REST API that creates, resolves, and deletes short URLs. No framework, no database — just a TCP server, HTTP parsing, and in-memory storage. The focus is on the core algorithm (turning URLs into short codes) and the API design (routing, status codes, JSON responses).

## Usage

```bash
gig-urls [--port PORT] [--base-url URL]
```

| Flag | Default | Description |
|------|---------|-------------|
| `--port` | `8080` | Port to listen on |
| `--base-url` | `http://localhost:8080` | Base URL for generated short links |

```bash
# Start with defaults
gig-urls

# Custom port
gig-urls --port 3000

# Create a short URL
curl -s -X POST http://localhost:8080/ -d '{"url":"https://example.com"}'

# Follow a short URL (302 redirect)
curl -i http://localhost:8080/50D8vZsq

# Delete a short URL
curl -i -X DELETE http://localhost:8080/50D8vZsq
```

Stop the server with `Ctrl+C`. If the port is still in use after stopping:

```bash
lsof -ti:8080 | xargs kill
```

## Approach

Six components, each handling one concern:

1. **CodeGenerator** — SHA-256 hashing with base62 encoding to produce 8-character codes
2. **UrlStore** — dual ConcurrentHashMap for O(1) lookups in both directions
3. **ApiHandler** — REST routing (POST create, GET redirect, DELETE remove)
4. **HttpRequest / HttpResponse** — data classes for parsed requests and formatted responses
5. **RequestParser** — reads raw bytes into an HttpRequest, including body via Content-Length
6. **UrlServer** — TCP accept loop with virtual thread per client

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Code generation | SHA-256 → base62 (8 chars) | Deterministic, uniform distribution, no counter state to persist |
| Idempotency | Dual-map lookup (URL → code) before generating | Same URL always returns same short link; O(1) check |
| Redirect status | 302 Found (not 301) | Preserves server-side analytics; browser doesn't cache permanently |
| Storage | In-memory ConcurrentHashMap | Challenge scope; thread-safe without explicit locking |
| Concurrency | Virtual threads (Java 21) | Same pattern as web server and Redis challenges |
| JSON parsing | Regex extraction | No dependency needed for single-field extraction |

## Code Generation: SHA-256 to Base62

The core problem: given an arbitrary URL, produce a short, unique string. Random generation works but isn't idempotent — the same URL would get different codes each time. Instead, hashing the URL produces a deterministic output:

```kotlin
fun generate(url: String, attempt: Int = 0): String {
    val input = if (attempt == 0) url else "$url#$attempt"
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(input.toByteArray())

    var value = 0L
    for (i in 0 until 6) {
        value = (value shl 8) or (hash[i].toLong() and 0xFF)
    }

    return toBase62(value, 8)
}
```

The algorithm takes the first 6 bytes (48 bits) of the SHA-256 hash and encodes them as 8 base62 characters. Base62 uses `0-9`, `A-Z`, `a-z` — 62 characters that are all URL-safe without encoding.

Why 48 bits into 8 characters? 62^8 ≈ 218 trillion possible codes, but we're only using 2^48 ≈ 281 trillion possible hash values. The math works out: 48 bits of entropy maps cleanly into 8 base62 digits with no wasted space.

The `attempt` parameter handles collisions. If two different URLs hash to the same code, appending `#1`, `#2`, etc. to the input produces a completely different hash. In practice, collisions are astronomically rare at this scale, but the retry loop ensures correctness:

```kotlin
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
```

## Idempotent Storage with Dual Maps

A URL shortener should return the same short link when given the same URL twice. This requires looking up URLs in both directions — code-to-URL for redirects, and URL-to-code for idempotency:

```kotlin
class UrlStore {
    private val codeToUrl = ConcurrentHashMap<String, String>()
    private val urlToCode = ConcurrentHashMap<String, String>()

    fun create(code: String, url: String): Boolean {
        val existing = codeToUrl.putIfAbsent(code, url)
        if (existing != null) return false
        urlToCode[url] = code
        return true
    }

    fun getUrl(code: String): String? = codeToUrl[code]
    fun getCode(url: String): String? = urlToCode[url]

    fun delete(code: String): Boolean {
        val url = codeToUrl.remove(code) ?: return false
        urlToCode.remove(url)
        return true
    }
}
```

`ConcurrentHashMap` provides thread-safe operations without explicit locking. `putIfAbsent` is atomic — if two threads try to create the same code simultaneously, only one succeeds. The other gets `false` and retries with a different attempt number.

The idempotency check happens before code generation in the handler:

```kotlin
val existingCode = store.getCode(url)
if (existingCode != null) {
    return buildCreateResponse(existingCode, url)
}
```

This means repeated POSTs with the same URL are O(1) lookups, not rehashing operations.

## REST API Routing

The API has three endpoints, distinguished by HTTP method and path:

```kotlin
fun handle(request: HttpRequest): HttpResponse {
    return when {
        request.method == "POST" && request.path == "/" -> handleCreate(request)
        request.method == "GET" && request.path.length > 1 -> handleRedirect(request)
        request.method == "DELETE" && request.path.length > 1 -> handleDelete(request)
        else -> HttpResponse.error(404, "Not Found", "Not Found")
    }
}
```

Each endpoint returns appropriate HTTP status codes: 201 Created for new short URLs, 302 Found for redirects, 200 OK for deletes, 400 Bad Request for invalid input, 404 Not Found for unknown codes.

The redirect uses 302 (temporary) rather than 301 (permanent). With 301, browsers cache the redirect and never hit the server again — which means you can't track clicks, can't update the target URL, and can't delete the mapping. 302 forces the browser to check with the server every time, keeping control server-side.

## Reading Request Bodies

Unlike the web server challenge (GET only), a URL shortener needs to read POST bodies. The parser uses `Content-Length` to know how many bytes to read after the headers:

```kotlin
val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
val body = if (contentLength > 0) {
    val chars = CharArray(contentLength)
    var read = 0
    while (read < contentLength) {
        val n = reader.read(chars, read, contentLength - read)
        if (n == -1) break
        read += n
    }
    String(chars, 0, read)
} else {
    ""
}
```

The read loop is important — TCP doesn't guarantee all data arrives in one `read()` call. A large body might arrive in multiple packets, so the loop continues until it has read exactly `Content-Length` bytes or the stream ends.

## What I Learned

**Deterministic hashing beats random generation for idempotency.** Random code generation is simpler but makes idempotency expensive — you'd need to look up every URL before generating. Hashing the URL means the same input always produces the same code, and the idempotency check is just a fast-path optimization.

**Dual maps are the natural structure for bidirectional lookups.** A single map from code to URL handles redirects but makes "does this URL already exist?" an O(n) scan. Maintaining both directions costs 2x memory but gives O(1) for every operation.

**302 vs 301 is a real design decision, not just a number.** 301 Permanent Redirect tells browsers to cache the redirect forever. For a URL shortener, that means losing the ability to track, update, or delete short URLs. 302 keeps the server in control at the cost of one extra round-trip per redirect.

**ConcurrentHashMap's atomic operations simplify concurrent code.** `putIfAbsent` handles the "check and insert" race condition in one call. Without it, you'd need explicit synchronization around the create path to prevent two threads from inserting different URLs under the same code.

**Collision handling is easy to add but almost never triggers.** With 2^48 possible hash values, the probability of collision for a small URL set is negligible. But the retry mechanism (appending `#attempt` to change the hash input) costs almost nothing to implement and guarantees correctness at any scale.
