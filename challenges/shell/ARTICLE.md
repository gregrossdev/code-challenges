# Building My Own Shell: REPL, Pipes & Process Management in Kotlin

> An interactive shell built from scratch — input parsing with quoting, external command execution via ProcessBuilder, pipe wiring between processes, built-in commands, Ctrl+C signal handling, and persistent command history with JLine 3.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Shell](https://codingchallenges.fyi/challenges/challenge-shell)

A shell is the interface between a user and the operating system. Every command you type — `ls`, `grep`, `cd` — goes through a shell that parses your input, finds the right program, and manages its execution. Building one from scratch reveals what happens between pressing Enter and seeing output.

The challenge walks through it step by step: accepting input, spawning processes, handling arguments and quoting, implementing built-in commands, wiring pipes between processes, handling Ctrl+C signals, and persisting command history across sessions.

## Usage

```bash
gig-sh
```

```bash
# Start the shell
gig-sh

# Run commands
gig-sh> echo hello
gig-sh> ls -la
gig-sh> cat file.txt | grep pattern | wc -l

# Built-in commands
gig-sh> cd /tmp
gig-sh> pwd
gig-sh> history
gig-sh> exit

# Arrow keys navigate command history
# Ctrl+C cancels current input (doesn't exit)
# Ctrl+D exits the shell
```

## Approach

Six components, each handling one concern:

1. **InputParser** — state machine that tokenizes input, handles quoting, and splits on pipes
2. **Executor** — runs external commands via ProcessBuilder, wires pipe chains
3. **BuiltinRegistry** — dispatches built-in commands (exit, cd, pwd, history)
4. **History** — in-memory command tracking for the history builtin
5. **Shell** — REPL loop that ties parsing, builtins, and execution together
6. **Main** — JLine terminal setup, signal handling, history persistence

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Input parsing | Character-by-character state machine | Handles quoting and pipes without a full grammar parser |
| Process execution | ProcessBuilder | Standard JVM API with automatic PATH lookup |
| Pipe wiring | Virtual threads copying stdout→stdin | Simple, non-blocking I/O between processes |
| Terminal UX | JLine 3 | Arrow key history, line editing, GraalVM-compatible |
| Signal handling | JLine's UserInterruptException | Catches Ctrl+C cleanly within the read loop |

## Input Parsing: A State Machine

The parser needs to handle three things: splitting on whitespace (for arguments), respecting quotes (preserving spaces inside them), and splitting on pipes (for command chaining). A character-by-character state machine handles all three:

```kotlin
fun parseTokens(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var state = State.NORMAL
    var inToken = false

    for (ch in input) {
        when (state) {
            State.NORMAL -> when (ch) {
                '\'' -> { state = State.SINGLE_QUOTED; inToken = true }
                '"' -> { state = State.DOUBLE_QUOTED; inToken = true }
                ' ', '\t' -> {
                    if (inToken) {
                        tokens.add(current.toString())
                        current.clear()
                        inToken = false
                    }
                }
                else -> { current.append(ch); inToken = true }
            }
            State.SINGLE_QUOTED -> when (ch) {
                '\'' -> state = State.NORMAL
                else -> current.append(ch)
            }
            State.DOUBLE_QUOTED -> when (ch) {
                '"' -> state = State.NORMAL
                else -> current.append(ch)
            }
        }
    }

    if (inToken) tokens.add(current.toString())
    return tokens
}
```

Three states: NORMAL (splitting on whitespace), SINGLE_QUOTED (everything is literal), DOUBLE_QUOTED (everything is literal). Opening a quote switches state; closing it switches back. The `inToken` flag tracks whether we've started building a token — this correctly handles empty quotes like `''` producing an empty string argument.

Pipe splitting uses the same state machine but splits on `|` instead of whitespace, preserving quotes so `echo 'hello | world'` doesn't get split.

## Process Execution and Pipe Wiring

For a single command, ProcessBuilder handles everything — PATH lookup, argument passing, I/O inheritance:

```kotlin
private fun executeSingle(command: List<String>, workingDir: File): Int {
    return try {
        ProcessBuilder(command)
            .directory(workingDir)
            .inheritIO()
            .start()
            .waitFor()
    } catch (e: IOException) {
        System.err.println("gig-sh: ${command[0]}: command not found")
        127
    }
}
```

Pipes are more complex. Each command in the pipeline needs its stdout connected to the next command's stdin:

```kotlin
private fun executePipeline(pipeline: List<List<String>>, workingDir: File): Int {
    val processes = mutableListOf<Process>()

    for ((index, command) in pipeline.withIndex()) {
        val pb = ProcessBuilder(command).directory(workingDir)

        if (index == 0) pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        else pb.redirectInput(ProcessBuilder.Redirect.PIPE)

        if (index == pipeline.lastIndex) pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        else pb.redirectOutput(ProcessBuilder.Redirect.PIPE)

        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = pb.start()
        processes.add(process)

        if (index > 0) {
            val prevProcess = processes[index - 1]
            Thread.startVirtualThread {
                prevProcess.inputStream.use { src ->
                    process.outputStream.use { dst ->
                        src.copyTo(dst)
                    }
                }
            }
        }
    }

    var lastExitCode = 0
    for (process in processes) lastExitCode = process.waitFor()
    return lastExitCode
}
```

The first process inherits stdin from the shell. The last process inherits stdout to the terminal. Everything in between uses pipes. Virtual threads handle the byte copying between processes — each thread reads from one process's output and writes to the next process's input.

## Built-in vs External Commands

Most commands are external programs found on PATH. But some commands must be built-in because they modify the shell's own state. `cd` is the classic example — an external `cd` program would change its own working directory and exit, leaving the shell unchanged.

```kotlin
fun processInput(input: String) {
    val pipeline = parser.parsePipeline(input)
    if (pipeline.isEmpty()) return

    if (pipeline.size == 1 && builtins.isBuiltin(pipeline[0][0])) {
        builtins.execute(pipeline[0])
        return
    }

    executor.execute(pipeline, workingDir)
}
```

The dispatch is simple: check if the first (and only) command is a builtin. If so, handle it in-process. Otherwise, hand it to the executor. Builtins are only checked for single commands — `cd /tmp | cat` doesn't make sense and gets treated as external.

The `cd` implementation supports bare `cd` (home directory), `~` expansion, relative paths, and absolute paths:

```kotlin
private fun cd(args: List<String>): Int {
    val path = when {
        args.size < 2 -> System.getProperty("user.home")
        args[1] == "~" -> System.getProperty("user.home")
        args[1].startsWith("~/") -> System.getProperty("user.home") + args[1].substring(1)
        else -> args[1]
    }

    val target = File(path).let {
        if (it.isAbsolute) it else File(shell.workingDir, path)
    }

    val canonical = target.canonicalFile
    if (!canonical.isDirectory) {
        error.println("gig-sh: cd: ${args.getOrElse(1) { "" }}: No such directory")
        return 1
    }

    shell.workingDir = canonical
    return 0
}
```

## Terminal UX with JLine

Raw terminal handling — reading arrow keys, supporting line editing, managing history — is genuinely hard to implement from scratch. JLine 3 is the standard JVM library for this. It provides:

- Arrow key navigation through command history
- Line editing (cursor movement, backspace, delete)
- Persistent history in its own timestamped format
- Ctrl+C as `UserInterruptException` instead of process termination

```kotlin
while (shell.running) {
    try {
        val line = lineReader.readLine("gig-sh> ")
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        shell.history.add(trimmed)
        shell.processInput(trimmed)
    } catch (_: UserInterruptException) {
        terminal.writer().println()
    } catch (_: EndOfFileException) {
        break
    }
}
```

JLine catches Ctrl+C as an exception rather than letting it kill the process. This means the shell survives Ctrl+C while child processes (which inherit the signal) can still be interrupted.

## What I Learned

**A shell is a loop with a dispatch table.** Read input, parse it, check if it's a builtin, otherwise spawn a process. The REPL pattern is simple — the complexity lives in the parsing and process management.

**Quoting is a state machine problem.** Single quotes, double quotes, and normal mode form three states. Transitions happen on quote characters. Everything else is just "append to the current token." This pattern handles all the edge cases cleanly.

**Pipes require threading.** You can't just pipe stdout to stdin synchronously — both processes run concurrently, and blocking on one while the other fills its buffer causes deadlock. Virtual threads make the byte-copying goroutine-style: one thread per pipe connection.

**cd must be a builtin.** An external `cd` command would fork, change its own working directory, and exit — the parent shell's directory would never change. This is why every shell implements cd internally. The same applies to `exit` — you can't exit a shell from a child process.

**JLine eliminates terminal complexity.** Raw terminal mode, ANSI escape sequences, cursor positioning, history file formats — JLine handles all of it. Trying to implement arrow key support from scratch means parsing multi-byte escape sequences (`\x1b[A` for up arrow) and managing terminal state. JLine is the right abstraction.
