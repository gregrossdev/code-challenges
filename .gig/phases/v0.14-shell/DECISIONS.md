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

## 2026-03-12 — Architecture: How should the shell be structured?

**Decision:** Six components: (1) **Shell** — main REPL loop (read, parse, execute, repeat). (2) **InputParser** — splits raw input into commands, handles pipes and quoting. (3) **Executor** — runs external commands via ProcessBuilder, wires pipes between processes. (4) **BuiltinRegistry** — dispatches built-in commands (exit, cd, pwd, history). (5) **History** — in-memory command history with file persistence (~/.gig-sh_history). (6) **Main** — entry point, signal setup, shutdown hook.
**Rationale:** Clean separation between input handling, parsing, execution, and built-ins. Each component is testable in isolation. Follows the established multi-component pattern from prior challenges.
**Alternatives considered:** Monolithic shell class (untestable), AST-based parser (overkill for the challenge scope).
**Status:** ACTIVE
**ID:** D-14.1

## 2026-03-12 — Input parsing: How to handle quoting and pipes?

**Decision:** Support single quotes, double quotes, and pipe (`|`) splitting. Single quotes preserve literal content. Double quotes preserve spaces but don't need variable expansion (no env vars in scope). Pipes split input into multiple commands. Parsing approach: character-by-character state machine (NORMAL, SINGLE_QUOTED, DOUBLE_QUOTED) that splits on whitespace outside quotes, and splits on `|` for pipe chains.
**Rationale:** The challenge requires pipes (Step 6) and arguments (Step 4). Quoting is essential for filenames with spaces. A simple state machine handles all cases without a full lexer/parser.
**Alternatives considered:** Regex splitting (breaks on quoted pipes), full shell grammar parser (overkill — no variables, no redirects, no subshells needed).
**Status:** ACTIVE
**ID:** D-14.2

## 2026-03-12 — Process execution: How to run commands and pipes?

**Decision:** Use `ProcessBuilder` for external commands. For single commands: start process, inherit stdin/stdout/stderr, waitFor. For pipes: chain ProcessBuilder instances — redirect each process's stdout to the next process's stdin via `ProcessBuilder.Redirect.PIPE`, read final output to shell stdout. Search PATH for command resolution (ProcessBuilder does this automatically).
**Rationale:** ProcessBuilder is the standard JVM API for process management. It handles PATH lookup, argument passing, and I/O redirection. Pipe wiring requires manual stdin/stdout threading between processes.
**Alternatives considered:** Runtime.exec (older API, less control), writing a PATH resolver manually (unnecessary — ProcessBuilder handles it).
**Status:** ACTIVE
**ID:** D-14.3

## 2026-03-12 — Built-in commands: What to implement?

**Decision:** Four built-ins: `exit` (terminate shell, save history), `cd <path>` (change working directory, support `~` expansion), `pwd` (print working directory), `history` (list numbered history entries). Built-ins execute in-process (no fork). `cd` must be built-in because it modifies the shell's own working directory. All other commands are external.
**Rationale:** Challenge Steps 2 and 5 require exit, cd, pwd. Step 8 requires history. These four cover the challenge requirements. Built-ins are checked before PATH lookup.
**Alternatives considered:** Adding `echo` as built-in (unnecessary — /bin/echo exists), adding `type` command (not in challenge spec).
**Status:** ACTIVE
**ID:** D-14.4

## 2026-03-12 — Signal handling & terminal: How to handle Ctrl+C and history navigation?

**Decision:** Handle SIGINT (Ctrl+C) via `sun.misc.Signal` — in the shell loop, print a new prompt instead of exiting. Child processes receive SIGINT naturally (they're in the same process group). For history navigation with arrow keys, use JLine 3 as a dependency — it provides line editing, arrow key history, and proper terminal handling. JLine is GraalVM native-image compatible. The prompt is `gig-sh> `.
**Rationale:** Challenge Step 7 requires Ctrl+C handling. Step 8 requires arrow key history navigation. Raw terminal mode for arrow keys is complex to implement from scratch — JLine is the standard JVM solution and is well-tested with GraalVM native-image. The prompt matches the project naming convention (gig- prefix).
**Alternatives considered:** No arrow keys (poor UX, challenge asks for it), manual ANSI escape parsing (fragile, reimplements JLine), System.console().readLine (no arrow keys or history).
**Status:** ACTIVE
**ID:** D-14.5

## 2026-03-12 — Testing: What test strategy?

**Decision:** Three tiers: (1) **InputParserTest** — tokenization, quoting, pipe splitting, edge cases (empty input, trailing pipes, nested quotes). (2) **BuiltinTest** — cd changes directory, pwd returns current dir, history tracks commands. (3) **IntegrationTest** — launch shell as subprocess, send commands via stdin, verify stdout output for echo, ls, pipes, built-ins, error messages.
**Rationale:** Parser is the most complex logic and needs thorough unit testing. Built-ins have testable state changes. Integration tests verify the full REPL including process spawning. Follows the 3-tier pattern.
**Status:** ACTIVE
**ID:** D-14.6

## 2026-03-12 — Content: Article write-up

**Decision:** Write `challenges/shell/ARTICLE.md` after implementation. Include Usage section per template. Focus on REPL architecture, input parsing state machine, process spawning and pipe wiring, built-in vs external command dispatch, signal handling, and JLine integration for history.
**Rationale:** Established convention. Shell internals are a rich topic for the article.
**Status:** ACTIVE
**ID:** D-14.7
