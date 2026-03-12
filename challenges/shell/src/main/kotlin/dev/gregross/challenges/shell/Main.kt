package dev.gregross.challenges.shell

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import java.io.File

fun main() {
    val historyFile = File(System.getProperty("user.home"), ".gig-sh_history")

    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()

    val jlineHistory = DefaultHistory()

    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .history(jlineHistory)
        .variable(org.jline.reader.LineReader.HISTORY_FILE, historyFile.toPath())
        .build()

    // Load existing history
    val shell = Shell()
    shell.history.load(historyFile)

    // Populate JLine history from our history
    for (entry in shell.history.entries()) {
        jlineHistory.add(entry)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        shell.history.save(historyFile)
        terminal.close()
    })

    while (shell.running) {
        try {
            val line = lineReader.readLine("gig-sh> ")
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            shell.history.add(trimmed)
            jlineHistory.add(trimmed)
            shell.processInput(trimmed)
        } catch (_: UserInterruptException) {
            // Ctrl+C — print newline, show new prompt
            terminal.writer().println()
        } catch (_: EndOfFileException) {
            // Ctrl+D — exit
            break
        }
    }

    shell.history.save(historyFile)
}
