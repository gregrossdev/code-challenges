# Code Challenges

Implementations of classic Unix tools, network servers, and algorithms — all built from scratch in **Kotlin** with **GraalVM native images**.

Each challenge is a standalone Gradle subproject under `challenges/` with its own tests, native binary, and write-up.

## Challenges

| #  | Challenge | Description |
|----|-----------|-------------|
| 1  | [wc](challenges/wc) | Word, line, byte & character counting |
| 2  | [json-parser](challenges/json-parser) | Recursive-descent JSON lexer & parser |
| 3  | [compression-tool](challenges/compression-tool) | Huffman coding compression |
| 4  | [cut-tool](challenges/cut-tool) | Field extraction (delimiters & byte ranges) |
| 5  | [load-balancer](challenges/load-balancer) | HTTP reverse proxy with health checks |
| 6  | [sort-tool](challenges/sort-tool) | Four sorting algorithms (merge, quick, heap, radix) |
| 7  | [calculator](challenges/calculator) | Shunting-yard expression evaluator |
| 8  | [redis-server](challenges/redis-server) | RESP protocol server with virtual threads |
| 9  | [grep](challenges/grep) | Line-by-line regex pattern matching |
| 10 | [uniq](challenges/uniq) | Streaming adjacent-duplicate filtering |
| 11 | [web-server](challenges/web-server) | HTTP/1.1 server from raw sockets |
| 12 | [url-shortener](challenges/url-shortener) | SHA-256 hashing & REST API from raw sockets |
| 13 | [diff](challenges/diff) | Longest common subsequence diff |
| 14 | [shell](challenges/shell) | REPL with pipes, builtins & job control |
| 15 | [cat](challenges/cat) | File concatenation & line numbering |
| 16 | [irc-client](challenges/irc-client) | Real-time IRC chat over raw TCP |

## Tech Stack

- **Kotlin** on JDK 21
- **GraalVM Native Image** (Oracle GraalVM 24) for AOT-compiled binaries
- **JUnit 5** for testing
- **Gradle** multi-project build with shared conventions

## Getting Started

```bash
# Build & test a specific challenge
./gradlew :challenges:wc:test

# Build native binary
./gradlew :challenges:wc:nativeCompile

# Install to /usr/local/bin (requires sudo)
./gradlew :challenges:wc:install
```

## Project Structure

```
challenges/
  wc/                  # Each challenge is a standalone subproject
    src/main/kotlin/   # Implementation
    src/test/kotlin/   # Tests
    ARTICLE.md         # Write-up explaining the approach
    build.gradle.kts   # Applies shared challenge-conventions plugin
buildSrc/              # Shared Gradle convention plugin
```

Each challenge includes an `ARTICLE.md` with a detailed write-up covering the design, algorithms, and usage.
