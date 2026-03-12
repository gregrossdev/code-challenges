package dev.gregross.challenges.redis

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpiryTest {

    private val store = DataStore()
    private val handler = CommandHandler(store)

    private fun exec(vararg args: String): RespValue = handler.execute(args.toList())

    @Test fun `SET with EX expires after seconds`() {
        exec("SET", "key", "value", "EX", "1")
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
        Thread.sleep(1100)
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "key"))
    }

    @Test fun `SET with PX expires after milliseconds`() {
        exec("SET", "key", "value", "PX", "200")
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
        Thread.sleep(300)
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "key"))
    }

    @Test fun `SET with EXAT expires at unix timestamp`() {
        val futureSeconds = (System.currentTimeMillis() / 1000) + 1
        exec("SET", "key", "value", "EXAT", futureSeconds.toString())
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
        Thread.sleep(1100)
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "key"))
    }

    @Test fun `SET with PXAT expires at unix timestamp millis`() {
        val futureMillis = System.currentTimeMillis() + 200
        exec("SET", "key", "value", "PXAT", futureMillis.toString())
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
        Thread.sleep(300)
        assertEquals(RespValue.NULL_BULK_STRING, exec("GET", "key"))
    }

    @Test fun `EXISTS returns false for expired key`() {
        exec("SET", "key", "value", "PX", "100")
        Thread.sleep(200)
        assertEquals(RespValue.Integer(0), exec("EXISTS", "key"))
    }

    @Test fun `SET without expiry does not expire`() {
        exec("SET", "key", "value")
        Thread.sleep(100)
        assertEquals(RespValue.BulkString("value"), exec("GET", "key"))
    }
}
