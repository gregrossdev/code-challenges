package dev.gregross.challenges.irc

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var server = "irc.libera.chat"
    var port = 6667
    var nick: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-s", "--server" -> { server = args.getOrNull(++i) ?: usage() }
            "-p", "--port" -> { port = args.getOrNull(++i)?.toIntOrNull() ?: usage() }
            "-n", "--nick" -> { nick = args.getOrNull(++i) ?: usage() }
            "-h", "--help" -> usage()
            else -> {
                System.err.println("Unknown option: ${args[i]}")
                usage()
            }
        }
        i++
    }

    if (nick == null) {
        System.err.println("Error: --nick is required.")
        usage()
    }

    val terminal = TerminalBuilder.builder().system(true).build()
    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()

    val connection = IrcConnection(server, port)
    val writer = terminal.writer()

    val client = IrcClient(connection, nick) { msg ->
        writer.println(msg)
        writer.flush()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        if (connection.isConnected) connection.disconnect()
        terminal.close()
    })

    writer.println("Connecting to $server:$port as $nick...")
    writer.flush()

    try {
        connection.connect()
        connection.onMessage { msg -> client.handleMessage(msg) }
        connection.startReading()
        client.register()
    } catch (e: Exception) {
        writer.println("Failed to connect: ${e.message}")
        writer.flush()
        terminal.close()
        exitProcess(1)
    }

    while (client.running && connection.isConnected) {
        try {
            val line = lineReader.readLine(client.prompt)
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            client.processInput(trimmed)
        } catch (_: UserInterruptException) {
            writer.println()
            writer.flush()
        } catch (_: EndOfFileException) {
            client.processInput("/quit")
            break
        }
    }

    exitProcess(0)
}

private fun usage(): Nothing {
    System.err.println("Usage: gig-irc -n <nick> [-s <server>] [-p <port>]")
    System.err.println()
    System.err.println("Options:")
    System.err.println("  -n, --nick <nick>      Nickname (required)")
    System.err.println("  -s, --server <server>  IRC server (default: irc.libera.chat)")
    System.err.println("  -p, --port <port>      Port (default: 6667)")
    System.err.println()
    System.err.println("Commands:")
    System.err.println("  /join #channel         Join a channel")
    System.err.println("  /part [message]        Leave current channel")
    System.err.println("  /nick <name>           Change nickname")
    System.err.println("  /msg <target> <text>   Send private message")
    System.err.println("  /quit [message]        Disconnect")
    exitProcess(1)
}
