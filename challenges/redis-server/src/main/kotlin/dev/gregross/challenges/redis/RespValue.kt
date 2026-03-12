package dev.gregross.challenges.redis

sealed interface RespValue {
    data class SimpleString(val value: String) : RespValue
    data class Error(val message: String) : RespValue
    data class Integer(val value: Long) : RespValue
    data class BulkString(val value: String?) : RespValue
    data class Array(val elements: List<RespValue>?) : RespValue

    companion object {
        val OK = SimpleString("OK")
        val PONG = SimpleString("PONG")
        val NULL_BULK_STRING = BulkString(null)
        val NULL_ARRAY = Array(null)
    }
}
