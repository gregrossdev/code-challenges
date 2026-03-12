# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 10 — Uniq (v0.10.x)

> Build a Unix-like uniq tool — streaming adjacent-duplicate filtering with count prefix (`-c`), repeated-only (`-d`), unique-only (`-u`), case-insensitive (`-i`) flags, stdin/file input, stdout/file output, and proper GNU-compatible count formatting. Deliver as `gig-uniq` native binary with article write-up.

**Decisions:** D-10.1, D-10.2, D-10.3, D-10.4, D-10.5, D-10.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 10.1 | `0.10.1` | Module scaffold & UniqProcessor | in-session | done |
| 10.2 | `0.10.2` | CLI, native image & integration tests | in-session | done |
| 10.3 | `0.10.3` | Uniq article write-up | in-session | done |

### Batch 10.1 — Module scaffold & UniqProcessor

**Delegation:** in-session
**Decisions:** D-10.1, D-10.2, D-10.3, D-10.4, D-10.5
**Files:**
- `settings.gradle.kts` (modify — add `challenges:uniq`)
- `challenges/uniq/build.gradle.kts` (create)
- `challenges/uniq/src/main/kotlin/dev/gregross/challenges/uniq/UniqProcessor.kt` (create)
- `challenges/uniq/src/main/kotlin/dev/gregross/challenges/uniq/Main.kt` (create — stub)
- `challenges/uniq/src/test/kotlin/dev/gregross/challenges/uniq/UniqProcessorTest.kt` (create)
- `challenges/uniq/src/test/resources/test.txt` (create — small test fixture)

**Work:**
1. Add `challenges:uniq` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-uniq`.
3. Implement `UniqProcessor(count: Boolean, repeated: Boolean, unique: Boolean, ignoreCase: Boolean)`. Method `process(input: InputStream, output: PrintStream)` — streaming adjacent-line grouping with flag-based filtering and output formatting. Count format: `%7d %s`.
4. Create `test.txt` fixture with repeated/unique/mixed lines.
5. Write tests: default dedup, `-c` count prefix, `-d` repeated only, `-u` unique only, `-c -d` combined, `-i` case-insensitive, empty input, single line, all identical, blank lines, `-d -u` outputs nothing.

**Test criteria:**
- `./gradlew :challenges:uniq:test` — all processor tests pass.
- Default mode removes adjacent duplicates.
- `-c` prefixes with right-justified 7-char count.
- `-d` shows only repeated groups; `-u` shows only non-repeated.

**Acceptance:** UniqProcessor correctly handles all flag combinations and edge cases.

### Batch 10.2 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-10.2, D-10.5
**Depends on:** Batch 10.1
**Files:**
- `challenges/uniq/src/main/kotlin/dev/gregross/challenges/uniq/Main.kt` (modify)
- `challenges/uniq/src/test/kotlin/dev/gregross/challenges/uniq/IntegrationTest.kt` (create)
- `challenges/uniq/src/test/resources/countries.txt` (create — challenge test data)

**Work:**
1. Implement `main()`: parse flags (`-c`, `-d`, `-u`, `-i`), extract positional args (input file, output file). Wire up UniqProcessor. Stdin when no input or `-`. Output file when second positional arg given.
2. Create `countries.txt` test fixture (sorted country list with adjacent duplicates — 246 unique lines after dedup per challenge validation).
3. Write integration tests: default dedup on countries.txt yields 246 lines, `-c` shows counts, `-d` shows repeated, `-u` shows unique, `-c -d` combined, stdin input, output to file.
4. Build native image: `./gradlew :challenges:uniq:nativeCompile`.
5. Install: `./gradlew :challenges:uniq:install`.
6. Verify symlink and present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:uniq:test` — all tests pass.
- `gig-uniq countries.txt | wc -l` outputs 246.
- `cat test.txt | gig-uniq -` works via stdin.
- `gig-uniq -c -d countries.txt` shows repeated lines with counts.

**Acceptance:** Native binary installed as `gig-uniq`, user has manually verified.

### Batch 10.3 — Uniq article write-up

**Delegation:** in-session
**Decisions:** D-10.6
**Depends on:** Batch 10.2
**Files:**
- `challenges/uniq/ARTICLE.md` (create)

**Work:**
1. Write article from established template.
2. Focus areas: streaming adjacent-line comparison, why uniq only handles adjacent duplicates (sort | uniq pattern), flag combinations and output filtering, count formatting, stdin/stdout/file I/O in Unix tools.

**Test criteria:**
- `challenges/uniq/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-uniq countries.txt | wc -l` outputs 246
- [ ] `gig-uniq -c countries.txt` shows count-prefixed lines
- [ ] `gig-uniq -d countries.txt` shows only repeated lines
- [ ] `gig-uniq -u countries.txt` shows only unique lines
- [ ] `gig-uniq -c -d countries.txt` shows repeated with counts
- [ ] `gig-uniq -i file` compares case-insensitively
- [ ] Reads from stdin when no file or `-`
- [ ] Writes to output file when second arg given
- [ ] Empty input produces empty output
- [ ] Native binary installed as `gig-uniq`
- [ ] Article written at `challenges/uniq/ARTICLE.md`

**Completion triggers Phase 11 → version `0.11.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
