# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 1 — Project Scaffolding & wc Tool (v0.1.x)

> Set up the Gradle multi-module monorepo with shared conventions, create the article template, and implement the first coding challenge — a `wc` (word count) clone supporting all standard flags and stdin input.

**Decisions:** D-1.1, D-1.2r, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 1.1 | `0.1.1` | Gradle monorepo scaffolding & article template | in-session | pending |
| 1.2 | `0.1.2` | wc tool implementation & tests | in-session | pending |
| 1.3 | `0.1.3` | wc challenge article write-up | in-session | pending |

### Batch 1.1 — Gradle monorepo scaffolding & article template

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2r, D-1.3, D-1.6, D-1.7
**Files:**
- `settings.gradle.kts` (create)
- `build.gradle.kts` (create — root)
- `buildSrc/build.gradle.kts` (create)
- `buildSrc/src/main/kotlin/challenge-conventions.gradle.kts` (create — convention plugin)
- `challenges/wc/build.gradle.kts` (create — wc module)
- `templates/article.md` (create — reusable article template)
- `gradle/wrapper/` (generate via `gradle wrapper`)

**Work:**
1. Generate Gradle wrapper (9.4.0).
2. Create root `settings.gradle.kts` with project name `code-challenges`, include `challenges:wc`.
3. Create root `build.gradle.kts` — minimal, applies Kotlin plugin with `apply false`.
4. Create `buildSrc/build.gradle.kts` with `kotlin-dsl` plugin and Kotlin Gradle plugin dependency.
5. Create convention plugin `challenge-conventions.gradle.kts`: Kotlin JVM, JVM 21 toolchain, JUnit 5 + kotlin-test, `application` plugin for runnable challenges.
6. Create `challenges/wc/build.gradle.kts` applying the convention plugin.
7. Create `templates/article.md` — reusable write-up template with sections: Challenge Overview, Approach (from decisions), Build Log (from batches), Key Code, Testing, What I Learned.

**Test criteria:**
- `./gradlew tasks` succeeds without errors.
- `./gradlew :challenges:wc:build` compiles successfully (empty source is fine).
- `templates/article.md` exists with all required sections.

**Acceptance:** Gradle monorepo compiles, convention plugin applies correctly, article template is ready.

### Batch 1.2 — wc tool implementation & tests

**Delegation:** in-session
**Decisions:** D-1.4, D-1.5
**Depends on:** Batch 1.1
**Files:**
- `challenges/wc/src/main/kotlin/dev/gregross/challenges/wc/Main.kt` (create)
- `challenges/wc/src/main/kotlin/dev/gregross/challenges/wc/WordCount.kt` (create)
- `challenges/wc/src/test/kotlin/dev/gregross/challenges/wc/WordCountTest.kt` (create)
- `challenges/wc/src/test/resources/test.txt` (create — test fixture from challenge)

**Work:**
1. Download/create `test.txt` test fixture (the file from the challenge with known counts).
2. Implement `WordCount` class with methods: `countBytes()`, `countLines()`, `countWords()`, `countChars()`.
3. Implement `main()` in `Main.kt` with raw args parsing:
   - `-c` → byte count
   - `-l` → line count
   - `-w` → word count
   - `-m` → character count
   - No flags → lines + words + bytes (default)
   - No filename → read from stdin
4. Write tests verifying all flags against known `test.txt` values:
   - `-c` → `342190 test.txt`
   - `-l` → `7145 test.txt`
   - `-w` → `58164 test.txt`
   - `-m` → `339292 test.txt`
   - Default → `7145 58164 342190 test.txt`
5. Test stdin reading path.

**Test criteria:**
- `./gradlew :challenges:wc:test` — all tests pass.
- `./gradlew :challenges:wc:run --args="-c src/test/resources/test.txt"` → outputs `342190 test.txt`.
- `cat test.txt | ./gradlew :challenges:wc:run` → outputs line/word/byte counts from stdin.

**Acceptance:** All wc flags produce correct output matching challenge expectations. Tests pass. Stdin works.

### Batch 1.3 — wc challenge article write-up

**Delegation:** in-session
**Decisions:** D-1.7
**Depends on:** Batch 1.2
**Files:**
- `challenges/wc/ARTICLE.md` (create — from template)

**Work:**
1. Copy `templates/article.md` to `challenges/wc/ARTICLE.md`.
2. Populate using gig artifacts as source material:
   - **Challenge Overview:** from the coding challenge description (wc tool, what it does, link to challenge).
   - **Approach:** from DECISIONS.md — key choices made (raw args, file I/O strategy, Kotlin idioms used).
   - **Build Log:** from STATE.md batch history — what was built in each step.
   - **Key Code:** highlight interesting implementation details (byte vs char counting, stdin handling, Kotlin-specific patterns).
   - **Testing:** from test criteria — what was tested, how, expected values.
   - **What I Learned:** reflections on Kotlin for CLI tools, challenge takeaways.

**Test criteria:**
- `challenges/wc/ARTICLE.md` exists and has all template sections populated.
- No placeholder/TODO text remains.
- Article reads as a coherent, publishable blog post.

**Acceptance:** Complete, publishable article documenting how the wc challenge was built.

**Phase Acceptance Criteria:**
- [ ] Gradle monorepo builds successfully
- [ ] Convention plugin configures Kotlin 2.3.10, JVM 21, JUnit 5
- [ ] `wc` tool handles all flags (-c, -l, -w, -m, default)
- [ ] `wc` tool reads from stdin when no filename given
- [ ] All tests pass with correct counts against test.txt
- [ ] Article template exists at `templates/article.md`
- [ ] wc article written at `challenges/wc/ARTICLE.md` — complete and publishable

**Completion triggers Phase 2 → version `0.2.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-03-10 | 0.1.0 | Added Batch 1.3 — article write-up after tests | User requested article as part of challenge completion |
