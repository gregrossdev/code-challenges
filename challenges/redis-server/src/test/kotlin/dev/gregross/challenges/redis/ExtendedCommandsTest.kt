package dev.gregross.challenges.redis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtendedCommandsTest {

    private val store = DataStore()
    private val handler = CommandHandler(store)

    private fun exec(vararg args: String): RespValue = handler.execute(args.toList())

    // EXISTS
    @Test fun `EXISTS returns 1 for existing key`() {
        exec("SET", "key", "value")
        assertEquals(RespValue.Integer(1), exec("EXISTS", "key"))
    }

    @Test fun `EXISTS returns 0 for missing key`() {
        assertEquals(RespValue.Integer(0), exec("EXISTS", "missing"))
    }

    @Test fun `EXISTS counts multiple keys`() {
        exec("SET", "a", "1")
        exec("SET", "b", "2")
        assertEquals(RespValue.Integer(2), exec("EXISTS", "a", "b", "c"))
    }

    // DEL
    @Test fun `DEL removes key`() {
        exec("SET", "key", "value")
        assertEquals(RespValue.Integer(1), exec("DEL", "key"))
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "key"))
    }

    @Test fun `DEL returns 0 for missing key`() {
        assertEquals(RespValue.Integer(0), exec("DEL", "missing"))
    }

    @Test fun `DEL counts multiple deletions`() {
        exec("SET", "a", "1")
        exec("SET", "b", "2")
        assertEquals(RespValue.Integer(2), exec("DEL", "a", "b", "c"))
    }

    // INCR
    @Test fun `INCR increments existing integer`() {
        exec("SET", "counter", "10")
        assertEquals(RespValue.Integer(11), exec("INCR", "counter"))
        assertEquals(RespValue.BulkString("11"), exec("GET", "counter"))
    }

    @Test fun `INCR creates key at 1 if missing`() {
        assertEquals(RespValue.Integer(1), exec("INCR", "new"))
        assertEquals(RespValue.BulkString("1"), exec("GET", "new"))
    }

    @Test fun `INCR on non-integer returns error`() {
        exec("SET", "str", "hello")
        val result = exec("INCR", "str") as RespValue.Error
        assertTrue(result.message.contains("not an integer"))
    }

    // DECR
    @Test fun `DECR decrements existing integer`() {
        exec("SET", "counter", "10")
        assertEquals(RespValue.Integer(9), exec("DECR", "counter"))
    }

    @Test fun `DECR creates key at -1 if missing`() {
        assertEquals(RespValue.Integer(-1), exec("DECR", "new"))
    }

    // LPUSH
    @Test fun `LPUSH creates list and prepends`() {
        assertEquals(RespValue.Integer(1), exec("LPUSH", "list", "a"))
        assertEquals(RespValue.Integer(2), exec("LPUSH", "list", "b"))
        // b is at head, a is at tail
    }

    @Test fun `LPUSH multiple values`() {
        assertEquals(RespValue.Integer(3), exec("LPUSH", "list", "a", "b", "c"))
    }

    @Test fun `LPUSH on string key returns WRONGTYPE`() {
        exec("SET", "str", "hello")
        val result = exec("LPUSH", "str", "a") as RespValue.Error
        assertTrue(result.message.contains("WRONGTYPE"))
    }

    // RPUSH
    @Test fun `RPUSH creates list and appends`() {
        assertEquals(RespValue.Integer(1), exec("RPUSH", "list", "a"))
        assertEquals(RespValue.Integer(2), exec("RPUSH", "list", "b"))
    }

    @Test fun `RPUSH multiple values`() {
        assertEquals(RespValue.Integer(3), exec("RPUSH", "list", "a", "b", "c"))
    }

    @Test fun `RPUSH on string key returns WRONGTYPE`() {
        exec("SET", "str", "hello")
        val result = exec("RPUSH", "str", "a") as RespValue.Error
        assertTrue(result.message.contains("WRONGTYPE"))
    }

    // GET on list returns WRONGTYPE
    @Test fun `GET on list returns WRONGTYPE`() {
        exec("LPUSH", "list", "a")
        val result = exec("GET", "list") as RespValue.Error
        assertTrue(result.message.contains("WRONGTYPE"))
    }

    // INCR on list returns WRONGTYPE
    @Test fun `INCR on list returns WRONGTYPE`() {
        exec("LPUSH", "list", "a")
        val result = exec("INCR", "list") as RespValue.Error
        assertTrue(result.message.contains("WRONGTYPE"))
    }
}
