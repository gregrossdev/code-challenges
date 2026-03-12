package dev.gregross.challenges.irc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

class IrcClientTest {

    private class FakeConnection : IrcConnection("localhost", 0) {
        val sent = mutableListOf<String>()
        override fun send(message: String) { sent.add(message) }
    }

    private fun createClient(nick: String = "testuser"): Triple<IrcClient, FakeConnection, MutableList<String>> {
        val conn = FakeConnection()
        val output = mutableListOf<String>()
        val client = IrcClient(conn, nick) { output.add(it) }
        return Triple(client, conn, output)
    }

    // --- Registration ---

    @Test fun `register sends NICK and USER`() {
        val (client, conn, _) = createClient()
        client.register()
        assertEquals("NICK testuser\r\n", conn.sent[0])
        assertEquals("USER testuser 0 * :testuser\r\n", conn.sent[1])
    }

    // --- Command dispatch ---

    @Test fun `join sends JOIN command`() {
        val (client, conn, _) = createClient()
        client.processInput("/join #test")
        assertEquals("JOIN #test\r\n", conn.sent[0])
    }

    @Test fun `join without channel shows usage`() {
        val (client, _, output) = createClient()
        client.processInput("/join")
        assertEquals("Usage: /join #channel", output[0])
    }

    @Test fun `part sends PART and clears channel`() {
        val (client, conn, _) = createClient()
        // Simulate joining first
        client.handleMessage(IrcMessage("testuser!u@h", "JOIN", listOf("#test")))
        conn.sent.clear()
        client.processInput("/part")
        assertEquals("PART #test\r\n", conn.sent[0])
        assertNull(client.currentChannel)
    }

    @Test fun `part with no channel shows error`() {
        val (client, _, output) = createClient()
        client.processInput("/part")
        assertEquals("Not in a channel.", output[0])
    }

    @Test fun `nick sends NICK command`() {
        val (client, conn, _) = createClient()
        client.processInput("/nick NewName")
        assertEquals("NICK NewName\r\n", conn.sent[0])
    }

    @Test fun `quit sends QUIT and stops client`() {
        val (client, conn, _) = createClient()
        client.processInput("/quit goodbye")
        assertEquals("QUIT :goodbye\r\n", conn.sent[0])
        assertFalse(client.running)
    }

    @Test fun `plain text sends PRIVMSG to current channel`() {
        val (client, conn, _) = createClient()
        client.handleMessage(IrcMessage("testuser!u@h", "JOIN", listOf("#test")))
        conn.sent.clear()
        client.processInput("hello world")
        assertEquals("PRIVMSG #test :hello world\r\n", conn.sent[0])
    }

    @Test fun `plain text without channel shows error`() {
        val (client, _, output) = createClient()
        client.processInput("hello")
        assertEquals("Not in a channel. Use /join #channel first.", output[0])
    }

    @Test fun `unknown command shows error`() {
        val (client, _, output) = createClient()
        client.processInput("/unknown")
        assertEquals("Unknown command: /unknown", output[0])
    }

    // --- Incoming message handling ---

    @Test fun `handle PRIVMSG displays sender and text`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("alice!a@h", "PRIVMSG", listOf("#test", "hello")))
        assertEquals("<alice> hello", output[0])
    }

    @Test fun `handle JOIN from self sets current channel`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("testuser!u@h", "JOIN", listOf("#test")))
        assertEquals("#test", client.currentChannel)
        assertEquals("* You have joined #test", output[0])
    }

    @Test fun `handle JOIN from other shows notification`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("alice!a@h", "JOIN", listOf("#test")))
        assertEquals("* alice has joined #test", output[0])
    }

    @Test fun `handle PART from other shows notification`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("alice!a@h", "PART", listOf("#test", "leaving")))
        assertEquals("* alice has left #test (leaving)", output[0])
    }

    @Test fun `handle QUIT shows notification`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("alice!a@h", "QUIT", listOf("goodbye")))
        assertEquals("* alice has quit (goodbye)", output[0])
    }

    @Test fun `handle NICK change for self updates nick`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("testuser!u@h", "NICK", listOf("newnick")))
        assertEquals("newnick", client.nick)
        assertEquals("* You are now known as newnick", output[0])
    }

    @Test fun `handle NICK change for other shows notification`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("alice!a@h", "NICK", listOf("bob")))
        assertEquals("* alice is now known as bob", output[0])
    }

    @Test fun `handle welcome 001 displays text`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("server", "001", listOf("testuser", "Welcome to IRC")))
        assertEquals("Welcome to IRC", output[0])
    }

    @Test fun `handle nickname in use shows error`() {
        val (client, _, output) = createClient()
        client.handleMessage(IrcMessage("server", "433", listOf("*", "testuser", "Nickname is already in use.")))
        assertEquals("Nickname already in use.", output[0])
    }

    // --- Prompt ---

    @Test fun `prompt shows no channel when not joined`() {
        val (client, _, _) = createClient()
        assertEquals("[no channel]> ", client.prompt)
    }

    @Test fun `prompt shows channel when joined`() {
        val (client, _, _) = createClient()
        client.handleMessage(IrcMessage("testuser!u@h", "JOIN", listOf("#test")))
        assertEquals("[#test]> ", client.prompt)
    }

    // --- /msg command ---

    @Test fun `msg sends PRIVMSG to specific target`() {
        val (client, conn, _) = createClient()
        client.processInput("/msg alice hello there")
        assertEquals("PRIVMSG alice :hello there\r\n", conn.sent[0])
    }
}
