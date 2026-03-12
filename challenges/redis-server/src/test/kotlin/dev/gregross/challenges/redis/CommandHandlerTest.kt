package dev.gregross.challenges.redis

import kotlin.test.Test
import kotlin.test.assertEquals

class CommandHandlerTest {

    private val store = DataStore()
    private val handler = CommandHandler(store)

    private fun exec(vararg args: String): RespValue = handler.execute(args.toList())

    // PING
    @Test fun `PING returns PONG`() {
        assertEquals(RespValue.PONG, exec("PING"))
    }

    @Test fun `PING with message echoes it`() {
        assertEquals(RespValue.BulkString("hello"), exec("PING", "hello"))
    }

    // ECHO
    @Test fun `ECHO returns message`() {
        assertEquals(RespValue.BulkString("hello"), exec("ECHO", "hello"))
    }

    @Test fun `ECHO with wrong args returns error`() {
        val result = exec("ECHO") as RespValue.Error
        assert(result.message.contains("wrong number"))
    }

    // SET/GET
    @Test fun `SET returns OK`() {
        assertEquals(RespValue.OK, exec("SET", "key", "value"))
    }

    @Test fun `GET returns stored value`() {
        exec("SET", "key", "value")
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
    }

    @Test fun `GET nonexistent returns null`() {
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "nonexistent"))
    }

    @Test fun `SET overwrites existing value`() {
        exec("SET", "key", "first")
        exec("SET", "key", "second")
        assertEquals(RespValue.BulkString("second"), exec("GET", "key"))
    }

    // Case insensitive
    @Test fun `commands are case insensitive`() {
        assertEquals(RespValue.PONG, exec("ping"))
        assertEquals(RespValue.OK, exec("set", "k", "v"))
        assertEquals(RespValue.BulkString("v"), exec("get", "k"))
    }

    // Unknown command
    @Test fun `unknown command returns error`() {
        val result = exec("FOOBAR") as RespValue.Error
        assert(result.message.contains("unknown command"))
    }

    // Wrong arg counts
    @Test fun `SET with too few args returns error`() {
        val result = exec("SET", "key") as RespValue.Error
        assert(result.message.contains("wrong number"))
    }

    @Test fun `GET with wrong args returns error`() {
        val result = exec("GET") as RespValue.Error
        assert(result.message.contains("wrong number"))
    }
}
