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

## 2026-03-11 — Architecture: How should the cut tool be structured?

**Decision:** Simple single-pass line processor. A `CutProcessor` class takes a delimiter and a list of field indices, then processes an `InputStream` line by line — splitting on delimiter, selecting fields, and joining with the delimiter. No complex data model needed.
**Rationale:** `cut` is fundamentally a line-at-a-time filter. There's no parsing tree, no state between lines, no complex data structures. A single class with a `process(input: InputStream, output: PrintStream)` method covers the entire core logic. This is deliberately simpler than the JSON parser or compression tool — matching the tool's simplicity.
**Alternatives considered:** Separate classes for parsing, field selection, and output (over-engineered for this scope), streaming character-by-character (unnecessary, lines fit in memory).
**Status:** ACTIVE
**ID:** D-4.1

## 2026-03-11 — CLI: What flags and behavior should gig-cut support?

**Decision:** Support `-f` for field selection (1-indexed, comma-separated list like `-f1,2,3`) and `-d` for delimiter (single character, defaults to tab). Read from file argument or stdin when no file given. Output selected fields joined by the delimiter. Lines without the delimiter are passed through unchanged. Exit 0 on success, 1 on error.
**Rationale:** Matches the challenge requirements and real `cut` behavior. The `-f` flag accepts comma-separated values which is the standard. Passing through lines without the delimiter matches GNU cut's default behavior (without `-s`). The challenge doesn't require `-c`, `-b`, or `-s` flags.
**Alternatives considered:** Supporting `-c` for character positions (out of scope), requiring `-s` to suppress delimiter-less lines (challenge doesn't ask for it).
**Status:** ACTIVE
**ID:** D-4.2

## 2026-03-11 — Argument Parsing: How should CLI arguments be parsed?

**Decision:** Custom argument parser handling `-f`, `-d`, and file arguments. Support both `-f2` (attached) and `-f 2` (separate) styles. For `-d`, accept `-d,` (attached) and `-d ","` (separate). Remaining non-flag arguments are file paths.
**Rationale:** The challenge examples show `-f2` (attached style) and `-d,` (attached). Real `cut` supports both attached and separate styles. A simple custom parser is straightforward for two flags and avoids adding a CLI library dependency.
**Alternatives considered:** Using a CLI parsing library like clikt (adds dependency, overkill for two flags), only supporting attached style (less flexible).
**Status:** ACTIVE
**ID:** D-4.3

## 2026-03-11 — Testing: What test strategy for the cut tool?

**Decision:** Two tiers: (1) Unit tests for `CutProcessor` — field extraction with tab delimiter, custom delimiter, multiple fields, missing fields, lines without delimiter. (2) Integration tests using the challenge's `sample.tsv` and `fourchords.csv` files — verifying output matches expected results from the challenge steps.
**Rationale:** The core logic is simple enough that unit tests on the processor cover most cases. Integration tests with the provided test files validate end-to-end correctness. No need for the multi-tier approach used in the parser or compression tool.
**Alternatives considered:** Only integration tests (misses edge cases like missing fields), adding argument parser tests (low value, parser is trivial).
**Status:** ACTIVE
**ID:** D-4.4

## 2026-03-11 — Content: Article write-up for cut tool challenge

**Decision:** Write `challenges/cut-tool/ARTICLE.md` after implementation using the established template. Focus on Unix philosophy, the simplicity of line-oriented processing, delimiter handling edge cases, and pipeline composition.
**Rationale:** Established convention. The cut tool is simpler than prior challenges, so the article should emphasize design philosophy (doing one thing well) rather than complex algorithms.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-4.5
