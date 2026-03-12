package dev.gregross.challenges.irc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class IntegrationTest {

    private fun withServer(block: (MockIrcServer, IrcConnection, IrcClient, MutableList<String>) -> Unit) {
        MockIrcServer().use { server ->
            server.start()
            val connection = IrcConnection("localhost", server.port)
            val output = mutableListOf<String>()
            val client = IrcClient(connection, "testuser") { output.add(it) }

            connection.connect()
            connection.onMessage { msg -> client.handleMessage(msg) }
            connection.startReading()

            server.waitForClient()

            try {
                block(server, connection, client, output)
            } finally {
                connection.disconnect()
            }
        }
    }

    @Test fun `registration sends NICK and USER`() {
        withServer { server, _, client, _ ->
            client.register()
            Thread.sleep(100)

            val nick = server.readCommand()
            val user = server.readCommand()
            assertEquals("NICK testuser", nick)
            assertTrue(user!!.startsWith("USER testuser"))
        }
    }

    @Test fun `receives welcome message`() {
        withServer { server, _, client, output ->
            client.register()
            Thread.sleep(50)
            server.readCommand() // NICK
            server.readCommand() // USER
            server.sendWelcome("testuser")
            Thread.sleep(100)

            assertTrue(output.any { it.contains("Welcome to the Test IRC Network") })
        }
    }

    @Test fun `auto responds to PING with PONG`() {
        withServer { server, _, _, _ ->
            server.sendPing("testtoken")
            Thread.sleep(100)

            val pong = server.readCommand()
            assertEquals("PONG :testtoken", pong)
        }
    }

    @Test fun `join channel sends JOIN and updates state`() {
        withServer { server, _, client, output ->
            client.processInput("/join #test")
            Thread.sleep(50)

            val join = server.readCommand()
            assertEquals("JOIN #test", join)

            server.sendJoinConfirmation("testuser", "#test")
            Thread.sleep(100)

            assertEquals("#test", client.currentChannel)
            assertTrue(output.any { it.contains("You have joined #test") })
        }
    }

    @Test fun `send PRIVMSG to channel`() {
        withServer { server, _, client, _ ->
            // Join first
            client.processInput("/join #test")
            Thread.sleep(50)
            server.readCommand() // JOIN
            server.sendJoinConfirmation("testuser", "#test")
            Thread.sleep(100)

            client.processInput("hello world")
            Thread.sleep(50)

            val msg = server.readCommand()
            assertEquals("PRIVMSG #test :hello world", msg)
        }
    }

    @Test fun `receive PRIVMSG from another user`() {
        withServer { server, _, client, output ->
            // Join first
            client.processInput("/join #test")
            Thread.sleep(50)
            server.readCommand()
            server.sendJoinConfirmation("testuser", "#test")
            Thread.sleep(100)

            server.sendPrivmsg("alice", "#test", "hi there")
            Thread.sleep(100)

            assertTrue(output.any { it == "<alice> hi there" })
        }
    }

    @Test fun `quit sends QUIT command`() {
        withServer { server, connection, client, _ ->
            client.processInput("/quit goodbye")
            Thread.sleep(50)

            val quit = server.readCommand()
            assertEquals("QUIT :goodbye", quit)
            assertFalse(client.running)
        }
    }

    @Test fun `nick change sends NICK command`() {
        withServer { server, _, client, _ ->
            client.processInput("/nick newnick")
            Thread.sleep(50)

            val nick = server.readCommand()
            assertEquals("NICK newnick", nick)
        }
    }
}
