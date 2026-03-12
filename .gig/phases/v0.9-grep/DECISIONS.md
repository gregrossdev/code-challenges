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

## 2026-03-11 — Architecture: How should the grep tool be structured?

**Decision:** Three components: (1) **Matcher** — wraps Kotlin/JVM `Regex` to match patterns against lines, handling flags like case-insensitive and invert. (2) **FileSearcher** — handles file/directory traversal, stdin reading, and recursive search. (3) **GrepProcessor** — orchestrates: reads input, applies matcher, formats output (with optional filename prefix for multi-file/recursive), tracks match status for exit codes. Use JVM's built-in regex engine (easy mode).
**Rationale:** The challenge is about grep behavior (flags, recursion, output formatting, exit codes) not about building a regex engine. Using JVM regex lets us focus on the grep CLI tool itself. Three components separate pattern matching from I/O from orchestration.
**Alternatives considered:** Custom regex engine (hard mode — interesting but doubles scope without teaching new concepts beyond the JSON parser's recursive descent), single monolithic class (harder to test).
**Status:** ACTIVE
**ID:** D-9.1

## 2026-03-11 — Flags: Which grep flags to support?

**Decision:** Three flags matching the challenge steps: `-i` (case-insensitive), `-v` (invert match), `-r` (recursive directory search). Plus implicit behaviors: line-by-line matching, filename prefix when multiple files or recursive mode, exit code 0 (match found), 1 (no match), 2 (error).
**Rationale:** These are the three flags the challenge explicitly requires across its 7 steps. Exit codes are core grep behavior tested by the challenge. Filename prefix in multi-file mode matches standard grep output.
**Alternatives considered:** Adding `-n` (line numbers), `-c` (count), `-l` (files only) — not required by the challenge, would add scope without teaching new concepts.
**Status:** ACTIVE
**ID:** D-9.2

## 2026-03-11 — Patterns: What regex features to support?

**Decision:** Use JVM `Regex` which natively supports all required features: literal strings, `\d` (digits), `\w` (word chars), `^` (start anchor), `$` (end anchor), character classes `[abc]`, negated classes `[^abc]`. Map `\d` and `\w` to JVM regex equivalents (they're the same syntax). Case-insensitive flag maps to `RegexOption.IGNORE_CASE`.
**Rationale:** JVM regex already supports every pattern the challenge requires. No translation layer needed — the patterns are passed directly. This is the "easy mode" the challenge offers.
**Alternatives considered:** Custom regex engine (hard mode), translating patterns to a different format (unnecessary since JVM regex is compatible).
**Status:** ACTIVE
**ID:** D-9.3

## 2026-03-11 — Input: How should file and stdin input work?

**Decision:** `gig-grep [flags] <pattern> [file...]`. No files → read stdin. Single file → output lines only. Multiple files or `-r` → prefix each line with `filename:`. `-r` with no files → search current directory. `-r` with directory argument → search that directory recursively. Skip binary files (files containing null bytes). Follow symlinks during recursion.
**Rationale:** Matches standard grep behavior. Filename prefix in multi-file mode is essential for usability. Binary file skipping prevents garbled output. Stdin support follows established CLI patterns from prior challenges.
**Alternatives considered:** Always showing filename (breaks single-file compatibility), not skipping binary files (ugly output), requiring explicit `.` for recursive (less ergonomic).
**Status:** ACTIVE
**ID:** D-9.4

## 2026-03-11 — Testing: What test strategy for the grep tool?

**Decision:** Three tiers: (1) Matcher tests — pattern matching, case insensitivity, inversion. (2) Processor tests — single file, multiple files, filename prefixing, exit codes, stdin. (3) Integration tests — recursive directory search with test fixture directories, binary file skipping, end-to-end with all flag combinations.
**Rationale:** Matcher tests verify regex behavior in isolation. Processor tests verify output formatting and exit code logic. Integration tests verify recursive traversal and real file I/O.
**Alternatives considered:** Comparing output against real `grep` (flaky across platforms), only integration tests (can't isolate failures).
**Status:** ACTIVE
**ID:** D-9.5

## 2026-03-11 — Content: Article write-up for grep challenge

**Decision:** Write `challenges/grep/ARTICLE.md` after implementation. Focus on how grep works internally — line-by-line processing, exit code semantics, recursive file traversal, and how regex engines match patterns.
**Rationale:** Established convention. Grep is a tool every developer uses daily — understanding its internals is broadly valuable.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-9.6
