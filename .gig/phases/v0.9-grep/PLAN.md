# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 9 — Grep (v0.9.x)

> Build a Unix-like grep tool — line-by-line pattern matching with regex support (`\d`, `\w`, `^`, `$`, character classes), recursive directory search, invert match, case-insensitive mode, proper exit codes, and filename prefixing in multi-file mode. Deliver as `gig-grep` native binary with article write-up.

**Decisions:** D-9.1, D-9.2, D-9.3, D-9.4, D-9.5, D-9.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 9.1 | `0.9.1` | Module scaffold & Matcher | in-session | done |
| 9.2 | `0.9.2` | GrepProcessor & single-file search | in-session | done |
| 9.3 | `0.9.3` | Recursive search & multi-file output | in-session | done |
| 9.4 | `0.9.4` | CLI, native image & integration tests | in-session | done |
| 9.5 | `0.9.5` | Grep article write-up | in-session | done |

### Batch 9.1 — Module scaffold & Matcher

**Delegation:** in-session
**Decisions:** D-9.1, D-9.3
**Files:**
- `settings.gradle.kts` (modify — add `challenges:grep`)
- `challenges/grep/build.gradle.kts` (create)
- `challenges/grep/src/main/kotlin/dev/gregross/challenges/grep/Matcher.kt` (create)
- `challenges/grep/src/main/kotlin/dev/gregross/challenges/grep/Main.kt` (create — stub)
- `challenges/grep/src/test/kotlin/dev/gregross/challenges/grep/MatcherTest.kt` (create)

**Work:**
1. Add `challenges:grep` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-grep`.
3. Implement `Matcher(pattern: String, ignoreCase: Boolean, invert: Boolean)`. Compiles pattern to JVM `Regex` with optional `IGNORE_CASE`. Method `matches(line: String): Boolean` — returns true if pattern found in line (or not found if inverted).
4. Write tests: literal match, no match, `\d`, `\w`, `^` anchor, `$` anchor, `[abc]`, `[^abc]`, case insensitive, invert match.

**Test criteria:**
- `./gradlew :challenges:grep:test` — all matcher tests pass.
- `Matcher("hello").matches("say hello world")` returns true.
- `Matcher("^hello").matches("say hello")` returns false.

**Acceptance:** Matcher correctly handles all pattern types and flags.

### Batch 9.2 — GrepProcessor & single-file search

**Delegation:** in-session
**Decisions:** D-9.1, D-9.2, D-9.4
**Depends on:** Batch 9.1
**Files:**
- `challenges/grep/src/main/kotlin/dev/gregross/challenges/grep/GrepProcessor.kt` (create)
- `challenges/grep/src/test/kotlin/dev/gregross/challenges/grep/GrepProcessorTest.kt` (create)
- `challenges/grep/src/test/resources/test.txt` (create — test fixture)

**Work:**
1. Implement `GrepProcessor(matcher: Matcher)`. Method `processStream(input: InputStream, output: PrintStream, filename: String?, showFilename: Boolean): Boolean` — reads line-by-line, applies matcher, writes matching lines (with optional `filename:` prefix), returns whether any match was found.
2. Method `processFile(file: File, output: PrintStream, showFilename: Boolean): Boolean` — opens file, delegates to processStream. Skip binary files (check first 8KB for null bytes).
3. Create test fixture `test.txt` with varied content for testing.
4. Write tests: match lines in file, no matches returns false, empty pattern matches all, filename prefix when showFilename=true, binary file skipped, stdin input.

**Test criteria:**
- `./gradlew :challenges:grep:test` — all processor tests pass.
- Processing file with matches returns true; no matches returns false.

**Acceptance:** Single-file grep works with proper output formatting.

### Batch 9.3 — Recursive search & multi-file output

**Delegation:** in-session
**Decisions:** D-9.2, D-9.4
**Depends on:** Batch 9.2
**Files:**
- `challenges/grep/src/main/kotlin/dev/gregross/challenges/grep/FileSearcher.kt` (create)
- `challenges/grep/src/test/kotlin/dev/gregross/challenges/grep/FileSearcherTest.kt` (create)
- `challenges/grep/src/test/resources/testdir/` (create — test directory structure)

**Work:**
1. Implement `FileSearcher`. Method `findFiles(paths: List<String>, recursive: Boolean): List<File>` — if path is file, return it. If directory and recursive, walk tree collecting files. Sort files for deterministic output.
2. Create test directory structure: `testdir/a.txt`, `testdir/b.txt`, `testdir/sub/c.txt`.
3. Write tests: single file, directory non-recursive (error), directory recursive lists all files, nested directories, sorted output.

**Test criteria:**
- `./gradlew :challenges:grep:test` — all file searcher tests pass.
- Recursive search finds files in subdirectories.

**Acceptance:** Recursive file discovery works correctly.

### Batch 9.4 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-9.2, D-9.4, D-9.5
**Depends on:** Batch 9.3
**Files:**
- `challenges/grep/src/main/kotlin/dev/gregross/challenges/grep/Main.kt` (modify)
- `challenges/grep/src/test/kotlin/dev/gregross/challenges/grep/IntegrationTest.kt` (create)

**Work:**
1. Implement `main()`: parse flags (`-i`, `-v`, `-r`), extract pattern and file args. Wire up Matcher + GrepProcessor + FileSearcher. Exit with code 0 (match), 1 (no match), 2 (error). Multi-file or recursive → show filenames.
2. Write integration tests exercising all 7 challenge steps: empty pattern, single char, recursive, invert, `\d`/`\w`, anchors, case-insensitive.
3. Build native image: `./gradlew :challenges:grep:nativeCompile`.
4. Install: `./gradlew :challenges:grep:install`.
5. Verify symlink and present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:grep:test` — all tests pass.
- `gig-grep "hello" test.txt` prints matching lines.
- `echo "hello world" | gig-grep "hello"` works via stdin.
- `gig-grep -r "pattern" .` recursively searches.
- Exit code 0 on match, 1 on no match.

**Acceptance:** Native binary installed as `gig-grep`, user has manually verified.

### Batch 9.5 — Grep article write-up

**Delegation:** in-session
**Decisions:** D-9.6
**Depends on:** Batch 9.4
**Files:**
- `challenges/grep/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template.
2. Focus areas: line-by-line processing model, regex pattern matching, recursive file traversal, exit code semantics, how grep handles binary files.

**Test criteria:**
- `challenges/grep/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-grep "" file` matches all lines (empty pattern)
- [ ] `gig-grep "x" file` matches lines containing "x"
- [ ] `gig-grep -r "pattern" dir` recursively searches directories
- [ ] `gig-grep -v "pattern" file` inverts match (shows non-matching lines)
- [ ] `gig-grep "\d" file` matches lines with digits
- [ ] `gig-grep "\w" file` matches lines with word characters
- [ ] `gig-grep "^start" file` matches lines starting with "start"
- [ ] `gig-grep "end$" file` matches lines ending with "end"
- [ ] `gig-grep -i "HELLO" file` matches case-insensitively
- [ ] Reads from stdin when no file argument given
- [ ] Shows `filename:line` prefix in multi-file/recursive mode
- [ ] Exit code 0 on match, 1 on no match, 2 on error
- [ ] Skips binary files
- [ ] Native binary installed as `gig-grep`
- [ ] Article written at `challenges/grep/ARTICLE.md`

**Completion triggers Phase 10 → version `0.10.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
