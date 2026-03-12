# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 14 — Shell (v0.14.x)

> Build an interactive shell from scratch — REPL loop, input parsing with quoting and pipe support, external command execution via ProcessBuilder, built-in commands (exit, cd, pwd, history), pipe wiring between processes, Ctrl+C signal handling, and persistent command history with arrow key navigation via JLine 3. Deliver as `gig-sh` native binary with article write-up.

**Decisions:** D-14.1, D-14.2, D-14.3, D-14.4, D-14.5, D-14.6, D-14.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 14.1 | `0.14.1` | Module scaffold, InputParser & basic REPL | in-session | pending |
| 14.2 | `0.14.2` | Executor & pipe wiring | in-session | pending |
| 14.3 | `0.14.3` | Built-in commands (exit, cd, pwd, history) | in-session | pending |
| 14.4 | `0.14.4` | JLine integration, signal handling & history persistence | in-session | pending |
| 14.5 | `0.14.5` | Native image, integration tests & manual verification | in-session | pending |
| 14.6 | `0.14.6` | Shell article write-up | in-session | pending |

### Batch 14.1 — Module scaffold, InputParser & basic REPL

**Delegation:** in-session
**Decisions:** D-14.1, D-14.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:shell`)
- `challenges/shell/build.gradle.kts` (create — convention plugin + JLine dependency)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/InputParser.kt` (create)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Shell.kt` (create — basic REPL with System.in)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Main.kt` (create — stub)
- `challenges/shell/src/test/kotlin/dev/gregross/challenges/shell/InputParserTest.kt` (create)

**Work:**
1. Add `challenges:shell` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-sh`. Add JLine 3 dependency.
3. Implement `InputParser`. Method `parseTokens(input: String): List<String>` — state machine (NORMAL, SINGLE_QUOTED, DOUBLE_QUOTED) splits on whitespace outside quotes. Method `parsePipeline(input: String): List<List<String>>` — splits on `|` first, then tokenizes each segment.
4. Create basic `Shell` class with REPL loop reading from BufferedReader, printing `gig-sh> ` prompt.
5. Write parser tests: simple command, command with args, single quotes, double quotes, pipes, empty input, whitespace-only, quoted pipes (not split), trailing whitespace.

**Test criteria:**
- `./gradlew :challenges:shell:test` — all tests pass.
- `parseTokens("ls -la")` → `["ls", "-la"]`.
- `parsePipeline("cat file | wc -l")` → `[["cat", "file"], ["wc", "-l"]]`.
- Quoted strings preserve spaces.

**Acceptance:** Parser correctly tokenizes input with quoting and pipe support.

### Batch 14.2 — Executor & pipe wiring

**Delegation:** in-session
**Decisions:** D-14.3
**Depends on:** Batch 14.1
**Files:**
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Executor.kt` (create)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Shell.kt` (modify — wire executor)

**Work:**
1. Implement `Executor`. Method `execute(pipeline: List<List<String>>, workingDir: File): Int` — for single commands, use ProcessBuilder with inherited I/O. For pipelines, chain processes: first process stdout → next process stdin, last process stdout → inherited. Return exit code of last process.
2. Wire Executor into Shell REPL: parse input, execute, display errors for unknown commands.
3. Handle `IOException` for non-existent commands — print error message, continue REPL.

**Test criteria:**
- Shell can run `echo hello` and display output.
- Shell can run `ls` and display directory listing.
- Shell can run `echo hello | cat` (pipe works).
- Non-existent command prints error, shell continues.

**Acceptance:** External commands execute with proper I/O, pipes chain correctly.

### Batch 14.3 — Built-in commands (exit, cd, pwd, history)

**Delegation:** in-session
**Decisions:** D-14.4
**Depends on:** Batch 14.2
**Files:**
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/BuiltinRegistry.kt` (create)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/History.kt` (create)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Shell.kt` (modify — integrate builtins)
- `challenges/shell/src/test/kotlin/dev/gregross/challenges/shell/BuiltinTest.kt` (create)

