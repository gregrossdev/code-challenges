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

## 2026-03-11 — Architecture: How should the Redis server be structured?

**Decision:** Four components: (1) **RESP codec** — parses and serializes RESP protocol messages. (2) **Command handler** — dispatches parsed commands to the appropriate logic. (3) **Data store** — thread-safe in-memory key-value store with expiry support. (4) **TCP server** — accepts client connections, one virtual thread per client, reads RESP messages in a loop. A `RespValue` sealed interface models all RESP types.
**Rationale:** Clean separation of protocol, command logic, storage, and networking. Virtual threads (Java 21) give simple blocking I/O per client without thread pool sizing. The RESP codec is reusable for both parsing requests and serializing responses. Matches patterns from load balancer (virtual threads, ServerSocket) and JSON parser (sealed interface for data model).
**Alternatives considered:** Single-threaded event loop (more complex, unnecessary for this challenge), coroutines (external dependency), NIO channels (over-engineered).
**Status:** ACTIVE
**ID:** D-8.1

## 2026-03-11 — Protocol: How should RESP parsing work?

**Decision:** `RespValue` sealed interface with: `SimpleString(value: String)`, `Error(message: String)`, `Integer(value: Long)`, `BulkString(value: String?)` (nullable for null bulk strings), `Array(elements: List<RespValue>?)` (nullable for null arrays). Parser reads from `InputStream` byte-by-byte, dispatching on the first byte (`+`, `-`, `:`, `$`, `*`). Serializer writes RESP format to `OutputStream`.
**Rationale:** The five RESP types map directly to sealed interface subtypes. Byte-by-byte parsing with CRLF termination is straightforward. Nullable bulk strings and arrays handle the null cases (`$-1`, `*-1`). Client requests are always arrays of bulk strings per the spec.
**Alternatives considered:** String-based parsing (loses binary safety), buffered line reading (doesn't handle bulk strings correctly since they use length prefixes).
**Status:** ACTIVE
**ID:** D-8.2

## 2026-03-11 — Commands: Which Redis commands to implement?

**Decision:** 11 commands matching the challenge steps: PING, ECHO (Step 2), SET, GET (Step 3), SET with EX/PX/EXAT/PXAT options (Step 5), EXISTS, DEL, INCR, DECR, LPUSH, RPUSH (Step 6). No SAVE — persistence is out of scope for the core challenge. Commands are case-insensitive.
**Rationale:** These cover all challenge steps except SAVE. Skipping SAVE keeps the scope focused on networking and protocol — persistence adds serialization complexity without teaching new concepts beyond what the compression tool already covered. The 11 commands cover strings, integers, lists, and key operations.
**Alternatives considered:** Including SAVE (adds file I/O complexity, diminishing returns), adding more list commands like LRANGE/LPOP (not requested).
**Status:** ACTIVE
**ID:** D-8.3

## 2026-03-11 — Storage: How should the data store work?

**Decision:** `DataStore` class using `ConcurrentHashMap<String, StoreEntry>` where `StoreEntry` holds the value (String or MutableList<String>) and optional expiry timestamp. Expiry checked lazily on access (get/exists) — if expired, remove and return null. Type checking on access: INCR/DECR require string-parseable-as-long, LPUSH/RPUSH require list type. Thread-safe via ConcurrentHashMap's built-in concurrency.
**Rationale:** ConcurrentHashMap provides thread-safe access without explicit locking, which is simpler and more performant than synchronized blocks. Lazy expiry avoids the need for a background cleanup thread. StoreEntry wrapping allows storing both strings and lists with metadata (expiry).
**Alternatives considered:** Synchronized HashMap (coarser locking), separate maps for strings and lists (complicates EXISTS/DEL), active expiry via scheduled executor (unnecessary complexity for the challenge).
**Status:** ACTIVE
**ID:** D-8.4

## 2026-03-11 — Networking: How should the TCP server work?

**Decision:** `ServerSocket` on port 6379 (configurable via `--port`). Accept loop spawns a virtual thread per client. Each client thread reads RESP commands in a loop, dispatches to command handler, writes RESP response, until the client disconnects. Graceful shutdown on SIGINT.
**Rationale:** Same pattern as the load balancer — proven to work with virtual threads and GraalVM native image. One thread per client with blocking I/O is the simplest model and performs well with virtual threads. Port configurability avoids conflicts with a real Redis instance.
**Alternatives considered:** NIO selector loop (more complex, unnecessary), fixed thread pool (virtual threads are better), Netty (external dependency).
**Status:** ACTIVE
**ID:** D-8.5

## 2026-03-11 — Testing: What test strategy for the Redis server?

**Decision:** Three tiers: (1) Unit tests for RESP codec — parse and serialize all 5 types, round-trip tests. (2) Unit tests for command handler — verify each command's behavior and error cases. (3) Integration tests using a real TCP client — connect, send RESP commands, verify responses. Use embedded server with random port to avoid conflicts.
**Rationale:** RESP codec has clear input/output for unit testing. Command handler tests verify business logic without networking. Integration tests verify the full stack including TCP and concurrency. Random ports prevent test flakiness from port conflicts.
**Alternatives considered:** Testing with redis-cli (requires Redis installed, flaky), only integration tests (can't pinpoint failures).
**Status:** ACTIVE
**ID:** D-8.6

## 2026-03-11 — Content: Article write-up for Redis server challenge

**Decision:** Write `challenges/redis-server/ARTICLE.md` after implementation. Focus on RESP protocol parsing, concurrent client handling with virtual threads, and how Redis's simple protocol enables high performance.
**Rationale:** Established convention. RESP is the most interesting part — it's a real-world protocol that's elegantly simple. The virtual threads angle connects to the load balancer.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-8.7
