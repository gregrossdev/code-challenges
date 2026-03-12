# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 11 — Web Server (v0.11.x)

> Build an HTTP/1.1 web server from raw TCP sockets — request parsing, static file serving with MIME types, virtual thread concurrency, path traversal protection, configurable document root and port. Deliver as `gig-httpd` native binary with article write-up.

**Decisions:** D-11.1, D-11.2, D-11.3, D-11.4, D-11.5, D-11.6, D-11.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 11.1 | `0.11.1` | Module scaffold, HttpRequest & RequestParser | in-session | done |
| 11.2 | `0.11.2` | HttpResponse & StaticFileHandler | in-session | done |
| 11.3 | `0.11.3` | WebServer, CLI & concurrency | in-session | done |
| 11.4 | `0.11.4` | Native image, integration tests & manual verification | in-session | done |
| 11.5 | `0.11.5` | Web server article write-up | in-session | done |

### Batch 11.1 — Module scaffold, HttpRequest & RequestParser

**Delegation:** in-session
**Decisions:** D-11.1, D-11.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:web-server`)
- `challenges/web-server/build.gradle.kts` (create)
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/HttpRequest.kt` (create)
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/RequestParser.kt` (create)
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/Main.kt` (create — stub)
- `challenges/web-server/src/test/kotlin/dev/gregross/challenges/httpd/RequestParserTest.kt` (create)

**Work:**
1. Add `challenges:web-server` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-httpd`.
3. Implement `HttpRequest(method: String, path: String, version: String, headers: Map<String, String>)` data class.
4. Implement `RequestParser`. Method `parse(input: InputStream): HttpRequest?` — reads request line, parses method/path/version, reads headers until blank line. Returns null for malformed or empty input.
5. Write tests: valid GET request, request with headers, missing request line (null), malformed request line, various paths, POST/PUT methods parsed correctly.

**Test criteria:**
- `./gradlew :challenges:web-server:test` — all parser tests pass.
- `RequestParser.parse(validGet)` returns correct method, path, version.
- Malformed input returns null.

**Acceptance:** Request parsing handles valid and malformed HTTP requests correctly.

### Batch 11.2 — HttpResponse & StaticFileHandler

**Delegation:** in-session
**Decisions:** D-11.1, D-11.2, D-11.3, D-11.5
**Depends on:** Batch 11.1
**Files:**
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/HttpResponse.kt` (create)
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/StaticFileHandler.kt` (create)
- `challenges/web-server/src/test/kotlin/dev/gregross/challenges/httpd/StaticFileHandlerTest.kt` (create)
- `challenges/web-server/src/test/resources/www/index.html` (create — test fixture)
- `challenges/web-server/src/test/resources/www/style.css` (create — test fixture)
- `challenges/web-server/src/test/resources/www/sub/page.html` (create — test fixture)

**Work:**
1. Implement `HttpResponse(statusCode: Int, reason: String, headers: Map<String, String>, body: ByteArray)`. Method `writeTo(output: OutputStream)` — writes status line, headers, blank line, body.
2. Implement `StaticFileHandler(docRoot: File)`. Method `handle(request: HttpRequest): HttpResponse` — resolves path, checks traversal, reads file, determines MIME type. Returns 200/403/404/405 responses. Maps `/` to `/index.html`. Extension-based MIME type lookup.
3. Create test fixtures: `www/index.html`, `www/style.css`, `www/sub/page.html`.
4. Write tests: serve index.html, serve CSS with correct MIME, 404 for missing file, default document (`/`), path traversal blocked (403), subdirectory access, 405 for non-GET.

**Test criteria:**
- `./gradlew :challenges:web-server:test` — all handler tests pass.
- GET `/` returns 200 with index.html content.
- GET `/../etc/passwd` returns 403.
- GET `/missing.html` returns 404.

**Acceptance:** Static file serving with path traversal protection works correctly.

### Batch 11.3 — WebServer, CLI & concurrency

**Delegation:** in-session
**Decisions:** D-11.1, D-11.2, D-11.4
**Depends on:** Batch 11.2
**Files:**
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/WebServer.kt` (create)
- `challenges/web-server/src/main/kotlin/dev/gregross/challenges/httpd/Main.kt` (modify)

**Work:**
1. Implement `WebServer(port: Int, docRoot: File)`. Method `start()` — bind ServerSocket, accept connections in a loop, dispatch each to a virtual thread. Each connection: parse request, handle, write response, close.
2. Implement `main()`: parse `--port` and `--docroot` flags, validate docroot exists, create and start WebServer. Shutdown hook for clean exit.
3. Create `www/` directory with default `index.html` in project root for manual testing.

**Test criteria:**
- Server starts and accepts connections on configured port.
- Responds to curl requests.

**Acceptance:** Server runs, serves files, handles concurrent connections.

### Batch 11.4 — Native image, integration tests & manual verification

**Delegation:** in-session
**Decisions:** D-11.6
**Depends on:** Batch 11.3
**Files:**
- `challenges/web-server/src/test/kotlin/dev/gregross/challenges/httpd/IntegrationTest.kt` (create)

**Work:**
1. Write integration tests: start server on random port, send HTTP requests via Socket, verify 200/404/403/405 responses. Test concurrency with parallel requests. Test path traversal attempts.
2. Build native image: `./gradlew :challenges:web-server:nativeCompile`.
3. Install: `./gradlew :challenges:web-server:install`.
4. Verify symlink and present manual verification commands using curl.

**Test criteria:**
- `./gradlew :challenges:web-server:test` — all tests pass.
- `curl -i http://localhost:8080/` returns 200 with HTML.
- `curl -i http://localhost:8080/../etc/passwd` returns 403.
- `curl -i http://localhost:8080/missing` returns 404.
- Concurrent `curl` requests all succeed.

**Acceptance:** Native binary installed as `gig-httpd`, user has manually verified.

### Batch 11.5 — Web server article write-up

**Delegation:** in-session
**Decisions:** D-11.7
**Depends on:** Batch 11.4
**Files:**
- `challenges/web-server/ARTICLE.md` (create)

**Work:**
1. Write article from established template.
2. Focus areas: HTTP/1.1 request/response format, TCP socket programming, virtual threads for concurrency, path traversal defense with canonical path resolution, MIME type determination, static file serving model.

**Test criteria:**
- `challenges/web-server/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `curl http://localhost:8080/` returns 200 with index.html content
- [ ] `curl http://localhost:8080/index.html` returns 200
- [ ] `curl http://localhost:8080/style.css` returns 200 with `text/css` Content-Type
- [ ] `curl http://localhost:8080/missing` returns 404
- [ ] `curl http://localhost:8080/../etc/passwd` returns 403
- [ ] Non-GET method returns 405
- [ ] Concurrent requests handled (virtual threads)
- [ ] `--port` and `--docroot` flags work
- [ ] Native binary installed as `gig-httpd`
- [ ] Article written at `challenges/web-server/ARTICLE.md`

**Completion triggers Phase 12 → version `0.12.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
