# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 13 — Diff Tool (v0.13.x)

> Build a diff tool that computes the Longest Common Subsequence between two files and outputs an edit script in classic diff format (`< ` removed, `> ` added, `---` separator). Uses dynamic programming for LCS computation, generates edit scripts from the LCS result, and delivers as `gig-diff` native binary with article write-up.

**Decisions:** D-13.1, D-13.2, D-13.3, D-13.4, D-13.5, D-13.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 13.1 | `0.13.1` | Module scaffold & LcsComputer | in-session | pending |
| 13.2 | `0.13.2` | DiffGenerator & DiffFormatter | in-session | pending |
| 13.3 | `0.13.3` | CLI, native image & integration tests | in-session | pending |
| 13.4 | `0.13.4` | Diff tool article write-up | in-session | pending |

### Batch 13.1 — Module scaffold & LcsComputer

**Delegation:** in-session
**Decisions:** D-13.1, D-13.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:diff`)
- `challenges/diff/build.gradle.kts` (create)
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/LcsComputer.kt` (create)
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/Main.kt` (create — stub)
- `challenges/diff/src/test/kotlin/dev/gregross/challenges/diff/LcsComputerTest.kt` (create)

**Work:**
1. Add `challenges:diff` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-diff`.
3. Implement `LcsComputer`. Method `compute(a: List<String>, b: List<String>): List<String>` — builds DP table (O(mn)), backtracks to extract the LCS. Also support `computeString(a: String, b: String): String` for character-level LCS (challenge Step 1 test cases).
4. Write tests using the challenge's exact test cases: identical strings, disjoint, partial overlap, empty, subsequence. Plus line-level LCS tests.

**Test criteria:**
- `./gradlew :challenges:diff:test` — all tests pass.
- Challenge test cases: `LCS("ABCDEF", "ABCDEF") = "ABCDEF"`, `LCS("ABC", "XYZ") = ""`, `LCS("AABCXY", "XYZ") = "XY"`, `LCS("", "") = ""`, `LCS("ABCD", "AC") = "AC"`.

**Acceptance:** LCS algorithm works correctly for both character and line sequences.

### Batch 13.2 — DiffGenerator & DiffFormatter

**Delegation:** in-session
**Decisions:** D-13.2, D-13.3
**Depends on:** Batch 13.1
**Files:**
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/DiffEntry.kt` (create)
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/DiffGenerator.kt` (create)
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/DiffFormatter.kt` (create)
- `challenges/diff/src/test/kotlin/dev/gregross/challenges/diff/DiffGeneratorTest.kt` (create)
- `challenges/diff/src/test/kotlin/dev/gregross/challenges/diff/DiffFormatterTest.kt` (create)

**Work:**
1. Create `DiffEntry` — sealed interface or enum with COMMON, ADDED, REMOVED variants, each holding a line string.
2. Implement `DiffGenerator`. Method `generate(original: List<String>, modified: List<String>): List<DiffEntry>` — computes LCS, then walks both input arrays and the LCS to classify each line as common, added, or removed.
3. Implement `DiffFormatter`. Method `format(entries: List<DiffEntry>): String` — outputs `< ` for REMOVED, `> ` for ADDED, `---` between consecutive remove/add groups. Common lines are not printed.
4. Write tests: identical files (empty output), all lines removed, all lines added, single modification, mixed insertions/deletions/modifications, multiple change groups with `---` separators.

**Test criteria:**
- `./gradlew :challenges:diff:test` — all tests pass.
- Identical files produce no output.
- Modifications produce `< old` / `---` / `> new` groups.

**Acceptance:** Diff generation and formatting produce correct classic diff output.

### Batch 13.3 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-13.4, D-13.5
**Depends on:** Batch 13.2
**Files:**
- `challenges/diff/src/main/kotlin/dev/gregross/challenges/diff/Main.kt` (modify)
- `challenges/diff/src/test/kotlin/dev/gregross/challenges/diff/IntegrationTest.kt` (create)
- `challenges/diff/src/test/resources/original.txt` (create — test fixture)
- `challenges/diff/src/test/resources/modified.txt` (create — test fixture)

**Work:**
1. Implement `main()`: parse two positional arguments (file paths), read files as line lists, run DiffGenerator, format with DiffFormatter, print to stdout. Exit code 0 if identical, 1 if differences, 2 on error (file not found, wrong args).
2. Create test fixture files with known differences.
3. Write integration tests: identical files (exit 0, no output), files with differences (exit 1, correct output), missing file (exit 2, error message), wrong number of args (exit 2, usage message).
4. Build native image: `./gradlew :challenges:diff:nativeCompile`.
5. Install: `./gradlew :challenges:diff:install`.
6. Present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:diff:test` — all tests pass.
- `gig-diff original.txt modified.txt` produces correct diff output.
- Exit codes match GNU diff conventions.

**Acceptance:** Native binary installed as `gig-diff`, user has manually verified.

### Batch 13.4 — Diff tool article write-up

**Delegation:** in-session
**Decisions:** D-13.6
**Depends on:** Batch 13.3
**Files:**
- `challenges/diff/ARTICLE.md` (create)

**Work:**
1. Write article from `~/.claude/templates/gig/ARTICLE.md` template.
2. Include Usage section with example commands.
3. Focus areas: LCS algorithm (DP table construction, backtracking), edit script generation from LCS, classic diff output format, time/space complexity (O(mn)).

**Test criteria:**
- `challenges/diff/ARTICLE.md` exists with all sections populated including Usage.
- No placeholder text remains.

**Acceptance:** Complete, publishable article with Usage section.

**Phase Acceptance Criteria:**
- [ ] LCS correctly computed for character and line sequences
- [ ] Challenge's exact LCS test cases pass
- [ ] Diff output uses `< ` / `> ` / `---` format
- [ ] Identical files produce no output (exit 0)
- [ ] Different files produce correct diff (exit 1)
- [ ] Missing file produces error (exit 2)
- [ ] Wrong number of args prints usage (exit 2)
- [ ] Native binary installed as `gig-diff`
- [ ] Article written at `challenges/diff/ARTICLE.md` with Usage section

**Completion triggers Phase 14 → version `0.14.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
