# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 12 — URL Shortener (v0.12.x)

> Build a URL shortener REST API from raw TCP sockets — SHA-256 hash-based short code generation with collision handling, in-memory dual-map storage for O(1) idempotent lookups, 302 redirects, JSON request/response handling, and virtual thread concurrency. Reuses web server infrastructure from Phase 11. Deliver as `gig-urls` native binary with article write-up.

**Decisions:** D-12.1, D-12.2, D-12.3, D-12.4, D-12.5, D-12.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 12.1 | `0.12.1` | Module scaffold, CodeGenerator & UrlStore | in-session | done |
| 12.2 | `0.12.2` | Request body parsing & ApiHandler | in-session | done |
| 12.3 | `0.12.3` | WebServer, CLI & integration tests | in-session | done |
| 12.4 | `0.12.4` | Native image & manual verification | in-session | done |
| 12.5 | `0.12.5` | URL shortener article write-up | in-session | done |

### Batch 12.1 — Module scaffold, CodeGenerator & UrlStore

**Delegation:** in-session
**Decisions:** D-12.1, D-12.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:url-shortener`)
- `challenges/url-shortener/build.gradle.kts` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/CodeGenerator.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/UrlStore.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/Main.kt` (create — stub)
- `challenges/url-shortener/src/test/kotlin/dev/gregross/challenges/urls/CodeGeneratorTest.kt` (create)
- `challenges/url-shortener/src/test/kotlin/dev/gregross/challenges/urls/UrlStoreTest.kt` (create)

**Work:**
1. Add `challenges:url-shortener` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-urls`.
3. Implement `CodeGenerator`. Method `generate(url: String): String` — SHA-256 hash, take first 6 bytes (48 bits), encode as base62 (8 chars). Method `generate(url: String, attempt: Int): String` for collision retry (appends attempt number before hashing).
4. Implement `UrlStore` — thread-safe (ConcurrentHashMap) dual maps: `codeToUrl` and `urlToCode`. Methods: `create(url: String, code: String): Boolean`, `getUrl(code: String): String?`, `getCode(url: String): String?`, `delete(code: String): Boolean`.
5. Write tests: deterministic hash output, same URL same code, base62 alphabet, collision retry produces different code. Store CRUD, idempotency check, thread safety.

**Test criteria:**
- `./gradlew :challenges:url-shortener:test` — all tests pass.
- Same URL always generates the same code.
- Store supports create/read/delete with dual-map consistency.

**Acceptance:** Code generation and storage work correctly with idempotency.

### Batch 12.2 — Request body parsing & ApiHandler

**Delegation:** in-session
**Decisions:** D-12.1, D-12.3
**Depends on:** Batch 12.1
**Files:**
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/HttpRequest.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/HttpResponse.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/RequestParser.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/ApiHandler.kt` (create)
- `challenges/url-shortener/src/test/kotlin/dev/gregross/challenges/urls/ApiHandlerTest.kt` (create)

**Work:**
1. Create HttpRequest/HttpResponse/RequestParser — adapted from Phase 11 but with body reading support (reads Content-Length bytes after headers).
2. Implement `ApiHandler(store: UrlStore, generator: CodeGenerator, baseUrl: String)`. Methods: `handle(request: HttpRequest): HttpResponse`. Routes: POST / → create (parse JSON body, validate URL, generate code, store, return 201 JSON), GET /{code} → redirect (lookup, return 302 with Location header or 404), DELETE /{code} → delete (remove, return 200).
3. Write tests: create returns 201 with JSON, duplicate URL returns same code, missing URL field returns 400, invalid URL returns 400, redirect returns 302 with Location, unknown code returns 404, delete returns 200, delete unknown returns 200.

**Test criteria:**
- `./gradlew :challenges:url-shortener:test` — all tests pass.
- POST with valid URL returns 201 with key/long_url/short_url.
- GET /{code} returns 302 with correct Location header.

**Acceptance:** All three API operations work with proper status codes and JSON responses.

### Batch 12.3 — WebServer, CLI & integration tests

**Delegation:** in-session
**Decisions:** D-12.1, D-12.4, D-12.5
**Depends on:** Batch 12.2
**Files:**
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/UrlServer.kt` (create)
- `challenges/url-shortener/src/main/kotlin/dev/gregross/challenges/urls/Main.kt` (modify)
- `challenges/url-shortener/src/test/kotlin/dev/gregross/challenges/urls/IntegrationTest.kt` (create)

**Work:**
1. Implement `UrlServer(port: Int, baseUrl: String)` — TCP ServerSocket, virtual thread per client, delegates to RequestParser + ApiHandler.
2. Implement `main()`: parse `--port` and `--base-url` flags, create and start server.
3. Write integration tests: start server on random port, create URL via POST, redirect via GET, delete via DELETE, idempotent create, 404 after delete, concurrent creates.

**Test criteria:**
- `./gradlew :challenges:url-shortener:test` — all tests pass.
- Full create → redirect → delete cycle works over TCP.

**Acceptance:** Server handles all API operations over HTTP with concurrent clients.

### Batch 12.4 — Native image & manual verification

**Delegation:** in-session
**Decisions:** D-12.4
**Depends on:** Batch 12.3
**Files:** (none new)

**Work:**
1. Build native image: `./gradlew :challenges:url-shortener:nativeCompile`.
2. Install: `./gradlew :challenges:url-shortener:install`.
3. Verify symlink and present manual verification commands using curl.

**Test criteria:**
- `gig-urls` binary runs and accepts HTTP requests.
- `curl -X POST -d '{"url":"https://example.com"}' http://localhost:8080/` returns 201.
- `curl -i http://localhost:8080/{code}` returns 302.

**Acceptance:** Native binary installed as `gig-urls`, user has manually verified.

### Batch 12.5 — URL shortener article write-up

**Delegation:** in-session
**Decisions:** D-12.6
**Depends on:** Batch 12.4
**Files:**
- `challenges/url-shortener/ARTICLE.md` (create)

**Work:**
1. Write article from `~/.claude/templates/gig/ARTICLE.md` template.
2. Include Usage section with curl examples for all three endpoints.
3. Focus areas: hash-based code generation with collision handling, base62 encoding, idempotent POST, 302 vs 301 redirects, dual-map storage pattern, building a REST API from raw sockets.

**Test criteria:**
- `challenges/url-shortener/ARTICLE.md` exists with all sections populated including Usage.
- No placeholder text remains.

**Acceptance:** Complete, publishable article with Usage section.

**Phase Acceptance Criteria:**
- [ ] `POST /` with valid URL returns 201 with JSON (key, long_url, short_url)
- [ ] `POST /` with same URL returns same short code (idempotent)
- [ ] `POST /` with missing/invalid URL returns 400
- [ ] `GET /{code}` returns 302 with Location header
- [ ] `GET /{code}` for unknown code returns 404
- [ ] `DELETE /{code}` returns 200
- [ ] `DELETE /{code}` for unknown code returns 200 (idempotent)
- [ ] `GET /{code}` after delete returns 404
- [ ] Concurrent requests handled (virtual threads)
- [ ] `--port` and `--base-url` flags work
- [ ] Native binary installed as `gig-urls`
- [ ] Article written at `challenges/url-shortener/ARTICLE.md` with Usage section

**Completion triggers Phase 13 → version `0.13.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
