# Building My Own Uniq: Streaming Adjacent-Duplicate Filtering in Kotlin

> A Unix-like uniq implementation — streaming line-by-line comparison, adjacent-duplicate semantics, count prefixing, repeated/unique filtering, case-insensitive mode, and why `sort | uniq` is a pattern.

## The Challenge

**Source:** [Coding Challenges - Build Your Own uniq Tool](https://codingchallenges.fyi/challenges/challenge-uniq)

Uniq is deceptively simple. It reads input, removes adjacent duplicate lines, and writes the result. That's it — until you start thinking about what "adjacent" means, why it matters, and what the flags do. The challenge walks through building uniq step by step: basic deduplication, stdin support, count prefixing (`-c`), repeated-only filtering (`-d`), unique-only filtering (`-u`), and combining flags.

## Approach

Two components:

1. **UniqProcessor** — streaming adjacent-line grouper with flag-based output
2. **Main.kt** — CLI flag parsing, stdin/file input, stdout/file output

Uniq is simpler than grep or sort — there's no pattern matching, no file traversal, no algorithm selection. The entire tool is one streaming loop. The interesting parts are the semantics, not the complexity.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Architecture | Single processor class + CLI | No need for separate components — uniq's logic is one loop |
| Comparison | Streaming, adjacent-only | Memory-efficient; matches real uniq behavior |
| Count format | `%7d` right-justified | Matches GNU coreutils output format |
| Flags | `-c`, `-d`, `-u`, `-i` | Challenge requirements plus case-insensitive as natural addition |

## The Streaming Loop

The core of uniq is a single pass through the input, tracking the current group:

```kotlin
fun process(input: InputStream, output: PrintStream) {
    var currentLine: String? = null
    var currentCount = 0

    input.bufferedReader().forEachLine { line ->
        if (currentLine == null) {
            currentLine = line
            currentCount = 1
        } else if (linesEqual(currentLine!!, line)) {
            currentCount++
        } else {
            emitGroup(currentLine!!, currentCount, output)
            currentLine = line
            currentCount = 1
        }
    }

    if (currentLine != null) {
        emitGroup(currentLine!!, currentCount, output)
    }
}
```

Two variables — `currentLine` and `currentCount` — are all the state needed. When a new line matches the current one, increment the count. When it doesn't, emit the group and start a new one. Flush the last group after the loop ends.

This streams naturally: memory usage is constant regardless of input size. A 10GB file with millions of lines uses the same memory as a 10-line file.

## Why Adjacent-Only?

The most common misconception about uniq is that it removes all duplicates. It doesn't — it only removes *adjacent* duplicates. This is why the Unix pattern `sort | uniq` exists: sort brings duplicates together, then uniq collapses them.

```bash
# This does NOT remove all duplicates:
echo -e "a\nb\na" | uniq
# Output: a, b, a (three lines — the two "a"s aren't adjacent)

# This does:
echo -e "a\nb\na" | sort | uniq
# Output: a, b (two lines)
```

The adjacent-only design is intentional. It allows uniq to work in constant memory with a single pass. If uniq had to detect all duplicates globally, it would need to store every line it had ever seen — either in memory or using a more complex data structure. The Unix philosophy: do one thing well, and compose tools with pipes.

## Flag Combinations

The flags interact in a specific way:

| Flags | Behavior |
|-------|----------|
| (none) | Print one copy of every group |
| `-c` | Print one copy with count prefix |
| `-d` | Print only groups that appear more than once |
| `-u` | Print only groups that appear exactly once |
| `-c -d` | Print repeated groups with their counts |
| `-c -u` | Print unique groups with count (always 1) |
| `-d -u` | Print nothing (repeated and unique are disjoint sets) |

The filtering logic is clean:

```kotlin
private fun emitGroup(line: String, groupCount: Int, output: PrintStream) {
    val isRepeated = groupCount > 1

    if (repeated && !isRepeated) return
    if (unique && isRepeated) return

    if (count) {
        output.println(String.format("%7d %s", groupCount, line))
    } else {
        output.println(line)
    }
}
```

The `-d` and `-u` flags are filters applied before output. The `-c` flag is a formatter applied during output. This separation means any combination just works — no special-case logic needed.

One subtle behavior: `-d -u` together outputs nothing. Repeated lines fail the unique check; unique lines fail the repeated check. Every group is filtered out. This matches real uniq behavior.

## Count Format

GNU uniq right-justifies the count in a 7-character field:

```
      1 Afghanistan
      2 Brazil
      1 Cambodia
```

This isn't just aesthetics — it makes the output align nicely and is what scripts parsing uniq output expect. `String.format("%7d %s", count, line)` handles this in one call.

## Case-Insensitive Comparison

The `-i` flag changes how lines are compared without changing what's printed:

```kotlin
private fun linesEqual(a: String, b: String): Boolean {
    return if (ignoreCase) a.equals(b, ignoreCase = true) else a == b
}
```

When `-i` is active, `"Hello"`, `"hello"`, and `"HELLO"` are all considered equal. The first occurrence's text is preserved in the output — uniq prints the representative line from each group, not a normalized version.

## Input and Output

Following Unix conventions:

- **No arguments or `-`**: read from stdin
- **First positional arg**: input file
- **Second positional arg**: output file (instead of stdout)

This means uniq fits naturally into pipelines:

```bash
sort data.txt | gig-uniq -c           # stdin → stdout
gig-uniq data.txt                      # file → stdout
gig-uniq data.txt result.txt           # file → file
cat data.txt | gig-uniq - result.txt   # stdin → file
```

The output file support is unusual among Unix tools (most use shell redirection), but it's part of uniq's POSIX specification.

## What I Learned

**Streaming is the natural fit.** Uniq's adjacent-only semantics aren't a limitation — they're what makes constant-memory streaming possible. The design constraint enables the implementation simplicity.

**Flag combinations emerge from orthogonal design.** By making `-d`/`-u` filters and `-c` a formatter, all combinations work without special cases. `-c -d` isn't a separate mode — it's count formatting applied to the repeated-only filter.

**The `sort | uniq` pattern teaches composition.** Uniq deliberately does less so it composes well. This is the Unix philosophy in practice: tools that do one thing can be combined to do many things.

**Output format matters for interop.** The `%7d` count format isn't arbitrary — it's a contract that other tools and scripts depend on. Matching GNU output format means `gig-uniq` is a drop-in replacement in pipelines.

**Simple tools still have edge cases.** Empty input, single lines, all-identical input, blank lines as duplicates, disjoint `-d -u` — each needs handling. The streaming loop covers them all naturally, but the test suite needs to verify each one explicitly.
