# Building My Own Grep: Line-by-Line Pattern Matching in Kotlin

> A Unix-like grep implementation using JVM regex — line-by-line processing, recursive directory traversal, flag handling, binary file detection, and proper exit code semantics.

## The Challenge

**Source:** [Coding Challenges - Build Your Own grep](https://codingchallenges.fyi/challenges/challenge-grep)

Grep is one of the most-used Unix tools. It scans input line by line, prints lines that match a pattern, and exits with a code that tells you whether anything matched. It sounds simple until you start listing what it actually does: regex pattern matching, recursive directory search, case-insensitive mode, inverted matching, multi-file output with filename prefixes, binary file detection, and stdin support.

This challenge walks through building grep incrementally — starting with empty patterns that match everything, then single characters, recursive search, inverted matches, character classes, anchors, and case insensitivity.

## Approach

Three components, each with a single responsibility:

1. **Matcher** — wraps a compiled regex with flag support (case-insensitive, invert)
2. **GrepProcessor** — reads input line by line, applies the matcher, formats output
3. **FileSearcher** — resolves paths to files, walks directories recursively

The CLI wires them together: parse flags, build a Matcher, hand files to the processor.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Regex engine | JVM `kotlin.text.Regex` | Challenge uses standard patterns (`\d`, `\w`, `^`, `$`, `[abc]`); JVM regex handles all of them natively |
| Architecture | Three-class pipeline | Each class is independently testable; Matcher doesn't know about files, FileSearcher doesn't know about patterns |
| Binary detection | Null byte check in first 8KB | Simple heuristic; matches what real grep does (avoid printing garbage to terminal) |
| Exit codes | 0 = match, 1 = no match, 2 = error | Standard grep convention; scripts rely on these codes |
| Filename prefix | Auto-enable for multi-file or recursive | Matches real grep behavior; single-file mode stays clean |

## The Matcher

The simplest component. It compiles the user's pattern into a JVM `Regex` and checks if any part of a line matches:

```kotlin
class Matcher(
    pattern: String,
    ignoreCase: Boolean = false,
    private val invert: Boolean = false,
) {
    private val regex: Regex = if (ignoreCase) {
        Regex(pattern, RegexOption.IGNORE_CASE)
    } else {
        Regex(pattern)
    }

    fun matches(line: String): Boolean {
        val found = regex.containsMatchIn(line)
        return if (invert) !found else found
    }
}
```

Using `containsMatchIn` rather than `matches` is important — grep searches for the pattern *anywhere* in the line, not as a full-line match. `Matcher("hello")` should match the line `"say hello world"`.

The invert flag flips the result. Combined with case-insensitive mode, this gives four matching modes from two boolean flags.

### What the JVM Regex Gives Us for Free

The challenge steps map directly to regex features:

| Challenge Step | Pattern | JVM Regex Feature |
|----------------|---------|-------------------|
| Empty pattern | `""` | Empty regex matches everything |
| Single char | `"x"` | Literal matching |
| Digit class | `\d` | Built-in character class |
| Word class | `\w` | Built-in character class |
| Start anchor | `^start` | Anchor metacharacter |
| End anchor | `end$` | Anchor metacharacter |
| Character class | `[abc]` | Character class syntax |
| Negated class | `[^abc]` | Negated character class |
| Case insensitive | `-i` flag | `RegexOption.IGNORE_CASE` |

By delegating to JVM regex, the Matcher is 15 lines of code and handles all these patterns. The trade-off is that we're not building a regex engine from scratch — but the challenge is about building grep, not building a regex engine.

## Line-by-Line Processing

The GrepProcessor reads input one line at a time, applies the matcher, and writes matching lines to the output stream:

```kotlin
fun processStream(
    input: InputStream,
    output: PrintStream,
    filename: String? = null,
    showFilename: Boolean = false,
): Boolean {
    var matched = false
    input.bufferedReader().forEachLine { line ->
        if (matcher.matches(line)) {
            matched = true
            if (showFilename && filename != null) {
                output.println("$filename:$line")
            } else {
                output.println(line)
            }
        }
    }
    return matched
}
```

The return value — whether *any* line matched — drives the exit code. This is how shell scripts use grep: `if grep -q pattern file; then ...`.

### Filename Prefix

When searching multiple files or recursively, grep prefixes each matching line with the filename and a colon: `path/to/file.txt:matching line`. The processor accepts `showFilename` as a parameter; the CLI enables it automatically when there are multiple files or the `-r` flag is set.

### Binary File Detection

Grep should not dump binary content to the terminal. The detection is simple: read the first 8KB and check for null bytes. If any are found, skip the file:

```kotlin
private fun isBinary(file: File): Boolean {
    val bytes = file.inputStream().use { it.readNBytes(8192) }
    return bytes.any { it == 0.toByte() }
}
```

This is the same heuristic GNU grep uses. Text files don't contain null bytes; binary files almost always do.

## Recursive Directory Traversal

The FileSearcher resolves user-provided paths into a sorted list of files:

```kotlin
fun findFiles(paths: List<String>, recursive: Boolean): List<File> {
    val files = mutableListOf<File>()
    for (path in paths) {
        val file = File(path)
        if (file.isFile) {
            files.add(file)
        } else if (file.isDirectory) {
            if (recursive) {
                file.walkTopDown()
                    .filter { it.isFile }
                    .forEach { files.add(it) }
            } else {
                System.err.println("gig-grep: $path: Is a directory")
            }
        }
    }
    return files.sortedBy { it.path }
}
```

Sorting by path ensures deterministic output — important for testing and for users who expect predictable ordering. `walkTopDown()` handles arbitrary directory depth.

Without `-r`, passing a directory prints an error to stderr, matching real grep behavior. The file list is built separately from processing, keeping concerns cleanly separated.

## Exit Code Semantics

Grep's exit codes are a contract:

| Code | Meaning | When |
|------|---------|------|
| 0 | Match found | At least one line matched across all files |
| 1 | No match | Pattern searched successfully but nothing matched |
| 2 | Error | Invalid usage, missing files, etc. |

This matters because grep is often used in pipelines and conditionals. `grep -q pattern file && echo "found"` relies on the exit code, not the output. The implementation tracks whether any file produced a match and exits accordingly:

```kotlin
val matched = files.fold(false) { acc, file ->
    processor.processFile(file, System.out, showFilename) || acc
}
exitProcess(if (matched) 0 else 1)
```

## What I Learned

**Grep's simplicity is its power.** The core algorithm is trivial: read line, check pattern, print or skip. Everything else — flags, recursion, filename prefixing, binary detection — is layering behavior around that core loop. The architecture reflects this: Matcher does matching, GrepProcessor does the loop, FileSearcher finds files.

**Exit codes are an API.** They're not just for human convenience — they're how grep communicates with other programs. Getting them right (0 for match, 1 for no match) is as important as printing the right output.

**JVM regex is a pragmatic choice.** Building a regex engine would be educational, but the challenge is about grep's *behavior* — how it processes files, handles flags, formats output. Delegating pattern matching to a battle-tested engine lets the implementation focus on what makes grep grep.

**Binary detection doesn't need to be perfect.** The null-byte heuristic is simple and covers the vast majority of cases. A text file with embedded null bytes is rare enough that the simple check is the right engineering trade-off.

**Separation of concerns pays off in testing.** Matcher tests don't need files. Processor tests don't need directories. FileSearcher tests don't need patterns. Integration tests verify the wiring. Each layer can be tested and debugged independently.
