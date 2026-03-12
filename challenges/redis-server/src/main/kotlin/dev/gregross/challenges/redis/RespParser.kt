package dev.gregross.challenges.redis

import java.io.InputStream

class RespParser(private val input: InputStream) {

    fun parse(): RespValue {
        val type = input.read()
        if (type == -1) throw RespException("Unexpected end of stream")

        return when (type.toChar()) {
            '+' -> RespValue.SimpleString(readLine())
            '-' -> RespValue.Error(readLine())
            ':' -> RespValue.Integer(readLine().toLong())
            '$' -> parseBulkString()
            '*' -> parseArray()
            else -> throw RespException("Unknown RESP type: '${type.toChar()}'")
        }
    }

    private fun parseBulkString(): RespValue.BulkString {
        val length = readLine().toInt()
        if (length == -1) return RespValue.NULL_BULK_STRING

        val data = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(data, read, length - read)
            if (n == -1) throw RespException("Unexpected end of stream in bulk string")
            read += n
        }
        // Consume trailing \r\n
        input.read() // \r
        input.read() // \n

        return RespValue.BulkString(String(data))
    }

    private fun parseArray(): RespValue.Array {
        val count = readLine().toInt()
        if (count == -1) return RespValue.NULL_ARRAY

        val elements = (0 until count).map { parse() }
        return RespValue.Array(elements)
    }

    private fun readLine(): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) throw RespException("Unexpected end of stream")
            if (b.toChar() == '\r') {
                input.read() // consume \n
                return sb.toString()
            }
            sb.append(b.toChar())
        }
    }
}

class RespException(message: String) : RuntimeException(message)
