package dev.gregross.challenges.shell

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.File

fun main() {
    val historyFile = File(System.getProperty("user.home"), ".gig-sh_history")

    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()

    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .variable(org.jline.reader.LineReader.HISTORY_FILE, historyFile.toPath())
        .build()

    val shell = Shell()

    // Populate shell history from JLine's persisted history (loaded on first readLine)
    Runtime.getRuntime().addShutdownHook(Thread {
        terminal.close()
    })

    while (shell.running) {
        try {
            val line = lineReader.readLine("gig-sh> ")
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            shell.history.add(trimmed)
            shell.processInput(trimmed)
        } catch (_: UserInterruptException) {
            // Ctrl+C — print newline, show new prompt
            terminal.writer().println()
        } catch (_: EndOfFileException) {
            // Ctrl+D — exit
            break
        }
    }
}
