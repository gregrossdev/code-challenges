# Building My Own Redis Server: RESP Protocol & Virtual Threads in Kotlin

> Implementing a Redis-compatible server from scratch — RESP protocol parsing, TCP socket server with virtual thread per client, ConcurrentHashMap-backed key-value store with lazy expiry, and 11 commands covering strings, integers, lists, and key operations — all in Kotlin using only JVM stdlib.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Redis Server](https://codingchallenges.fyi/challenges/challenge-redis)

Redis is one of the most widely used infrastructure components in modern systems — caching, session storage, message queues, rate limiting. Under the hood, it's surprisingly simple: a single-threaded server speaking a text-based protocol over TCP, storing everything in memory.

This challenge asks you to build a Redis-compatible server that can handle real `redis-cli` connections. It's the most complex challenge yet — combining networking (TCP server), protocol parsing (RESP), concurrent client handling (virtual threads), and data structure management (strings, lists, expiry).

## Usage

```bash
gig-redis [--port PORT]
```

| Flag | Default | Description |
|------|---------|-------------|
| `--port` | `6379` | Listening port |

Supports PING, ECHO, SET (with EX/PX/EXAT/PXAT), GET, EXISTS, DEL, INCR, DECR, LPUSH, RPUSH.

```bash
gig-redis
gig-redis --port 6380

# Test with netcat
echo -ne '*1\r\n$4\r\nPING\r\n' | nc localhost 6379
```

## Approach

Four components with clean separation: a **RESP codec** (parser + serializer) for protocol handling, a **command handler** for dispatching and executing commands, a **data store** for thread-safe in-memory storage, and a **TCP server** that ties it all together with one virtual thread per client connection.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Protocol | Byte-by-byte RESP parsing with sealed interface | Direct mapping to spec; type-safe dispatching |
| Concurrency | Virtual thread per client (Java 21) | Blocking I/O with async performance; proven in load balancer |
| Storage | ConcurrentHashMap with StoreEntry wrapper | Thread-safe without explicit locking; supports mixed types |
| Expiry | Lazy on access (check timestamp on get/exists) | No background thread needed; simple and correct |
| Type system | StoreValue sealed interface (StringValue, ListValue) | Type-safe; clean WRONGTYPE error handling |

## The RESP Protocol

RESP (Redis Serialization Protocol) is elegantly simple. Five data types, each identified by a single prefix byte:

| Prefix | Type | Example | Use |
|--------|------|---------|-----|
| `+` | Simple String | `+OK\r\n` | Command acknowledgements |
| `-` | Error | `-ERR unknown command\r\n` | Error responses |
| `:` | Integer | `:42\r\n` | Counts, INCR results |
| `$` | Bulk String | `$5\r\nhello\r\n` | Values, binary-safe data |
| `*` | Array | `*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n` | Commands, multi-value responses |

Clients always send commands as arrays of bulk strings. The server responds with whichever type is appropriate.

### The Sealed Interface

```kotlin
sealed interface RespValue {
    data class SimpleString(val value: String) : RespValue
    data class Error(val message: String) : RespValue
    data class Integer(val value: Long) : RespValue
    data class BulkString(val value: String?) : RespValue   // null = $-1
    data class Array(val elements: List<RespValue>?) : RespValue  // null = *-1
}
```

Nullable fields handle Redis's null representations: `$-1\r\n` for absent values and `*-1\r\n` for absent arrays. The sealed interface ensures every response path handles all types.

### Parsing

The parser reads byte-by-byte, dispatching on the first character:

```kotlin
fun parse(): RespValue {
    val type = input.read()
    return when (type.toChar()) {
        '+' -> RespValue.SimpleString(readLine())
        '-' -> RespValue.Error(readLine())
        ':' -> RespValue.Integer(readLine().toLong())
        '$' -> parseBulkString()   // read length, then exact bytes
        '*' -> parseArray()         // read count, then recurse N times
    }
}
```

Bulk strings use length-prefixed reading — read the length, then read exactly that many bytes. This makes the protocol binary-safe. Arrays recurse: read the count, then call `parse()` that many times.

## Thread-Safe Storage

The data store wraps `ConcurrentHashMap` with a `StoreEntry` that carries both the value and an optional expiry timestamp:

```kotlin
data class StoreEntry(val value: StoreValue, val expiresAt: Long?)

sealed interface StoreValue {
    data class StringValue(val data: String) : StoreValue
    data class ListValue(val data: MutableList<String>) : StoreValue
}
```

Two key design choices:

**Lazy expiry:** Instead of a background thread scanning for expired keys, we check on access. When `get()` or `exists()` finds an expired entry, it removes it and returns null. This is how real Redis handles most expiry too — active expiry is just an optimization on top.

**Type discrimination via sealed interface:** Redis stores different types under the same key namespace. When you `SET foo bar` then `LPUSH foo x`, Redis returns a WRONGTYPE error. Our sealed interface makes this check a simple `when` expression — if the stored value is a `ListValue` and you're running `GET`, return the error.

## Virtual Thread Concurrency

The server accepts connections on a `ServerSocket` and spawns a virtual thread per client:

```kotlin
val client = socket.accept()
executor.submit {
    client.use { s ->
        while (running.get()) {
            val request = RespParser(s.getInputStream()).parse()
            val args = extractCommand(request)
            val response = handler.execute(args)
            RespSerializer(s.getOutputStream()).serialize(response)
        }
    }
}
```

Each client gets a dedicated thread that blocks on I/O — reading the next command, writing the response. With virtual threads, this scales to thousands of connections without the overhead of OS threads. The `ConcurrentHashMap` handles thread safety for the shared data store.

This is the same pattern from the load balancer challenge, proving it works well for TCP servers in general.

## Command Dispatch

The command handler is a straightforward dispatcher:

```kotlin
fun execute(args: List<String>): RespValue {
    val command = args[0].uppercase()
    return when (command) {
        "PING" -> handlePing(params)
        "SET"  -> handleSet(params)
        "GET"  -> handleGet(params)
        "INCR" -> handleIncr(params)
        // ...
        else -> RespValue.Error("ERR unknown command '$command'")
    }
}
```

Each handler validates argument count, checks types, performs the operation, and returns the appropriate RESP response. Error handling is explicit — wrong argument counts, type mismatches, and invalid values all return descriptive RESP errors.

## What I Learned

**RESP is a masterclass in protocol design.** Five types, one prefix byte each, CRLF-terminated. It's human-readable (you can telnet to a Redis server), binary-safe (bulk strings use length prefixes), and trivial to parse. The entire parser is under 50 lines.

**ConcurrentHashMap is the right abstraction for a concurrent data store.** No explicit locks, no synchronized blocks. Each operation is atomic at the key level, which is exactly the granularity Redis needs. The sealed StoreValue type adds type safety on top.

**Lazy expiry is surprisingly practical.** No background threads, no timers, no priority queues. Just check the timestamp when you access the key. The trade-off — expired keys linger in memory until accessed — is acceptable for this challenge. Real Redis adds periodic random sampling on top, but lazy expiry is the foundation.

**Virtual threads make TCP servers trivial.** The entire server architecture is "accept connection, spawn thread, block on I/O." No event loops, no callbacks, no async/await. The JVM handles the scheduling. This same pattern has now worked across two challenges (load balancer and Redis server).

**Sealed interfaces keep appearing as the right tool.** This is the fourth challenge using them — JSON tokens, Huffman nodes, calculator tokens, and now RESP values. When you have a fixed set of variants that need exhaustive handling, sealed interfaces eliminate entire categories of bugs.
