# Building My Own Load Balancer: HTTP Reverse Proxy in Kotlin

> Building a Layer 7 HTTP load balancer from scratch — TCP socket server, HTTP parsing, round-robin routing, background health checks with automatic failover, and virtual thread concurrency — all in Kotlin using only JVM stdlib.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Load Balancer](https://codingchallenges.fyi/challenges/challenge-load-balancer)

Load balancers are the invisible infrastructure behind every high-traffic website. They sit between clients and backend servers, distributing requests to keep things fast and reliable. When a server goes down, the load balancer routes around it. When a server recovers, it's added back.

This challenge asks you to build one: accept HTTP connections, forward them to backend servers using round-robin, and periodically health-check the backends. It's a significant step up from the previous challenges — this is the first one involving networking, concurrency, and real-time state management.

This is challenge #4 on Coding Challenges, and it's the most complex one I've tackled so far. Instead of processing files, you're managing TCP connections, parsing HTTP on the wire, and coordinating background health checks with request routing.

## Approach

Three components with clean separation: an **HTTP server** that accepts and parses incoming connections, a **load balancer** that routes requests round-robin across healthy backends, and a **health checker** that probes backends in the background and updates their status. All concurrency uses Java 21 virtual threads — one per connection, no thread pools to size, no async callbacks.

The key architectural bet: use `java.net.ServerSocket` for inbound connections (full control over HTTP parsing) and `java.net.http.HttpClient` for outbound backend requests (modern API with connection pooling). Everything stays within JVM stdlib — no Ktor, no Netty, no external dependencies.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Inbound server | `java.net.ServerSocket` | Full control, zero deps, GraalVM-friendly |
| Outbound client | `java.net.http.HttpClient` | Modern API, connection pooling, built into JVM 11+ |
| Concurrency | Virtual threads (Java 21) | Simplest model — blocking code with async performance |
| HTTP scope | GET forwarding, `Connection: close` | Challenge only needs GET; close avoids keep-alive complexity |
| Health checks | `ScheduledExecutorService` with virtual threads | Stdlib periodic scheduling, non-blocking |
| Thread safety | `AtomicBoolean` on Backend, `AtomicInteger` in RoundRobin | Lock-free concurrent access |

## Build Log

### Step 1 — HTTP Request/Response Parsing

Hand-rolled HTTP/1.1 parsing. Read the request line (`GET /path HTTP/1.1`), then read headers line by line until the blank line (`\r\n`). The response writer formats status line, headers, `Connection: close`, `Content-Length`, then the body. Simple but complete enough for a proxy.

### Step 2 — Single-Backend Forwarding

The `ProxyServer` accepts connections on a `ServerSocket`, spawns a virtual thread per connection, parses the request, forwards it to a backend using `HttpClient.send()`, and writes the response back. Hop-by-hop headers (`Connection`, `Transfer-Encoding`, etc.) are stripped per the HTTP spec. `X-Forwarded-For` is added with the client's IP.

### Step 3 — Round-Robin Distribution

The `RoundRobin` class uses an `AtomicInteger` counter to cycle through healthy backends. Each call to `next()` filters the list to healthy backends and picks the next one. Thread-safe by design — the atomic counter handles concurrent access without locks.

### Step 4 — Health Checks and Failover

The `HealthChecker` runs on a scheduled virtual thread, sending HTTP GET to each backend's root path every N seconds. 200 means healthy, anything else (including connection refused) means unhealthy. Backend health is stored in an `AtomicBoolean`, so the round-robin immediately sees changes without synchronization. When a backend goes down, it's automatically excluded. When it recovers, it's automatically re-added.

### Step 5 — CLI and Native Image

The CLI parses `--port`, `--backends`, and `--health-interval` flags. A shutdown hook ensures clean teardown on Ctrl-C. The native image built successfully with GraalVM 24 — `java.net.http.HttpClient` and virtual threads both work in native-image without special configuration.

## Key Code

### Virtual thread per connection

```kotlin
val executor = Executors.newVirtualThreadPerTaskExecutor()
while (running.get()) {
    val clientSocket = socket.accept()
    executor.submit { handleConnection(clientSocket) }
}
```

One virtual thread per connection. No thread pool sizing, no callback hell, no async/await. Write blocking code, get async performance. On JVM 21, virtual threads unmount from carrier threads on blocking I/O, so millions of concurrent connections are possible.

### Thread-safe round-robin

```kotlin
class RoundRobin(private val backends: List<Backend>) {
    private val counter = AtomicInteger(0)

    fun next(): Backend? {
        val healthy = backends.filter { it.healthy }
        if (healthy.isEmpty()) return null
        val index = counter.getAndIncrement() % healthy.size
        return healthy[index]
    }
}
```

The `AtomicInteger` ensures every concurrent request gets a unique counter value. Filtering to healthy backends on every call means changes from the health checker are picked up immediately.

### HTTP forwarding with header handling

```kotlin
val hopByHop = setOf("connection", "keep-alive", "proxy-authenticate",
    "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade")
for ((name, value) in request.headers) {
    if (name.lowercase() !in hopByHop && name.lowercase() != "host") {
        builder.header(name, value)
    }
}
builder.header("X-Forwarded-For", clientIp)
```

Strip hop-by-hop headers (required by the HTTP spec for proxies), add the standard `X-Forwarded-For` header so backends know the real client IP.

## Testing

Three tiers: HTTP parsing, round-robin logic, and full integration with embedded servers.

| Test Case | Category | Expected | Result |
|-----------|----------|----------|--------|
| Parse GET request line | Parsing | Correct method/path/version | Pass |
| Parse multiple headers | Parsing | All headers captured | Pass |
| Parse path with segments | Parsing | Full path preserved | Pass |
| No headers request | Parsing | Empty header map | Pass |
| Reject empty input | Parsing | Throws exception | Pass |
| Reject malformed request | Parsing | Throws exception | Pass |
| Write HTTP response | Parsing | Correct format with headers | Pass |
| Write error response | Parsing | 503 with body | Pass |
| Forward to single backend | Proxy | Correct response returned | Pass |
| Return 503 when no backends | Proxy | 503 status | Pass |
| X-Forwarded-For added | Proxy | No crash, header added | Pass |
| Round-robin cycles in order | RoundRobin | A, B, C sequence | Pass |
| Round-robin wraps around | RoundRobin | Returns to first | Pass |
| Skips unhealthy backends | RoundRobin | Only healthy selected | Pass |
| Returns null when all down | RoundRobin | null | Pass |
| Single backend works | RoundRobin | Same backend always | Pass |
| Recovered backend re-added | RoundRobin | Both in rotation | Pass |
| Even distribution (6 req, 3 srv) | RoundRobin | 2 each | Pass |
| Marks running as healthy | HealthCheck | healthy=true | Pass |
| Marks stopped as unhealthy | HealthCheck | healthy=false | Pass |
| Recovers backend | HealthCheck | healthy restored | Pass |
| Independent multi-backend | HealthCheck | Mixed health states | Pass |
| 3-backend round-robin | Integration | Even distribution | Pass |
| Failover on backend death | Integration | Remaining backends serve | Pass |
| Concurrent requests | Integration | All 10 succeed | Pass |
| All backends down | Integration | 503 returned | Pass |

Total: 26 tests across HTTP parsing (8), proxy (3), round-robin (7), health checker (4), and integration (4).

## What I Learned

- **Virtual threads change everything about JVM concurrency.** No thread pools to size, no async callbacks, no `CompletableFuture` chains. Just `executor.submit { blockingCode() }` and the JVM handles the rest. For an I/O-bound server like a load balancer, virtual threads are the perfect fit.

- **Hand-rolling HTTP parsing is educational but tedious.** Reading `\r\n`-delimited lines, parsing the request line, handling headers — it's straightforward but there are many edge cases. Using `java.net.http.HttpClient` for the outbound side was the right call — it handles connection pooling, redirect following, and proper HTTP semantics.

- **Atomic operations make concurrency simple when the state is simple.** `AtomicBoolean` for backend health, `AtomicInteger` for the round-robin counter — no locks, no synchronized blocks, no race conditions. When your shared state is a single boolean or integer, atomics are the right tool.

- **Health checks need to be fire-and-forget.** The health checker runs in the background and updates state that the request path reads. There's no coordination, no signaling, no waiting. The round-robin just checks `backend.healthy` on every request — if the health checker updated it, great; if not, the old value is still valid.

## Running It

```bash
# Start test backends (in separate terminals)
python3 -m http.server 9090 --directory /tmp/server1
python3 -m http.server 9091 --directory /tmp/server2
python3 -m http.server 9092 --directory /tmp/server3

# Start the load balancer
gig-lb --backends http://localhost:9090,http://localhost:9091,http://localhost:9092 --port 8080 --health-interval 10

# Send requests
curl http://localhost:8080/
curl http://localhost:8080/
curl http://localhost:8080/

# Test failover: kill one backend, wait for health check, requests route around it
```

---

*Built as part of my [Coding Challenges](https://codingchallenges.fyi) series. Stack: Kotlin + Gradle.*
