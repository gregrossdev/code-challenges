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

## 2026-03-11 — Architecture: How should the uniq tool be structured?

**Decision:** Two components: (1) **UniqProcessor** — reads input line-by-line, groups adjacent identical lines, applies filters (repeated/unique), formats output (with optional count prefix). Method `process(input: InputStream, output: PrintStream)`. (2) **Main.kt** — parses flags and positional args, wires processor, handles stdin/file input and stdout/file output.
**Rationale:** Uniq is simpler than grep — no file traversal, no pattern matching. A single processor class handles the core logic. Follows the established CutProcessor/GrepProcessor pattern. No need for a separate Matcher or FileSearcher since uniq only compares adjacent lines.
**Alternatives considered:** Three classes (LineGrouper + Filter + Formatter) — over-engineered for adjacent-line comparison. Single main function — harder to test.
**Status:** ACTIVE
**ID:** D-10.1

## 2026-03-11 — Flags: Which uniq flags to support?

**Decision:** Four flags matching the challenge steps: `-c` (prefix with count), `-d` (only repeated lines), `-u` (only unique lines), `-i` (case-insensitive comparison). Plus: support output file as second positional arg, stdin via `-` or no input arg.
**Rationale:** Challenge steps 3-5 require `-c`, `-d`, `-u`. Step 6 requires combining them. Case-insensitive (`-i`) is a natural addition that's trivial to implement and matches real uniq. Step 2 requires stdin and output file support.
**Alternatives considered:** Adding `-f` (skip fields) and `-s` (skip chars) — not required by the challenge, adds parsing complexity without teaching new concepts.
**Status:** ACTIVE
**ID:** D-10.2

## 2026-03-11 — Comparison: How should line comparison work?

**Decision:** Streaming comparison — track `currentLine` and `count`. On each new line: if equal to current (using `equals` or `equals(ignoreCase)` when `-i`), increment count. If different, emit the group (applying filters), then start a new group. Flush the last group at end of input. Never load entire file into memory.
**Rationale:** Uniq only compares adjacent lines, so streaming is natural and memory-efficient. Real uniq works this way. The challenge's test file (`countries.txt`) may be large, reinforcing streaming.
**Alternatives considered:** Collect all lines then group — wastes memory, doesn't match uniq's design intent.
**Status:** ACTIVE
**ID:** D-10.3

## 2026-03-11 — Output format: How should count prefix be formatted?

**Decision:** Right-justify count in a 7-character field followed by a space, matching GNU uniq output format (e.g., `      2 Madrid`). Use `String.format("%7d %s", count, line)`.
**Rationale:** Challenge step 3 shows count-prefixed output. Matching real uniq format ensures `uniq -c countries.txt | wc -l` validation works. The 7-char right-justified format is what GNU coreutils uses.
**Alternatives considered:** Simple `"$count $line"` without padding — diverges from real uniq, could break validation comparisons.
**Status:** ACTIVE
**ID:** D-10.4

## 2026-03-11 — Testing: What test strategy?

**Decision:** Two tiers: (1) **UniqProcessorTest** — unit tests covering all flag combinations: default (deduplicate), `-c` (count), `-d` (repeated only), `-u` (unique only), `-c -d` (count + repeated), `-i` (case-insensitive), empty input, single line, all identical, blank lines. (2) **IntegrationTest** — end-to-end with test fixture files matching challenge validation. Create `test.txt` (small fixture) and `countries.txt` (246 unique lines after dedup per challenge spec).
**Rationale:** Processor tests verify all flag logic in isolation using ByteArrayInputStream. Integration tests verify against challenge-provided data. Two tiers is sufficient — no separate classes to unit-test individually.
**Alternatives considered:** Three tiers (adding CLI flag parsing tests) — flag parsing is simple enough to cover via integration tests.
**Status:** ACTIVE
**ID:** D-10.5

## 2026-03-11 — Content: Article write-up

**Decision:** Write `challenges/uniq/ARTICLE.md` after implementation. Focus on streaming line-by-line processing, adjacent-duplicate semantics vs global dedup, flag combinations, and how uniq fits into Unix pipelines (`sort | uniq`).
**Rationale:** Established convention. Uniq's adjacent-only behavior is a common source of confusion worth explaining.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-10.6
