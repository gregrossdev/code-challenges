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

## 2026-03-12 — Architecture: How should the cat tool be structured?

**Decision:** Three components: (1) **CatProcessor** — reads from InputStream, writes to OutputStream, applies line numbering options. Processes one source at a time. (2) **Main** — CLI argument parsing (files, `-n`, `-b`, `-` for stdin), iterates sources and feeds them to CatProcessor. (3) Test fixtures for verification. This is a simple pipeline tool — no complex architecture needed.
**Rationale:** Cat is fundamentally simple: read bytes, optionally number lines, write bytes. A single processor class with flag configuration handles everything. Follows the pattern from uniq/grep (processor + CLI).
**Alternatives considered:** Separate Reader/Writer/Formatter classes (over-engineered for cat's simplicity).
**Status:** ACTIVE
**ID:** D-15.1

## 2026-03-12 — Features: What flags and behaviors?

**Decision:** Support: (1) Read one or more files from positional args. (2) Read stdin when `-` is passed or no args given. (3) `-n` flag — number all output lines. (4) `-b` flag — number only non-blank lines (overrides `-n`). Line number format: `%6d\t` (GNU cat format — 6-digit right-justified number followed by tab).
**Rationale:** Challenge Steps 1-5 require file reading, stdin, multiple files, `-n`, and `-b`. GNU cat uses `%6d\t` format. `-b` overrides `-n` per GNU cat behavior.
**Alternatives considered:** Custom line number format (non-standard), supporting more flags like `-s`, `-E` (out of challenge scope).
**Status:** ACTIVE
**ID:** D-15.2

## 2026-03-12 — Testing: What test strategy?

**Decision:** Two tiers: (1) **CatProcessorTest** — single file output, stdin reading, `-n` numbering, `-b` numbering (skips blanks), multiple files concatenated, empty file handling. (2) **IntegrationTest** — launch as subprocess, verify file output, pipe from stdin, multiple files, flag combinations, error on missing file.
**Rationale:** Processor is the core logic. Integration tests verify the full CLI including exit codes and error messages. Simpler challenge = fewer test tiers needed.
**Status:** ACTIVE
**ID:** D-15.3

## 2026-03-12 — Content: Article write-up

**Decision:** Write `challenges/cat/ARTICLE.md` after implementation. Include Usage section per template. Focus on streaming I/O, line numbering with blank-line awareness, stdin vs file reading, and the simplicity of Unix tools.
**Rationale:** Established convention.
**Status:** ACTIVE
**ID:** D-15.4
