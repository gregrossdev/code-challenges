# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

## 2026-03-11 — Architecture: How should the load balancer be structured?

**Decision:** Three components: (1) `HttpServer` — accepts inbound TCP connections on a configurable port using `java.net.ServerSocket`, parses HTTP requests, writes HTTP responses. (2) `LoadBalancer` — maintains the server pool, implements round-robin selection, forwards requests to backends using `java.net.http.HttpClient`. (3) `HealthChecker` — periodic background task that probes backends and updates their healthy/unhealthy status.
**Rationale:** Clean separation: the server handles TCP/HTTP, the balancer handles routing logic, the health checker handles monitoring. Each can be tested independently. All stdlib — no external dependencies.
**Alternatives considered:** Using Ktor or Netty (adds heavyweight dependencies, violates project philosophy), single monolithic class (too many concerns), event-loop architecture (more complex than needed for this scope).
**Status:** ACTIVE
**ID:** D-5.1

## 2026-03-11 — Concurrency: How should concurrent connections be handled?

**Decision:** Java 21 virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`. One virtual thread per incoming connection. Health checker runs on a separate scheduled virtual thread. `java.net.http.HttpClient` for outbound backend requests (blocking `send()` inside virtual threads).
**Rationale:** Virtual threads are the clear winner on JVM 21 — write simple blocking code, get async performance. No external dependency needed (unlike Kotlin coroutines which require kotlinx-coroutines). GraalVM 24 supports virtual threads in native-image. The `java.net.http.HttpClient` provides modern API with connection pooling built-in.
**Alternatives considered:** Kotlin coroutines (requires external dependency), fixed thread pool (caps concurrency unnecessarily), raw NIO/selectors (over-engineered for this scope).
**Status:** ACTIVE
**ID:** D-5.2

## 2026-03-11 — HTTP: What HTTP features should the proxy support?

**Decision:** HTTP/1.1 GET forwarding. Parse request line (method, path, version) and headers. Forward all headers to backend (stripping hop-by-hop headers like `Connection`). Forward response status, headers, and body back to client. Always send `Connection: close` to simplify — no keep-alive. Add `X-Forwarded-For` header with client IP.
**Rationale:** The challenge only requires GET forwarding. `Connection: close` avoids the complexity of keep-alive connection management while still being valid HTTP/1.1. Stripping hop-by-hop headers is required by the HTTP spec for proxies. `X-Forwarded-For` is standard proxy behavior.
**Alternatives considered:** Full HTTP method support (out of scope), keep-alive (adds significant complexity for minimal benefit in this challenge), HTTP/2 (overkill).
**Status:** ACTIVE
**ID:** D-5.3

## 2026-03-11 — CLI: What should gig-lb do and how should it behave?

**Decision:** `gig-lb --port 8080 --backends http://localhost:9090,http://localhost:9091,http://localhost:9092 --health-interval 10`. Port defaults to 8080. Backends are required (comma-separated URLs). Health check interval defaults to 10 seconds. Log request/response info to stdout. Exit on SIGINT/Ctrl-C with graceful shutdown.
**Rationale:** Command-line arguments match the challenge requirements. Comma-separated backends is straightforward. Health interval as a flag makes testing easy (short intervals). Logging to stdout follows the challenge's expectations.
**Alternatives considered:** Config file (overkill for 3 flags), YAML config (adds complexity/dependency).
**Status:** ACTIVE
**ID:** D-5.4

## 2026-03-11 — Health Checks: How should health checking work?

**Decision:** Background scheduled task sends HTTP GET to each backend's root path (`/`) every N seconds. HTTP 200 = healthy, anything else or connection failure = unhealthy. Unhealthy backends are removed from the round-robin pool. When a backend passes a health check again, it's re-added. Use `java.util.concurrent.ScheduledExecutorService` with virtual threads. Thread-safe server pool using `ConcurrentHashMap` or synchronized list.
**Rationale:** The challenge specifies GET to a health URL, 200 = healthy, configurable interval, background execution. `ScheduledExecutorService` is stdlib and handles periodic scheduling cleanly. Thread-safe collections prevent races between the health checker and the request handler.
**Alternatives considered:** Health check on every request (too slow, defeats purpose), external health check process (unnecessary complexity).
**Status:** ACTIVE
**ID:** D-5.5

## 2026-03-11 — Testing: What test strategy for the load balancer?

**Decision:** Three tiers: (1) Unit tests for HTTP request parsing and response writing, (2) Unit tests for round-robin selection and health-based pool management, (3) Integration tests that start embedded backend servers (using `ServerSocket` in test threads) and verify forwarding, round-robin distribution, and health check behavior.
**Rationale:** The networking nature makes integration tests essential — you need real TCP connections to verify the proxy works. But unit-testing the parsing and round-robin logic independently catches bugs faster. Embedded test servers avoid requiring external Python servers during `./gradlew test`.
**Alternatives considered:** Only manual testing with Python servers (not automated), mocking all networking (misses real integration bugs).
**Status:** ACTIVE
**ID:** D-5.6

## 2026-03-11 — Content: Article write-up for load balancer challenge

**Decision:** Write `challenges/load-balancer/ARTICLE.md` after implementation. Focus on: Layer 7 proxy concepts, virtual threads for concurrent connections, round-robin algorithm, health check design, and the experience of hand-rolling HTTP parsing vs using libraries.
**Rationale:** Established convention. This is the most complex challenge so far — the article should highlight the networking and concurrency concepts.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-5.7
