# Building My Own Cat: File Concatenation & Line Numbering in Kotlin

> A cat tool built from streaming I/O — sequential file reading, stdin support, line numbering with blank-line awareness, and multi-file concatenation with persistent line counters.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Cat Tool](https://codingchallenges.fyi/challenges/challenge-cat)

`cat` is one of the simplest and most frequently used Unix tools. It reads files sequentially and writes them to stdout. Despite its simplicity, it touches fundamental concepts: streaming I/O, stdin vs file reading, and the Unix philosophy of doing one thing well.

The challenge walks through building it step by step: reading a single file, reading from stdin, concatenating multiple files, and adding line numbering with two modes — number all lines (`-n`) and number only non-blank lines (`-b`).

## Usage

```bash
gig-cat [OPTIONS] [FILE...]
```

| Flag | Description |
|------|-------------|
| `-n` | Number all output lines |
| `-b` | Number non-blank lines only (overrides `-n`) |
| `-` | Read from standard input |

```bash
# Output a file
gig-cat file.txt

# Concatenate multiple files
gig-cat file1.txt file2.txt file3.txt

# Read from stdin
echo "hello" | gig-cat -
echo "hello" | gig-cat

# Number all lines
gig-cat -n file.txt

# Number non-blank lines only
gig-cat -b file.txt
```

## Approach

Two components — that's all cat needs:

1. **CatProcessor** — reads from an InputStream, optionally numbers lines, writes to an OutputStream
2. **Main** — parses flags, iterates over file arguments, feeds each to the processor

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Line number format | `%6d\t` (GNU cat format) | 6-digit right-justified, tab-separated — matches standard behavior |
| `-b` vs `-n` | `-b` overrides `-n` | GNU cat convention; `-b` is the more specific flag |
| No args | Read stdin | Standard Unix tool behavior |
| Missing file | Print error, continue | Process remaining files rather than aborting |

## Streaming with Line Numbering

The processor reads line by line and applies numbering based on the active mode:

```kotlin
class CatProcessor(
    private val numberAll: Boolean = false,
    private val numberNonBlank: Boolean = false,
) {
    private var lineNumber = 0

    fun process(input: InputStream, output: PrintStream) {
        input.bufferedReader().forEachLine { line ->
            if (numberNonBlank) {
                if (line.isNotEmpty()) {
                    lineNumber++
                    output.println(String.format("%6d\t%s", lineNumber, line))
                } else {
                    output.println()
                }
            } else if (numberAll) {
                lineNumber++
                output.println(String.format("%6d\t%s", lineNumber, line))
            } else {
                output.println(line)
            }
        }
    }
}
```

The key detail: `lineNumber` is an instance field, not a local variable. When processing multiple files, the counter persists — `gig-cat -n file1.txt file2.txt` numbers continuously across both files, not restarting at 1 for the second file. This matches GNU cat behavior.

The `-b` flag skips blank lines in the numbering count. Blank lines still appear in the output — they just don't get a number. This is useful for reading source code where blank lines separate logical sections but shouldn't waste line numbers.

## Stdin and File Sources

The CLI treats `-` as "read from stdin" and defaults to stdin when no arguments are given:

```kotlin
if (files.isEmpty()) files.add("-")

for (file in files) {
    if (file == "-") {
        processor.process(System.`in`, System.out)
    } else {
        val f = File(file)
        if (!f.exists()) {
            System.err.println("gig-cat: $file: No such file or directory")
            hasError = true
            continue
        }
        FileInputStream(f).use { input ->
            processor.process(input, System.out)
        }
    }
}
```

Both stdin and files are `InputStream` — the processor doesn't know or care which one it's reading from. This is the Unix composability pattern: tools that read from streams work equally well with files, pipes, and redirects.

When a file doesn't exist, the tool prints an error and continues with the remaining files rather than aborting. This matches GNU cat — `cat missing.txt existing.txt` still outputs `existing.txt`.

## What I Learned

**The simplest tools teach the most about design.** Cat's entire implementation is one class with one method. But the decisions — persistent line counter, `-b` overriding `-n`, error-and-continue for missing files — each reflect a design choice that matters for real usage.

**InputStream is the universal interface.** Files, stdin, pipes, network sockets — they all implement InputStream. Writing the processor against InputStream means it works with any source without modification. This is why Unix tools compose so well.

**Line numbering has two legitimate modes.** `-n` numbers everything, which is useful for referencing specific lines. `-b` skips blanks, which is useful for reading structured text where blank lines are separators, not content. Both exist because both are genuinely useful.

**Error handling in pipelines should be lenient.** Aborting on the first missing file would prevent processing the rest. Printing an error and continuing lets the user see partial results and know what failed. The exit code still reflects the error.
