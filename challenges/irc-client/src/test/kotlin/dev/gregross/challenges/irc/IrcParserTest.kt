package dev.gregross.challenges.irc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IrcParserTest {

    // --- Parsing incoming messages ---

    @Test fun `parse simple command without prefix`() {
        val msg = IrcParser.parse("PING :irc.libera.chat")
        assertNull(msg.prefix)
        assertEquals("PING", msg.command)
        assertEquals(listOf("irc.libera.chat"), msg.params)
    }

    @Test fun `parse server message with prefix`() {
        val msg = IrcParser.parse(":irc.libera.chat 001 testuser :Welcome to the Libera.Chat Network")
        assertEquals("irc.libera.chat", msg.prefix)
        assertEquals("001", msg.command)
        assertEquals(listOf("testuser", "Welcome to the Libera.Chat Network"), msg.params)
    }

    @Test fun `parse PRIVMSG with nick user host prefix`() {
        val msg = IrcParser.parse(":alice!alice@host.example PRIVMSG #test :hello world")
        assertEquals("alice!alice@host.example", msg.prefix)
        assertEquals("PRIVMSG", msg.command)
        assertEquals(listOf("#test", "hello world"), msg.params)
        assertEquals("alice", msg.nick)
    }

    @Test fun `parse JOIN message`() {
        val msg = IrcParser.parse(":alice!alice@host.example JOIN #test")
        assertEquals("JOIN", msg.command)
        assertEquals(listOf("#test"), msg.params)
        assertEquals("alice", msg.nick)
    }

    @Test fun `parse PART with reason`() {
        val msg = IrcParser.parse(":alice!alice@host.example PART #test :leaving now")
        assertEquals("PART", msg.command)
        assertEquals(listOf("#test", "leaving now"), msg.params)
    }

    @Test fun `parse QUIT with message`() {
        val msg = IrcParser.parse(":alice!alice@host.example QUIT :goodbye")
        assertEquals("QUIT", msg.command)
        assertEquals(listOf("goodbye"), msg.params)
    }

    @Test fun `parse NICK change`() {
        val msg = IrcParser.parse(":alice!alice@host.example NICK :bob")
        assertEquals("NICK", msg.command)
        assertEquals(listOf("bob"), msg.params)
        assertEquals("alice", msg.nick)
    }

    @Test fun `parse numeric reply with multiple params`() {
        val msg = IrcParser.parse(":irc.libera.chat 353 testuser = #test :alice bob charlie")
        assertEquals("353", msg.command)
        assertEquals(listOf("testuser", "=", "#test", "alice bob charlie"), msg.params)
    }

    @Test fun `parse NOTICE from server`() {
        val msg = IrcParser.parse(":irc.libera.chat NOTICE * :*** Looking up your hostname...")
        assertEquals("NOTICE", msg.command)
        assertEquals(listOf("*", "*** Looking up your hostname..."), msg.params)
    }

    @Test fun `parse message with CRLF`() {
        val msg = IrcParser.parse(":server 001 nick :Welcome\r\n")
        assertEquals("001", msg.command)
        assertEquals(listOf("nick", "Welcome"), msg.params)
    }

    @Test fun `nick extracts from prefix before bang`() {
        val msg = IrcParser.parse(":user!ident@host PRIVMSG #ch :hi")
        assertEquals("user", msg.nick)
    }

    @Test fun `nick is null when no prefix`() {
        val msg = IrcParser.parse("PING :server")
        assertNull(msg.nick)
    }

    @Test fun `nick from server prefix without bang`() {
        val msg = IrcParser.parse(":irc.libera.chat 001 nick :Welcome")
        assertEquals("irc.libera.chat", msg.nick)
    }

    @Test fun `trailing returns last param`() {
        val msg = IrcParser.parse(":alice!a@h PRIVMSG #test :hello world")
        assertEquals("hello world", msg.trailing)
    }

    @Test fun `parse command with no params`() {
        val msg = IrcParser.parse("QUIT")
        assertNull(msg.prefix)
        assertEquals("QUIT", msg.command)
        assertEquals(emptyList(), msg.params)
    }

    @Test fun `parse empty trailing`() {
        val msg = IrcParser.parse(":alice!a@h PRIVMSG #test :")
        assertEquals(listOf("#test", ""), msg.params)
    }

    // --- Formatting outgoing messages ---

    @Test fun `format NICK`() {
        assertEquals("NICK testuser\r\n", IrcParser.formatNick("testuser"))
    }

    @Test fun `format USER`() {
        assertEquals("USER testuser 0 * :Test User\r\n", IrcParser.formatUser("testuser", "Test User"))
    }

    @Test fun `format JOIN`() {
        assertEquals("JOIN #test\r\n", IrcParser.formatJoin("#test"))
    }

    @Test fun `format PART without message`() {
        assertEquals("PART #test\r\n", IrcParser.formatPart("#test"))
    }

    @Test fun `format PART with message`() {
        assertEquals("PART #test :leaving\r\n", IrcParser.formatPart("#test", "leaving"))
    }

    @Test fun `format PRIVMSG`() {
        assertEquals("PRIVMSG #test :hello world\r\n", IrcParser.formatPrivmsg("#test", "hello world"))
    }

    @Test fun `format PONG`() {
        assertEquals("PONG :irc.libera.chat\r\n", IrcParser.formatPong("irc.libera.chat"))
    }

    @Test fun `format QUIT without message`() {
        assertEquals("QUIT\r\n", IrcParser.formatQuit())
    }

    @Test fun `format QUIT with message`() {
        assertEquals("QUIT :goodbye\r\n", IrcParser.formatQuit("goodbye"))
    }
}
