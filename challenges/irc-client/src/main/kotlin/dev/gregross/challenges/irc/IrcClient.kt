package dev.gregross.challenges.irc

class IrcClient(
    private val connection: IrcConnection,
    var nick: String,
    private val display: (String) -> Unit = {},
) {
    var currentChannel: String? = null
        private set

    var running: Boolean = true
        private set

    val prompt: String
        get() = if (currentChannel != null) "[$currentChannel]> " else "[no channel]> "

    fun register() {
        connection.send(IrcParser.formatNick(nick))
        connection.send(IrcParser.formatUser(nick, nick))
    }

    fun processInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.startsWith("/")) {
            val parts = trimmed.substring(1).split(" ", limit = 2)
            val cmd = parts[0].lowercase()
            val arg = parts.getOrNull(1)

            when (cmd) {
                "join" -> {
                    if (arg.isNullOrBlank()) {
                        display("Usage: /join #channel")
                        return
                    }
                    connection.send(IrcParser.formatJoin(arg))
                }
                "part" -> {
                    val ch = currentChannel
                    if (ch == null) {
                        display("Not in a channel.")
                        return
                    }
                    connection.send(IrcParser.formatPart(ch, arg))
                    currentChannel = null
                }
                "nick" -> {
                    if (arg.isNullOrBlank()) {
                        display("Usage: /nick NewName")
                        return
                    }
                    connection.send(IrcParser.formatNick(arg))
                }
                "quit" -> {
                    connection.send(IrcParser.formatQuit(arg))
                    running = false
                    connection.disconnect()
                }
                "msg" -> {
                    if (arg == null) {
                        display("Usage: /msg <target> <message>")
                        return
                    }
                    val msgParts = arg.split(" ", limit = 2)
                    val target = msgParts[0]
                    val text = msgParts.getOrNull(1) ?: ""
                    connection.send(IrcParser.formatPrivmsg(target, text))
                }
                else -> display("Unknown command: /$cmd")
            }
        } else {
            val ch = currentChannel
            if (ch == null) {
                display("Not in a channel. Use /join #channel first.")
                return
            }
            connection.send(IrcParser.formatPrivmsg(ch, trimmed))
        }
    }

    fun handleMessage(msg: IrcMessage) {
        when (msg.command) {
            "PRIVMSG" -> {
                val sender = msg.nick ?: "unknown"
                val text = msg.params.getOrNull(1) ?: ""
                display("<$sender> $text")
            }
            "JOIN" -> {
                val channel = msg.params.firstOrNull() ?: return
                val who = msg.nick ?: return
                if (who == nick) {
                    currentChannel = channel
                    display("* You have joined $channel")
                } else {
                    display("* $who has joined $channel")
                }
            }
            "PART" -> {
                val channel = msg.params.firstOrNull() ?: return
                val who = msg.nick ?: return
                val reason = msg.params.getOrNull(1)
                if (who == nick) {
                    display("* You have left $channel")
                } else {
                    val suffix = if (reason != null) " ($reason)" else ""
                    display("* $who has left $channel$suffix")
                }
            }
            "QUIT" -> {
                val who = msg.nick ?: return
                val reason = msg.params.firstOrNull()
                val suffix = if (reason != null) " ($reason)" else ""
                display("* $who has quit$suffix")
            }
            "NICK" -> {
                val oldNick = msg.nick ?: return
                val newNick = msg.params.firstOrNull() ?: return
                if (oldNick == nick) {
                    nick = newNick
                    display("* You are now known as $newNick")
                } else {
                    display("* $oldNick is now known as $newNick")
                }
            }
            "NOTICE" -> {
                val text = msg.params.lastOrNull() ?: ""
                display("NOTICE: $text")
            }
            "001", "002", "003", "004" -> {
                val text = msg.params.lastOrNull() ?: ""
                display(text)
            }
            "375", "372", "376" -> {
                // MOTD start, body, end
                val text = msg.params.lastOrNull() ?: ""
                display(text)
            }
            "353" -> {
                // NAMES list
                val channel = msg.params.getOrNull(2) ?: ""
                val names = msg.params.lastOrNull() ?: ""
                display("Users in $channel: $names")
            }
            "366" -> {
                // End of NAMES — ignore
            }
            "433" -> {
                display("Nickname already in use.")
            }
            else -> {
                // Display other numeric replies
                if (msg.command.all { it.isDigit() }) {
                    val text = msg.params.lastOrNull() ?: ""
                    display(text)
                }
            }
        }
    }
}
