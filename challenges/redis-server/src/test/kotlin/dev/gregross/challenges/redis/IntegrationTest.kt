package dev.gregross.challenges.redis

import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun sendCommand(socket: Socket, vararg args: String): RespValue {
        val output = socket.getOutputStream()
        val serializer = RespSerializer(output)
        serializer.serialize(RespValue.Array(args.map { RespValue.BulkString(it) }))
        return RespParser(socket.getInputStream()).parse()
    }

    @Test fun `full workflow - all 11 commands`() {
        val server = RedisServer(18310)
        server.start()
        Thread.sleep(100)

        try {
            Socket("localhost", 18310).use { socket ->
                // PING
                assertEquals(RespValue.PONG, sendCommand(socket, "PING"))
                assertEquals(RespValue.BulkString("hi"), sendCommand(socket, "PING", "hi"))

                // ECHO
                assertEquals(RespValue.BulkString("hello"), sendCommand(socket, "ECHO", "hello"))

                // SET/GET
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "name", "redis"))
                assertEquals(RespValue.BulkString("redis"), sendCommand(socket, "GET", "name"))

                // GET nonexistent
                assertEquals(RespValue.NULL_BULK_STRING, sendCommand(socket, "GET", "nope"))

                // EXISTS
                assertEquals(RespValue.Integer(1), sendCommand(socket, "EXISTS", "name"))
                assertEquals(RespValue.Integer(0), sendCommand(socket, "EXISTS", "nope"))

                // INCR/DECR
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "counter", "5"))
                assertEquals(RespValue.Integer(6), sendCommand(socket, "INCR", "counter"))
                assertEquals(RespValue.Integer(5), sendCommand(socket, "DECR", "counter"))

                // INCR on new key
                assertEquals(RespValue.Integer(1), sendCommand(socket, "INCR", "fresh"))

                // LPUSH/RPUSH
                assertEquals(RespValue.Integer(1), sendCommand(socket, "LPUSH", "mylist", "a"))
                assertEquals(RespValue.Integer(2), sendCommand(socket, "RPUSH", "mylist", "b"))
                assertEquals(RespValue.Integer(3), sendCommand(socket, "LPUSH", "mylist", "c"))

                // DEL
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "temp", "x"))
                assertEquals(RespValue.Integer(1), sendCommand(socket, "DEL", "temp"))
                assertEquals(RespValue.NULL_BULK_STRING, sendCommand(socket, "GET", "temp"))

                // Unknown command
                val err = sendCommand(socket, "FOOBAR") as RespValue.Error
                assertTrue(err.message.contains("unknown command"))

                // WRONGTYPE
                val wrongType = sendCommand(socket, "GET", "mylist") as RespValue.Error
                assertTrue(wrongType.message.contains("WRONGTYPE"))
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `SET with EX expiry via TCP`() {
        val server = RedisServer(18311)
        server.start()
        Thread.sleep(100)

        try {
            Socket("localhost", 18311).use { socket ->
                assertEquals(RespValue.OK, sendCommand(socket, "SET", "ttl", "data", "PX", "200"))
                assertEquals(RespValue.BulkString("data"), sendCommand(socket, "GET", "ttl"))
                Thread.sleep(300)
                assertEquals(RespValue.NULL_BULK_STRING, sendCommand(socket, "GET", "ttl"))
            }
        } finally {
            server.stop()
        }
    }

    @Test fun `concurrent clients via TCP`() {
        val server = RedisServer(18312)
        server.start()
        Thread.sleep(100)

        try {
            val threads = (0 until 10).map { i ->
                Thread.startVirtualThread {
                    Socket("localhost", 18312).use { socket ->
                        sendCommand(socket, "SET", "k$i", "v$i")
                        val result = sendCommand(socket, "GET", "k$i")
                        assertEquals(RespValue.BulkString("v$i"), result)
                    }
                }
            }
            threads.forEach { it.join() }

            // Verify all keys exist from a single client
            Socket("localhost", 18312).use { socket ->
                val count = sendCommand(socket, "EXISTS",
                    "k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9")
                assertEquals(RespValue.Integer(10), count)
            }
        } finally {
            server.stop()
        }
    }
}
