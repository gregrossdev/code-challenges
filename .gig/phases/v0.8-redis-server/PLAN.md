# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 8 — Redis Server (v0.8.x)

> Build a Redis-compatible server from scratch — RESP protocol parsing, TCP server with virtual thread per client, in-memory key-value store with expiry, and 11 commands covering strings, integers, lists, and key operations. Testable with the real `redis-cli`. Deliver as `gig-redis` native binary with article write-up.

**Decisions:** D-8.1, D-8.2, D-8.3, D-8.4, D-8.5, D-8.6, D-8.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 8.1 | `0.8.1` | Module scaffold & RESP codec | in-session | done |
| 8.2 | `0.8.2` | Data store & PING/ECHO/SET/GET commands | in-session | done |
| 8.3 | `0.8.3` | TCP server & concurrent clients | in-session | done |
| 8.4 | `0.8.4` | Key expiry (EX/PX/EXAT/PXAT) | in-session | done |
| 8.5 | `0.8.5` | Extended commands: EXISTS, DEL, INCR, DECR, LPUSH, RPUSH | in-session | done |
| 8.6 | `0.8.6` | Native image, integration tests & manual verification | in-session | done |
| 8.7 | `0.8.7` | Redis server article write-up | in-session | done |

### Batch 8.1 — Module scaffold & RESP codec

**Delegation:** in-session
**Decisions:** D-8.1, D-8.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:redis-server`)
- `challenges/redis-server/build.gradle.kts` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/RespValue.kt` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/RespParser.kt` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/RespSerializer.kt` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/Main.kt` (create — stub)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/RespParserTest.kt` (create)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/RespSerializerTest.kt` (create)

**Work:**
1. Add `challenges:redis-server` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-redis`.
3. Define `RespValue` sealed interface with 5 subtypes: SimpleString, Error, Integer, BulkString (nullable value), Array (nullable elements).
4. Implement `RespParser.parse(input: InputStream): RespValue` — reads first byte to determine type, parses accordingly. Reads until CRLF for simple types, uses length prefix for bulk strings, recurses for arrays.
5. Implement `RespSerializer.serialize(value: RespValue, output: OutputStream)` — writes RESP format.
6. Write parser tests: all 5 types, null bulk string, null array, nested arrays, command arrays (e.g., `*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n`).
7. Write serializer tests: all 5 types round-trip with parser.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all RESP codec tests pass.
- Can parse `*1\r\n$4\r\nPING\r\n` into `Array([BulkString("PING")])`.
- Serialize → parse round-trip produces identical values.

**Acceptance:** RESP codec correctly handles all 5 protocol types.

### Batch 8.2 — Data store & PING/ECHO/SET/GET commands

**Delegation:** in-session
**Decisions:** D-8.3, D-8.4
**Depends on:** Batch 8.1
**Files:**
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/DataStore.kt` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/CommandHandler.kt` (create)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/CommandHandlerTest.kt` (create)

**Work:**
1. Implement `DataStore` with `ConcurrentHashMap<String, StoreEntry>`. StoreEntry holds: value (sealed: StringValue or ListValue), optional expiresAt (Long, millis since epoch). Methods: `get(key)`, `set(key, value, expiresAt?)`, `delete(key)`, `exists(key)`.
2. Implement `CommandHandler.execute(command: List<String>): RespValue`. Dispatch on command name (case-insensitive). PING → `+PONG`, ECHO → bulk string, SET → store and return `+OK`, GET → bulk string or null.
3. Write tests: PING, PING with message, ECHO, SET/GET, GET nonexistent key returns null, wrong argument count returns error.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all command handler tests pass.
- SET then GET returns the stored value.
- GET on missing key returns null bulk string.

**Acceptance:** Core commands work against the data store.

### Batch 8.3 — TCP server & concurrent clients

**Delegation:** in-session
**Decisions:** D-8.5
**Depends on:** Batch 8.2
**Files:**
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/RedisServer.kt` (create)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/Main.kt` (modify)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/RedisServerTest.kt` (create)

**Work:**
1. Implement `RedisServer(port: Int, store: DataStore)`. Accept loop spawns virtual thread per client. Each client thread: parse RESP command → execute via CommandHandler → serialize and write response → loop until disconnect.
2. Handle client disconnects gracefully (EOF on input).
3. Implement `main()` with `--port` flag (default 6379). Shutdown hook for graceful stop.
4. Write integration tests: start server on random port, connect with Socket, send raw RESP, verify response bytes. Test: PING, SET/GET, multiple commands on one connection, multiple concurrent clients.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all server tests pass.
- Multiple clients can connect and operate simultaneously.

