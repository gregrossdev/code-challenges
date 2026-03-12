package dev.gregross.challenges.redis

import java.io.ByteArrayOutputStream
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals

class RedisServerTest {

    private fun sendCommand(socket: Socket, vararg args: String): RespValue {
        val output = socket.getOutputStream()
        val serializer = RespSerializer(output)
        serializer.serialize(RespValue.Array(args.map { RespValue.BulkString(it) }))
        return RespParser(socket.getInputStream()).parse()
    }

    @Test fun `PING via TCP`() {
        val server = RedisServer(18300)
        server.start()
        Thread.sleep(100)

        try {
            Socket("localhost", 18300).use { socket ->
                val response = sendCommand(socket, "PING")
                assertEquals(RespValue.PONG, response)
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `SET and GET via TCP`() {
        val server = RedisServer(18301)
        server.start()
        Thread.sleep(100)

        try {
            Socket("localhost", 18301).use { socket ->
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "foo", "bar"))
                assertEquals(RespValue.BulkString("bar"), sendCommand(socket, "GET", "foo"))
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `multiple commands on one connection`() {
        val server = RedisServer(18302)
        server.start()
        Thread.sleep(100)

        try {
            Socket("localhost", 18302).use { socket ->
                assertEquals(RespValue.PONG, sendCommand(socket, "PING"))
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "x", "10"))
                assertEquals(RespValue.BulkString("10"), sendCommand(socket, "GET", "x"))
                assertEquals(RespValue.BulkString("hello"), sendCommand(socket, "ECHO", "hello"))
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `concurrent clients share data`() {
        val server = RedisServer(18303)
        server.start()
        Thread.sleep(100)

        try {
            // Client 1 sets a value
            Socket("localhost", 18303).use { socket1 ->
                sendCommand(socket1, "SET", "shared", "data")
            }

            // Client 2 reads it
            Socket("localhost", 18303).use { socket2 ->
                assertEquals(RespValue.BulkString("data"), sendCommand(socket2, "GET", "shared"))
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `multiple concurrent clients`() {
        val server = RedisServer(18304)
        server.start()
        Thread.sleep(100)

        try {
            val threads = (0 until 5).map { i ->
                Thread.startVirtualThread {
                    Socket("localhost", 18304).use { socket ->
                        sendCommand(socket, "SET", "key$i", "val$i")
                        val result = sendCommand(socket, "GET", "key$i")
                        assertEquals(RespValue.BulkString("val$i"), result)
                    }
                }
            }
            threads.forEach { it.join() }
        } finally {
            server.stop()
        }
    }
}
