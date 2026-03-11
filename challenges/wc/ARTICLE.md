# Building My Own wc: A Word Count Tool in Kotlin

> Recreating the Unix `wc` utility from scratch in Kotlin — counting bytes, lines, words, and characters from files and stdin.

## The Challenge

**Source:** [Coding Challenges - Build Your Own wc](https://codingchallenges.fyi/challenges/challenge-wc)

The `wc` command is one of those Unix tools you use without thinking — pipe some text through it and instantly know how many lines, words, or bytes you're dealing with. It's a small tool with a clear contract, which makes it a perfect first coding challenge.

The challenge asks you to build a clone that supports four flags (`-c` for bytes, `-l` for lines, `-w` for words, `-m` for characters), a sensible default when no flags are given, and the ability to read from either a file or standard input. It's deceptively simple — the interesting parts are in the details: what counts as a "word"? How do bytes differ from characters? How do you handle stdin cleanly?

This is challenge #1 on [Coding Challenges](https://codingchallenges.fyi), and it sets the tone for everything that follows: build real tools, understand what's happening under the hood, and validate against known expected outputs.

## Approach

Before writing any code, I set up the project structure — a Gradle multi-module monorepo where each challenge lives as its own subproject under `challenges/`. A shared convention plugin in `buildSrc` handles Kotlin configuration, JVM 21 toolchain, and JUnit 5 test setup so each new challenge module only needs a few lines of build config.

For the wc tool itself, I kept things deliberately simple. No CLI framework — just raw `args` parsing. The tool only needs to distinguish flags from filenames and handle a handful of options. Reaching for a library like Clikt would be over-engineering at this stage.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Repository structure | Gradle multi-module monorepo | Shared build config, independent modules, scales to 100+ challenges |
| CLI parsing | Raw `args` array | Only 4 flags and 1 positional arg — a framework would be overkill |
| Core design | Single `countFromStream(InputStream)` function | Unifies file and stdin paths; read once, compute all counts |
| Byte vs char counting | Read raw bytes first, then decode to String | Bytes come from the byte array size, chars from the decoded String length |
| Testing | JUnit 5 + kotlin-test against known reference file | Challenge provides exact expected values for a specific test file |

## Build Log

### Step 1 — Project Scaffolding

Set up the Gradle monorepo with a `buildSrc` convention plugin. The plugin configures Kotlin 2.1.20, JVM 21 toolchain, JUnit 5, and the `application` plugin so each challenge can be run directly with `./gradlew :challenges:{name}:run`.

The `wc` module's entire `build.gradle.kts` is just:

```kotlin
plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.wc.MainKt")
}
```

Every future challenge will follow this same minimal pattern.

### Step 2 — Core Counting Logic

The heart of the tool is a single function that takes an `InputStream` and returns all four counts at once:

```kotlin
fun countFromStream(input: InputStream): Counts {
    val content = input.readAllBytes()
    val text = String(content, Charsets.UTF_8)

    return Counts(
        bytes = content.size.toLong(),
        lines = text.count { it == '\n' }.toLong(),
        words = text.trim().split(Regex("\\s+"))
            .let { if (it == listOf("")) 0L else it.size.toLong() },
        chars = text.length.toLong(),
    )
}
```

Reading into a byte array first, then decoding to a String, is the key insight. Bytes and characters are different things — `café` is 5 characters but 6 bytes in UTF-8 because `é` takes 2 bytes. By keeping both the raw byte array and the decoded string, each count uses the right source of truth.

### Step 3 — CLI and stdin Support

The `main()` function splits args into flags and filenames, opens the right input source (file or stdin), and formats the output:

```kotlin
val input = if (files.isNotEmpty()) {
    file.inputStream()
} else {
    System.`in`
}
```

When no flags are specified, the default mirrors real `wc`: lines, words, and bytes (in that order). One subtlety — Gradle's `run` task doesn't forward stdin by default, so the build file needs `standardInput = System.in` on the `JavaExec` task.

## Key Code

### The bytes-vs-characters distinction

This is the most educational part of the challenge. The `-c` flag counts bytes, while `-m` counts characters. For ASCII text they're identical, but for anything with multibyte UTF-8 characters they diverge:

```kotlin
val content = input.readAllBytes()       // raw bytes
val text = String(content, Charsets.UTF_8) // decoded characters

bytes = content.size.toLong()   // byte array length
chars = text.length.toLong()    // string (character) length
```

For the challenge's test file: 342,190 bytes but only 339,292 characters — a difference of 2,898 multibyte characters.

### Word splitting with empty input handling

Kotlin's `split()` on an empty string returns `[""]` rather than an empty list, which would incorrectly count as 1 word. The fix:

```kotlin
words = text.trim().split(Regex("\\s+"))
    .let { if (it == listOf("")) 0L else it.size.toLong() }
```

### Data class for clean return values

Kotlin data classes make returning multiple values natural without tuples or out-parameters:

```kotlin
data class Counts(
    val lines: Long = 0,
    val words: Long = 0,
    val bytes: Long = 0,
    val chars: Long = 0,
)
```

## Testing

Tests run against the challenge's reference `test.txt` file with known expected counts, plus edge cases.

| Test Case | Input | Expected | Result |
|-----------|-------|----------|--------|
| Byte count | test.txt with `-c` | 342190 | Pass |
| Line count | test.txt with `-l` | 7145 | Pass |
| Word count | test.txt with `-w` | 58164 | Pass |
| Char count | test.txt with `-m` | 339292 | Pass |
| Empty input | empty stream | 0/0/0/0 | Pass |
| No trailing newline | `"hello world"` | 0 lines, 2 words | Pass |
| Multibyte chars | `"café\n"` | 5 chars, 6 bytes | Pass |

## What I Learned

- **Bytes and characters are not the same thing.** This is obvious in theory but easy to forget in practice. Reading the raw byte array before decoding forces you to think about encoding, which is exactly the kind of awareness a tool like `wc` should build.

- **Kotlin's standard library covers most of what you need.** `readAllBytes()`, `count {}`, `split(Regex)`, `buildString` — no external dependencies needed for a CLI tool this size. The language is expressive enough that the entire core logic fits in 10 lines.

- **Gradle needs explicit stdin forwarding.** The `JavaExec` task doesn't pass `System.in` through by default. A one-liner fix (`standardInput = System.in`), but easy to miss.

## Running It

```bash
# Count bytes
./gradlew :challenges:wc:run --args="-c path/to/file.txt"

# Count lines
./gradlew :challenges:wc:run --args="-l path/to/file.txt"

# Count words
./gradlew :challenges:wc:run --args="-w path/to/file.txt"

# Count characters
./gradlew :challenges:wc:run --args="-m path/to/file.txt"

# Default (lines + words + bytes)
./gradlew :challenges:wc:run --args="path/to/file.txt"

# Read from stdin
cat file.txt | ./gradlew :challenges:wc:run
```

---

*Built as part of my [Coding Challenges](https://codingchallenges.fyi) series. Stack: Kotlin + Gradle.*