**Acceptance:** TCP server handles concurrent clients with PING/SET/GET.

### Batch 8.4 — Key expiry (EX/PX/EXAT/PXAT)

**Delegation:** in-session
**Decisions:** D-8.3, D-8.4
**Depends on:** Batch 8.3
**Files:**
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/CommandHandler.kt` (modify)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/DataStore.kt` (modify)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/ExpiryTest.kt` (create)

**Work:**
1. Extend SET command parsing: `SET key value [EX seconds] [PX milliseconds] [EXAT timestamp] [PXAT timestamp]`. Parse options, compute absolute expiry time in millis.
2. Implement lazy expiry in DataStore: on `get()` and `exists()`, check if entry is expired. If so, remove and return null/false.
3. Write tests: SET with EX, verify GET returns value before expiry, verify GET returns null after expiry (use short TTLs like 100ms + Thread.sleep). Test all four expiry options.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all expiry tests pass.
- Key expires after TTL.

**Acceptance:** SET expiry options work, keys expire lazily on access.

### Batch 8.5 — Extended commands: EXISTS, DEL, INCR, DECR, LPUSH, RPUSH

**Delegation:** in-session
**Decisions:** D-8.3
**Depends on:** Batch 8.3
**Files:**
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/CommandHandler.kt` (modify)
- `challenges/redis-server/src/main/kotlin/dev/gregross/challenges/redis/DataStore.kt` (modify)
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/ExtendedCommandsTest.kt` (create)

**Work:**
1. EXISTS key [key ...] — return integer count of existing keys.
2. DEL key [key ...] — delete keys, return integer count deleted.
3. INCR key — parse value as long, increment, store, return new value. If key doesn't exist, treat as 0. If value isn't an integer, return WRONGTYPE error.
4. DECR key — same as INCR but decrement.
5. LPUSH key value [value ...] — create list if not exists, prepend values, return new length. If key exists but isn't a list, return WRONGTYPE error.
6. RPUSH key value [value ...] — same but append.
7. Write tests for each command including error cases.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all extended command tests pass.
- INCR on nonexistent key returns 1.
- LPUSH/RPUSH create and grow lists correctly.

**Acceptance:** All 11 commands implemented and tested.

### Batch 8.6 — Native image, integration tests & manual verification

**Delegation:** in-session
**Decisions:** D-8.6
**Depends on:** Batches 8.4, 8.5
**Files:**
- `challenges/redis-server/src/test/kotlin/dev/gregross/challenges/redis/IntegrationTest.kt` (create)

**Work:**
1. Write full integration tests: start server, connect via TCP, exercise all 11 commands via raw RESP protocol.
2. Build native image: `./gradlew :challenges:redis-server:nativeCompile`.
3. Install: `./gradlew :challenges:redis-server:install`.
4. Verify symlink at `/usr/local/bin/gig-redis`.
5. Present manual verification commands using `redis-cli`.

**Test criteria:**
- `./gradlew :challenges:redis-server:test` — all tests pass.
- `gig-redis --port 6380` starts and accepts connections.
- `redis-cli -p 6380 PING` → `PONG`.
- `redis-cli -p 6380 SET foo bar` → `OK`, `redis-cli -p 6380 GET foo` → `"bar"`.

**Acceptance:** Native binary installed as `gig-redis`, user has manually verified with redis-cli.

### Batch 8.7 — Redis server article write-up

**Delegation:** in-session
**Decisions:** D-8.7
**Depends on:** Batch 8.6
**Files:**
- `challenges/redis-server/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template.
2. Focus areas: RESP protocol walkthrough, virtual thread concurrency model, ConcurrentHashMap for thread safety, lazy key expiry, type system for mixed-type storage.

**Test criteria:**
- `challenges/redis-server/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] Server listens on configurable port (default 6379)
- [ ] Handles multiple concurrent clients via virtual threads
- [ ] Parses RESP protocol correctly (all 5 types)
- [ ] PING returns PONG
- [ ] ECHO echoes the message
- [ ] SET/GET store and retrieve string values
- [ ] SET with EX/PX/EXAT/PXAT expires keys correctly
- [ ] EXISTS returns count of existing keys
- [ ] DEL removes keys and returns count
- [ ] INCR/DECR increment/decrement integer values
- [ ] LPUSH/RPUSH create and modify lists
- [ ] Proper error responses for wrong types and argument counts
- [ ] Compatible with `redis-cli`
- [ ] Native binary installed as `gig-redis`
- [ ] Article written at `challenges/redis-server/ARTICLE.md`

**Completion triggers Phase 9 → version `0.9.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
