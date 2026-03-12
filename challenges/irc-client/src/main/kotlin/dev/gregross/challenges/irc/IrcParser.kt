package dev.gregross.challenges.irc

object IrcParser {

    fun parse(raw: String): IrcMessage {
        var line = raw.trimEnd('\r', '\n')
        var prefix: String? = null

        if (line.startsWith(':')) {
            val spaceIdx = line.indexOf(' ')
            prefix = line.substring(1, spaceIdx)
            line = line.substring(spaceIdx + 1)
        }

        val trailingIdx = line.indexOf(" :")
        val trailing: String?
        val front: String

        if (trailingIdx != -1) {
            trailing = line.substring(trailingIdx + 2)
            front = line.substring(0, trailingIdx)
        } else {
            trailing = null
            front = line
        }

        val parts = front.split(' ').filter { it.isNotEmpty() }
        val command = parts.first()
        val params = parts.drop(1).toMutableList()
        if (trailing != null) {
            params.add(trailing)
        }

        return IrcMessage(prefix, command, params)
    }

    fun formatNick(nick: String): String = "NICK $nick\r\n"

    fun formatUser(user: String, realName: String): String = "USER $user 0 * :$realName\r\n"

    fun formatJoin(channel: String): String = "JOIN $channel\r\n"

    fun formatPart(channel: String, message: String? = null): String =
        if (message != null) "PART $channel :$message\r\n" else "PART $channel\r\n"

    fun formatPrivmsg(target: String, text: String): String = "PRIVMSG $target :$text\r\n"

    fun formatPong(server: String): String = "PONG :$server\r\n"

    fun formatQuit(message: String? = null): String =
        if (message != null) "QUIT :$message\r\n" else "QUIT\r\n"
}
