# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 4 — Cut Tool (v0.4.x)

> Build a Unix-style cut tool from scratch — field extraction by delimiter, custom delimiters, multiple field selection, stdin support, and pipeline composition — all in Kotlin with no external dependencies. Deliver as `gig-cut` native binary with article write-up.

**Decisions:** D-4.1, D-4.2, D-4.3, D-4.4, D-4.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 4.1 | `0.4.1` | Module scaffold & test files | in-session | pending |
| 4.2 | `0.4.2` | CutProcessor, CLI & tests | in-session | pending |
| 4.3 | `0.4.3` | Native image, integration tests & manual verification | in-session | pending |
| 4.4 | `0.4.4` | Cut tool article write-up | in-session | pending |

### Batch 4.1 — Module scaffold & test files

**Delegation:** in-session
**Decisions:** D-4.4
**Files:**
- `settings.gradle.kts` (modify — add `challenges:cut-tool`)
- `challenges/cut-tool/build.gradle.kts` (create)
- `challenges/cut-tool/src/test/resources/sample.tsv` (create — from challenge download)
- `challenges/cut-tool/src/test/resources/fourchords.csv` (create — from challenge download)

**Work:**
1. Add `challenges:cut-tool` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-cut`.
3. Download challenge test files (`sample.tsv`, `fourchords.csv`).

**Test criteria:**
- `./gradlew :challenges:cut-tool:build` compiles.
- Test files exist in correct directories.

**Acceptance:** Module compiles, test files ready.

### Batch 4.2 — CutProcessor, CLI & tests

**Delegation:** in-session
**Decisions:** D-4.1, D-4.2, D-4.3, D-4.4
**Depends on:** Batch 4.1
**Files:**
- `challenges/cut-tool/src/main/kotlin/dev/gregross/challenges/cut/CutProcessor.kt` (create)
- `challenges/cut-tool/src/main/kotlin/dev/gregross/challenges/cut/Main.kt` (create)
- `challenges/cut-tool/src/test/kotlin/dev/gregross/challenges/cut/CutProcessorTest.kt` (create)
- `challenges/cut-tool/src/test/kotlin/dev/gregross/challenges/cut/IntegrationTest.kt` (create)

**Work:**
1. Implement `CutProcessor` with `process(input: InputStream, output: PrintStream)` method.
   - Split each line on delimiter, select 1-indexed fields, join with delimiter.
   - Lines without the delimiter pass through unchanged.
   - Handle missing fields gracefully (skip them).
2. Implement `main()` with argument parsing per D-4.3.
   - Parse `-f` (attached or separate, comma-separated field list).
   - Parse `-d` (attached or separate, single character, default tab).
   - Remaining args are file paths; no files = stdin.
3. Write processor unit tests: single field, multiple fields, custom delimiter, tab default, missing fields, lines without delimiter.
4. Write integration tests using `sample.tsv` and `fourchords.csv`.

**Test criteria:**
- `./gradlew :challenges:cut-tool:test` — all tests pass.
- Tab-delimited field extraction matches challenge Step 1 output.
- CSV field extraction with `-d,` matches challenge Step 2 output.
- Multiple fields (`-f1,2`) matches challenge Step 3 output.

**Acceptance:** CutProcessor handles all field extraction cases. CLI parses arguments correctly.

### Batch 4.3 — Native image, integration tests & manual verification

**Delegation:** in-session
**Decisions:** D-4.2
**Depends on:** Batch 4.2
**Files:**
- (no new files — build and install)

**Work:**
1. Build native image: `./gradlew :challenges:cut-tool:nativeCompile`.
2. Install: `./gradlew :challenges:cut-tool:install`.
3. Present manual verification commands for user to run.

**Test criteria:**
- `gig-cut -f2 sample.tsv` → prints second field of each line.
- `gig-cut -f1 -d, fourchords.csv | head -n5` → prints first field of CSV.
- `echo "a,b,c" | gig-cut -f2 -d,` → prints `b`.
- `tail -n5 fourchords.csv | gig-cut -d, -f1,2` → works from stdin pipeline.

**Acceptance:** Native binary installed as `gig-cut`, user has manually verified.

### Batch 4.4 — Cut tool article write-up

**Delegation:** in-session
**Decisions:** D-4.5
**Depends on:** Batch 4.3
**Files:**
- `challenges/cut-tool/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template using gig artifacts.
2. Focus areas: Unix philosophy (do one thing well), line-oriented processing simplicity, delimiter edge cases, pipeline composition examples.

**Test criteria:**
- `challenges/cut-tool/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-cut -f2 sample.tsv` extracts the correct field
- [ ] `-d` flag changes delimiter (default tab)
- [ ] `-f1,2` selects multiple fields
- [ ] Reads from stdin when no file argument given
- [ ] Works in pipelines (`tail | gig-cut | wc`)
- [ ] Lines without delimiter pass through unchanged
- [ ] Exit code 0 for success, 1 for error
- [ ] Native binary installed as `gig-cut`
- [ ] Article written at `challenges/cut-tool/ARTICLE.md`

**Completion triggers Phase 5 → version `0.5.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
