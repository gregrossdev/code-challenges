package dev.gregross.challenges.redis

import java.io.OutputStream

class RespSerializer(private val output: OutputStream) {

    fun serialize(value: RespValue) {
        when (value) {
            is RespValue.SimpleString -> {
                write("+${value.value}\r\n")
            }
            is RespValue.Error -> {
                write("-${value.message}\r\n")
            }
            is RespValue.Integer -> {
                write(":${value.value}\r\n")
            }
            is RespValue.BulkString -> {
                if (value.value == null) {
                    write("\$-1\r\n")
                } else {
                    val bytes = value.value.toByteArray()
                    write("\$${bytes.size}\r\n")
                    output.write(bytes)
                    write("\r\n")
                }
            }
            is RespValue.Array -> {
                if (value.elements == null) {
                    write("*-1\r\n")
                } else {
                    write("*${value.elements.size}\r\n")
                    for (element in value.elements) {
                        serialize(element)
                    }
                }
            }
        }
        output.flush()
    }

    private fun write(s: String) {
        output.write(s.toByteArray())
    }
}