**Work:**
1. Implement `History` — in-memory list, `add(command)`, `entries(): List<String>`, `load(file)`, `save(file)`.
2. Implement `BuiltinRegistry(shell)` — checks if first token is a builtin, dispatches. `exit` stops the REPL. `cd` changes `Shell.workingDir` (supports `~`, relative, absolute paths). `pwd` prints `workingDir`. `history` prints numbered entries.
3. Integrate into Shell: check builtins before executor. Track all commands in history.
4. Write tests: cd changes directory, cd ~ goes to home, pwd returns current dir, history tracks commands, exit signals shutdown.

**Test criteria:**
- `./gradlew :challenges:shell:test` — all tests pass.
- `cd /tmp && pwd` shows `/tmp` (conceptual — tested via unit tests).
- History accumulates commands.

**Acceptance:** All four built-ins work correctly, history tracks commands.

### Batch 14.4 — JLine integration, signal handling & history persistence

**Delegation:** in-session
**Decisions:** D-14.5
**Depends on:** Batch 14.3
**Files:**
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Shell.kt` (modify — replace BufferedReader with JLine LineReader)
- `challenges/shell/src/main/kotlin/dev/gregross/challenges/shell/Main.kt` (modify — signal setup, shutdown hook)

**Work:**
1. Replace `BufferedReader` input with JLine `LineReader`. Configure prompt `gig-sh> `, enable history with arrow key navigation.
2. Wire JLine's built-in history to the History class — JLine handles arrow keys and persistence automatically.
3. Set up SIGINT handler via `sun.misc.Signal` — on Ctrl+C, print newline and new prompt instead of exiting.
4. Add shutdown hook to save history to `~/.gig-sh_history`.
5. Load history from `~/.gig-sh_history` on startup.

**Test criteria:**
- Shell starts with JLine, prompt displays correctly.
- Ctrl+C doesn't exit the shell.
- History persists across restarts (manual verification).

**Acceptance:** JLine provides line editing and history navigation, Ctrl+C handled gracefully.

### Batch 14.5 — Native image, integration tests & manual verification

**Delegation:** in-session
**Decisions:** D-14.6
**Depends on:** Batch 14.4
**Files:**
- `challenges/shell/src/test/kotlin/dev/gregross/challenges/shell/IntegrationTest.kt` (create)

**Work:**
1. Write integration tests: launch shell subprocess, send commands via stdin, verify stdout. Test: echo output, ls, pipe (echo hello | cat), cd + pwd, unknown command error, exit.
2. Build native image: `./gradlew :challenges:shell:nativeCompile`. May need GraalVM reflect-config for JLine.
3. Install: `./gradlew :challenges:shell:install`.
4. Present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:shell:test` — all tests pass.
- `gig-sh` binary runs interactively.
- Commands, pipes, built-ins, Ctrl+C, arrow keys all work.

**Acceptance:** Native binary installed as `gig-sh`, user has manually verified.

### Batch 14.6 — Shell article write-up

**Delegation:** in-session
**Decisions:** D-14.7
**Depends on:** Batch 14.5
**Files:**
- `challenges/shell/ARTICLE.md` (create)

**Work:**
1. Write article from `~/.claude/templates/gig/ARTICLE.md` template.
2. Include Usage section with examples.
3. Focus areas: REPL architecture, input parsing state machine, ProcessBuilder pipe wiring, built-in vs external dispatch, signal handling, JLine for terminal UX.

**Test criteria:**
- `challenges/shell/ARTICLE.md` exists with all sections populated including Usage.
- No placeholder text remains.

**Acceptance:** Complete, publishable article with Usage section.

**Phase Acceptance Criteria:**
- [ ] Shell displays `gig-sh> ` prompt and accepts commands
- [ ] External commands execute (echo, ls, cat, etc.)
- [ ] Arguments passed correctly (ls -la, cat file)
- [ ] Pipes work (echo hello | cat, cat file | wc -l)
- [ ] Quoted strings preserved (echo "hello world")
- [ ] `exit` terminates the shell
- [ ] `cd` changes directory (relative, absolute, ~)
- [ ] `pwd` prints working directory
- [ ] `history` lists prior commands
- [ ] Non-existent commands show error, shell continues
- [ ] Ctrl+C doesn't exit the shell
- [ ] Arrow keys navigate command history
- [ ] History persists to ~/.gig-sh_history
- [ ] Native binary installed as `gig-sh`
- [ ] Article written at `challenges/shell/ARTICLE.md` with Usage section

**Completion triggers Phase 15 → version `0.15.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
