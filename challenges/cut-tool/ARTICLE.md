# Building My Own Cut Tool: Field Extraction in Kotlin

> Building a Unix-style cut tool from scratch — delimiter-based field extraction, custom delimiters, multiple field selection, and stdin pipeline support — all in Kotlin with no external dependencies.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Cut Tool](https://codingchallenges.fyi/challenges/challenge-cut)

The `cut` command is one of those Unix utilities you reach for constantly but rarely think about. Need the second column of a TSV? `cut -f2 data.tsv`. Need artist names from a CSV? `cut -f2 -d, music.csv`. It does one thing — extract fields from delimited text — and does it well.

This challenge asks you to build your own version: parse command-line flags, split lines on a delimiter, select specific fields, and play nicely in pipelines. It's a study in the Unix philosophy: small tools with clean interfaces that compose through stdin/stdout.

This is challenge #5 on Coding Challenges, and it's a nice change of pace from the parser and compression tool. No complex data structures, no binary I/O, no algorithms — just clean line-oriented text processing.

## Approach

The core insight: `cut` is fundamentally a line-at-a-time filter. There's no state between lines, no complex data structures, no parsing trees. Each line is independent — split it, pick the fields you want, join them back, print. This simplicity drove the architecture: a single `CutProcessor` class with one method.

The only real complexity is in argument parsing — supporting both attached (`-f2`) and separate (`-f 2`) flag styles, comma-separated field lists, and the interplay between `-f`, `-d`, and file arguments.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Architecture | Single `CutProcessor` class | Matches the tool's simplicity — no need for multiple classes |
| Delimiter default | Tab character | Matches real `cut` behavior |
| Lines without delimiter | Pass through unchanged | Matches GNU cut default (without `-s` flag) |
| Field indexing | 1-indexed | Matches real `cut` convention |
| Missing fields | Skip silently | Graceful handling, matches real `cut` |

## Build Log

### Step 1 — Field Extraction

The processor splits each line on the delimiter, picks the requested fields (converting from 1-indexed to 0-indexed), and joins the selected fields back with the delimiter. Lines that don't contain the delimiter at all are passed through unchanged — this matches GNU cut's default behavior.

### Step 2 — Custom Delimiter

Adding `-d` support was straightforward: the delimiter is just a constructor parameter that defaults to tab. The split and join operations use it consistently.

### Step 3 — Multiple Fields

Supporting `-f1,2,3` meant parsing a comma-separated list of integers from the flag value. Fields are applied in the order specified, and out-of-range fields are silently skipped (requesting field 10 from a 3-column file just gives you fewer output columns).

### Step 4 — CLI and Stdin

The argument parser handles both attached (`-f2`, `-d,`) and separate (`-f 2`, `-d ","`) styles. When no file arguments are given, the processor reads from stdin. This makes pipeline composition work naturally: `tail -n5 data.csv | gig-cut -d, -f1,2`.

## Key Code

### The entire processor

```kotlin
class CutProcessor(
    private val delimiter: Char = '\t',
    private val fields: List<Int>,
) {
    fun process(input: InputStream, output: PrintStream) {
        input.bufferedReader().forEachLine { line ->
            if (delimiter !in line) {
                output.println(line)
            } else {
                val parts = line.split(delimiter)
                val selected = fields
                    .filter { it in 1..parts.size }
                    .map { parts[it - 1] }
                output.println(selected.joinToString(delimiter.toString()))
            }
        }
    }
}
```

The entire core logic in 15 lines. Split, filter, map, join, print. No state, no side effects beyond output. The `filter` step silently handles out-of-range field indices.

### Argument parsing with attached/separate styles

```kotlin
arg.startsWith("-f") -> {
    val value = if (arg.length > 2) arg.substring(2) else {
        i++; args[i]
    }
    fields = parseFields(value)
}
```

This pattern handles both `-f2` (value is part of the flag string) and `-f 2` (value is the next argument). Simple but covers the common CLI conventions.

## Testing

Two tiers: unit tests on the processor, integration tests with the challenge's data files.

| Test Case | Category | Expected | Result |
|-----------|----------|----------|--------|
| Single field with tab | Unit | Correct column | Pass |
| Multiple fields | Unit | Correct columns in order | Pass |
| Custom delimiter (comma) | Unit | Splits on comma | Pass |
| Default delimiter is tab | Unit | Splits on tab | Pass |
| Lines without delimiter | Unit | Passed through unchanged | Pass |
| Missing field index | Unit | Empty output | Pass |
| Out-of-range field | Unit | Skipped silently | Pass |
| Fields joined by delimiter | Unit | Correct separator | Pass |
| sample.tsv field 2 | Integration | f1, 1, 6, 11, 16, 21 | Pass |
| fourchords.csv field 1 | Integration | Song titles | Pass |
| fourchords.csv fields 1,2 | Integration | Title and artist | Pass |
| sample.tsv all fields | Integration | All 5 columns, 6 rows | Pass |
| fourchords.csv row count | Integration | >100 rows | Pass |

Total: 15 tests across processor (9) and integration (6).

## What I Learned

- **Simplicity is its own reward.** After building a JSON parser and compression tool, implementing `cut` felt almost trivial. But that's exactly the point — `cut` does one thing and the code reflects that. The entire processor is 15 lines. Resisting the urge to over-engineer was the real challenge.

- **Argument parsing is always more work than you expect.** The actual field extraction is trivial. The argument parser — handling attached vs separate flag values, comma-separated lists, file arguments mixed with flags — took more code than the processor itself. This is true of most CLI tools.

- **The Unix philosophy works because the interfaces are simple.** `cut` reads lines from stdin or a file, writes lines to stdout. That's it. Because the interface is so simple, it composes naturally: `cat file | cut -f2 | sort | uniq | wc -l`. No configuration, no APIs, no serialization formats — just text and pipes.

## Running It

```bash
# Extract field 2 from a tab-separated file
gig-cut -f2 data.tsv

# Extract fields 1 and 3 from a CSV
gig-cut -f1,3 -d, data.csv

# Read from stdin
echo "a,b,c,d" | gig-cut -f2 -d,

# Pipeline composition
tail -n10 data.csv | gig-cut -d, -f1,2 | sort | uniq
```

---

*Built as part of my [Coding Challenges](https://codingchallenges.fyi) series. Stack: Kotlin + Gradle.*
