# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 15 — Cat Tool (v0.15.x)

> Build a cat tool that reads files sequentially and writes them to stdout — supporting multiple files, stdin via `-`, line numbering with `-n`, and non-blank line numbering with `-b`. Deliver as `gig-cat` native binary with article write-up.

**Decisions:** D-15.1, D-15.2, D-15.3, D-15.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 15.1 | `0.15.1` | Module scaffold, CatProcessor & tests | in-session | done |
| 15.2 | `0.15.2` | CLI, native image & integration tests | in-session | done |
| 15.3 | `0.15.3` | Cat tool article write-up | in-session | done |

### Batch 15.1 — Module scaffold, CatProcessor & tests

**Delegation:** in-session
**Decisions:** D-15.1, D-15.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:cat`)
- `challenges/cat/build.gradle.kts` (create)
- `challenges/cat/src/main/kotlin/dev/gregross/challenges/cat/CatProcessor.kt` (create)
- `challenges/cat/src/main/kotlin/dev/gregross/challenges/cat/Main.kt` (create — stub)
- `challenges/cat/src/test/kotlin/dev/gregross/challenges/cat/CatProcessorTest.kt` (create)
- `challenges/cat/src/test/resources/test.txt` (create)
- `challenges/cat/src/test/resources/test2.txt` (create)

**Work:**
1. Add `challenges:cat` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-cat`.
3. Implement `CatProcessor(numberAll: Boolean, numberNonBlank: Boolean)`. Method `process(input: InputStream, output: PrintStream)` — reads lines, optionally numbers them with `%6d\t` format. Tracks line counter across multiple calls (for multi-file concatenation). `-b` skips blank lines in numbering.
4. Create test fixtures.
5. Write tests: single file output, `-n` numbers all lines, `-b` skips blanks, empty file, line counter persists across files.

**Test criteria:**
- `./gradlew :challenges:cat:test` — all tests pass.
- `-n` produces `     1\tline` format.
- `-b` skips blank lines in numbering.

**Acceptance:** CatProcessor correctly reads and numbers lines with both flags.

### Batch 15.2 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-15.2, D-15.3
**Depends on:** Batch 15.1
**Files:**
- `challenges/cat/src/main/kotlin/dev/gregross/challenges/cat/Main.kt` (modify)
- `challenges/cat/src/test/kotlin/dev/gregross/challenges/cat/IntegrationTest.kt` (create)

**Work:**
1. Implement `main()`: parse flags (`-n`, `-b`), collect file args, process each in order. `-` reads stdin. No args reads stdin. Missing file → error to stderr, continue with next file.
2. Write integration tests: single file, multiple files, stdin via `-`, `-n` flag, `-b` flag, missing file error.
3. Build native image and install.
4. Present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:cat:test` — all tests pass.
- `gig-cat test.txt` outputs file contents.
- `echo hello | gig-cat -` reads from stdin.
- `gig-cat -n test.txt` numbers all lines.

**Acceptance:** Native binary installed as `gig-cat`, user has manually verified.

### Batch 15.3 — Cat tool article write-up

**Delegation:** in-session
**Decisions:** D-15.4
**Depends on:** Batch 15.2
**Files:**
- `challenges/cat/ARTICLE.md` (create)

**Work:**
1. Write article from template.
2. Include Usage section.
3. Focus areas: streaming I/O, line numbering with blank-line awareness, stdin vs file reading, multi-file concatenation.

**Test criteria:**
- `challenges/cat/ARTICLE.md` exists with all sections populated.

**Acceptance:** Complete article with Usage section.

**Phase Acceptance Criteria:**
- [ ] Single file output to stdout
- [ ] Multiple files concatenated in order
- [ ] `-` reads from stdin
- [ ] No args reads from stdin
- [ ] `-n` numbers all lines (`%6d\t` format)
- [ ] `-b` numbers non-blank lines only
- [ ] Missing file prints error, continues with next
- [ ] Native binary installed as `gig-cat`
- [ ] Article written at `challenges/cat/ARTICLE.md` with Usage section

**Completion triggers Phase 16 → version `0.16.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
