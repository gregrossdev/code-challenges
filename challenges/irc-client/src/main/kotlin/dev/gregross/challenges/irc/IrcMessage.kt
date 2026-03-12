package dev.gregross.challenges.irc

data class IrcMessage(
    val prefix: String?,
    val command: String,
    val params: List<String>,
) {
    val nick: String?
        get() = prefix?.substringBefore('!')?.takeIf { it.isNotEmpty() }

    val trailing: String?
        get() = params.lastOrNull()
}
