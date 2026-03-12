# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 5 — Load Balancer (v0.5.x)

> Build an HTTP Layer 7 load balancer from scratch — TCP socket server, HTTP request/response parsing, round-robin routing across multiple backends, periodic health checks with automatic failover, and virtual thread concurrency — all in Kotlin using only JVM stdlib. Deliver as `gig-lb` native binary with article write-up.

**Decisions:** D-5.1, D-5.2, D-5.3, D-5.4, D-5.5, D-5.6, D-5.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 5.1 | `0.5.1` | Module scaffold & HTTP parsing | in-session | pending |
| 5.2 | `0.5.2` | Load balancer core & single-backend forwarding | in-session | pending |
| 5.3 | `0.5.3` | Round-robin & multi-backend support | in-session | pending |
| 5.4 | `0.5.4` | Health checker & automatic failover | in-session | pending |
| 5.5 | `0.5.5` | CLI, native image & integration tests | in-session | pending |
| 5.6 | `0.5.6` | Load balancer article write-up | in-session | pending |

### Batch 5.1 — Module scaffold & HTTP parsing

**Delegation:** in-session
**Decisions:** D-5.1, D-5.3
**Files:**
- `settings.gradle.kts` (modify — add `challenges:load-balancer`)
- `challenges/load-balancer/build.gradle.kts` (create)
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/HttpRequest.kt` (create)
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/HttpResponse.kt` (create)
- `challenges/load-balancer/src/test/kotlin/dev/gregross/challenges/lb/HttpParsingTest.kt` (create)

**Work:**
1. Add `challenges:load-balancer` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-lb`.
3. Implement `HttpRequest` data class with parser: reads request line (method, path, version) and headers from an `InputStream`. Stops at blank line (`\r\n\r\n`).
4. Implement `HttpResponse` data class with writer: writes status line, headers, and body to an `OutputStream`.
5. Write parsing tests: valid GET request, multiple headers, missing headers, malformed request line.

**Test criteria:**
- `./gradlew :challenges:load-balancer:test` — all parsing tests pass.
- Parses `GET / HTTP/1.1` with headers correctly.

**Acceptance:** Module compiles, HTTP request/response parsing works.

### Batch 5.2 — Load balancer core & single-backend forwarding

**Delegation:** in-session
**Decisions:** D-5.1, D-5.2, D-5.3
**Depends on:** Batch 5.1
**Files:**
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/Backend.kt` (create)
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/ProxyServer.kt` (create)
- `challenges/load-balancer/src/test/kotlin/dev/gregross/challenges/lb/ProxyServerTest.kt` (create)

**Work:**
1. Define `Backend` data class: `url: String`, `healthy: Boolean` (volatile/atomic).
2. Implement `ProxyServer` that: accepts connections on a `ServerSocket`, spawns a virtual thread per connection, parses the incoming HTTP request, forwards to a backend using `java.net.http.HttpClient`, writes the backend's response back to the client socket.
3. Add `Connection: close` to responses, strip hop-by-hop headers, add `X-Forwarded-For`.
4. Log request info to stdout.
5. Write integration test: start an embedded test server, start the proxy, send a request through the proxy, verify the response matches the backend.

**Test criteria:**
- `./gradlew :challenges:load-balancer:test` — proxy forwards request to single backend and returns correct response.

**Acceptance:** Single-backend forwarding works end-to-end through real TCP connections.

### Batch 5.3 — Round-robin & multi-backend support

**Delegation:** in-session
**Decisions:** D-5.1
**Depends on:** Batch 5.2
**Files:**
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/RoundRobin.kt` (create)
- `challenges/load-balancer/src/test/kotlin/dev/gregross/challenges/lb/RoundRobinTest.kt` (create)

**Work:**
1. Implement `RoundRobin` class with thread-safe `next()` method using `AtomicInteger`. Only selects from healthy backends.
2. Integrate into `ProxyServer` — each request gets the next backend from the round-robin.
3. Write unit tests: cycles through backends in order, wraps around, skips unhealthy backends, handles single backend, handles all-unhealthy (returns 503).
4. Write integration test: start 2-3 embedded backends with distinct responses, verify requests are distributed round-robin.

**Test criteria:**
- `./gradlew :challenges:load-balancer:test` — round-robin distributes evenly across backends.
- 6 requests to 3 backends → each gets exactly 2.

**Acceptance:** Round-robin distributes requests across multiple healthy backends.

### Batch 5.4 — Health checker & automatic failover

**Delegation:** in-session
**Decisions:** D-5.5
**Depends on:** Batch 5.3
**Files:**
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/HealthChecker.kt` (create)
- `challenges/load-balancer/src/test/kotlin/dev/gregross/challenges/lb/HealthCheckerTest.kt` (create)

**Work:**
1. Implement `HealthChecker` using `ScheduledExecutorService` with virtual thread factory. Sends HTTP GET to each backend's root path. 200 = healthy, else = unhealthy. Updates `Backend.healthy` atomically.
2. On startup, run an initial health check before accepting connections.
3. Write integration tests: start backends, verify all marked healthy. Kill one, wait for health check interval, verify it's marked unhealthy and removed from rotation. Restart it, verify re-added.

**Test criteria:**
- `./gradlew :challenges:load-balancer:test` — health checker detects down/up backends.
- Dead backend removed from pool, recovered backend re-added.

**Acceptance:** Health checker runs in background, updates pool automatically.

### Batch 5.5 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-5.4
**Depends on:** Batch 5.4
**Files:**
- `challenges/load-balancer/src/main/kotlin/dev/gregross/challenges/lb/Main.kt` (create)
- `challenges/load-balancer/src/test/kotlin/dev/gregross/challenges/lb/IntegrationTest.kt` (create)

**Work:**
1. Implement `main()` with argument parsing: `--port`, `--backends`, `--health-interval`.
2. Wire up ProxyServer + RoundRobin + HealthChecker.
3. Graceful shutdown on SIGINT (close server socket, stop health checker).
4. Write full integration test: 3 embedded backends, send concurrent requests via virtual threads, verify round-robin, kill one backend, verify failover.
5. Build native image and install.
6. Present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:load-balancer:test` — all tests pass.
- `gig-lb --port 8080 --backends http://localhost:9090,http://localhost:9091` starts and forwards.
- Concurrent requests distribute correctly.

**Acceptance:** CLI works, native binary installed as `gig-lb`, user has manually verified.

### Batch 5.6 — Load balancer article write-up

**Delegation:** in-session
**Decisions:** D-5.7
**Depends on:** Batch 5.5
**Files:**
- `challenges/load-balancer/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template.
2. Focus areas: Layer 7 proxy concepts, virtual threads for concurrent connections, hand-rolled HTTP parsing, round-robin algorithm, health check design, automatic failover.

**Test criteria:**
- `challenges/load-balancer/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-lb` accepts HTTP connections and forwards to backend servers
- [ ] Round-robin distributes requests across multiple backends
- [ ] Health checker detects unhealthy backends and removes from rotation
- [ ] Recovered backends are re-added to rotation
- [ ] Handles concurrent connections via virtual threads
- [ ] Logs request info to stdout
- [ ] `--port`, `--backends`, `--health-interval` CLI flags work
- [ ] Returns 503 when all backends are down
- [ ] Adds `X-Forwarded-For` header
- [ ] Native binary installed as `gig-lb`
- [ ] Article written at `challenges/load-balancer/ARTICLE.md`

**Completion triggers Phase 6 → version `0.6.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
