package dev.gregross.challenges.redis

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RespParserTest {

    private fun parse(input: String): RespValue {
        return RespParser(ByteArrayInputStream(input.toByteArray())).parse()
    }

    @Test fun `simple string`() {
        val result = parse("+OK\r\n")
        assertEquals(RespValue.SimpleString("OK"), result)
    }

    @Test fun `simple string with spaces`() {
        val result = parse("+Hello World\r\n")
        assertEquals(RespValue.SimpleString("Hello World"), result)
    }

    @Test fun `error`() {
        val result = parse("-ERR unknown command\r\n")
        assertEquals(RespValue.Error("ERR unknown command"), result)
    }

    @Test fun `integer`() {
        val result = parse(":42\r\n")
        assertEquals(RespValue.Integer(42), result)
    }

    @Test fun `negative integer`() {
        val result = parse(":-1\r\n")
        assertEquals(RespValue.Integer(-1), result)
    }

    @Test fun `zero integer`() {
        val result = parse(":0\r\n")
        assertEquals(RespValue.Integer(0), result)
    }

    @Test fun `bulk string`() {
        val result = parse("\$5\r\nhello\r\n")
        assertEquals(RespValue.BulkString("hello"), result)
    }

    @Test fun `empty bulk string`() {
        val result = parse("\$0\r\n\r\n")
        assertEquals(RespValue.BulkString(""), result)
    }

    @Test fun `null bulk string`() {
        val result = parse("\$-1\r\n") as RespValue.BulkString
        assertNull(result.value)
    }

    @Test fun `array`() {
        val result = parse("*2\r\n\$5\r\nhello\r\n\$5\r\nworld\r\n")
        assertEquals(
            RespValue.Array(listOf(RespValue.BulkString("hello"), RespValue.BulkString("world"))),
            result,
        )
    }

    @Test fun `empty array`() {
        val result = parse("*0\r\n")
        assertEquals(RespValue.Array(emptyList()), result)
    }

    @Test fun `null array`() {
        val result = parse("*-1\r\n") as RespValue.Array
        assertNull(result.elements)
    }

    @Test fun `PING command`() {
        val result = parse("*1\r\n\$4\r\nPING\r\n")
        assertEquals(
            RespValue.Array(listOf(RespValue.BulkString("PING"))),
            result,
        )
    }

    @Test fun `SET command`() {
        val result = parse("*3\r\n\$3\r\nSET\r\n\$3\r\nkey\r\n\$5\r\nvalue\r\n")
        assertEquals(
            RespValue.Array(listOf(
                RespValue.BulkString("SET"),
                RespValue.BulkString("key"),
                RespValue.BulkString("value"),
            )),
            result,
        )
    }

    @Test fun `mixed type array`() {
        val result = parse("*3\r\n:1\r\n:2\r\n:3\r\n")
        assertEquals(
            RespValue.Array(listOf(
                RespValue.Integer(1),
                RespValue.Integer(2),
                RespValue.Integer(3),
            )),
            result,
        )
    }

    @Test fun `nested array`() {
        val result = parse("*2\r\n*2\r\n:1\r\n:2\r\n*2\r\n:3\r\n:4\r\n")
        assertEquals(
            RespValue.Array(listOf(
                RespValue.Array(listOf(RespValue.Integer(1), RespValue.Integer(2))),
                RespValue.Array(listOf(RespValue.Integer(3), RespValue.Integer(4))),
            )),
            result,
        )
    }
}
