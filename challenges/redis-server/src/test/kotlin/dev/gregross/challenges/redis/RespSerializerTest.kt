package dev.gregross.challenges.redis

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class RespSerializerTest {

    private fun serialize(value: RespValue): String {
        val baos = ByteArrayOutputStream()
        RespSerializer(baos).serialize(value)
        return baos.toString()
    }

    private fun roundTrip(value: RespValue): RespValue {
        val bytes = serialize(value).toByteArray()
        return RespParser(ByteArrayInputStream(bytes)).parse()
    }

    @Test fun `simple string`() {
        assertEquals("+OK\r\n", serialize(RespValue.SimpleString("OK")))
    }

    @Test fun `error`() {
        assertEquals("-ERR unknown\r\n", serialize(RespValue.Error("ERR unknown")))
    }

    @Test fun `integer`() {
        assertEquals(":42\r\n", serialize(RespValue.Integer(42)))
    }

    @Test fun `bulk string`() {
        assertEquals("\$5\r\nhello\r\n", serialize(RespValue.BulkString("hello")))
    }

    @Test fun `null bulk string`() {
        assertEquals("\$-1\r\n", serialize(RespValue.BulkString(null)))
    }

    @Test fun `empty bulk string`() {
        assertEquals("\$0\r\n\r\n", serialize(RespValue.BulkString("")))
    }

    @Test fun `array`() {
        val value = RespValue.Array(listOf(RespValue.BulkString("hello"), RespValue.BulkString("world")))
        assertEquals("*2\r\n\$5\r\nhello\r\n\$5\r\nworld\r\n", serialize(value))
    }

    @Test fun `null array`() {
        assertEquals("*-1\r\n", serialize(RespValue.Array(null)))
    }

    @Test fun `empty array`() {
        assertEquals("*0\r\n", serialize(RespValue.Array(emptyList())))
    }

    // Round-trip tests
    @Test fun `round-trip simple string`() {
        assertEquals(RespValue.SimpleString("OK"), roundTrip(RespValue.SimpleString("OK")))
    }

    @Test fun `round-trip bulk string`() {
        assertEquals(RespValue.BulkString("hello"), roundTrip(RespValue.BulkString("hello")))
    }

    @Test fun `round-trip integer`() {
        assertEquals(RespValue.Integer(42), roundTrip(RespValue.Integer(42)))
    }

    @Test fun `round-trip array`() {
        val value = RespValue.Array(listOf(
            RespValue.BulkString("SET"),
            RespValue.BulkString("key"),
            RespValue.BulkString("value"),
        ))
        assertEquals(value, roundTrip(value))
    }
}
