package dev.gregross.challenges.redis

class CommandHandler(private val store: DataStore) {

    fun execute(args: List<String>): RespValue {
        if (args.isEmpty()) return RespValue.Error("ERR no command")

        val command = args[0].uppercase()
        val params = args.subList(1, args.size)

        return when (command) {
            "PING" -> handlePing(params)
            "ECHO" -> handleEcho(params)
            "SET" -> handleSet(params)
            "GET" -> handleGet(params)
            "EXISTS" -> handleExists(params)
            "DEL" -> handleDel(params)
            "INCR" -> handleIncr(params)
            "DECR" -> handleDecr(params)
            "LPUSH" -> handleLpush(params)
            "RPUSH" -> handleRpush(params)
            else -> RespValue.Error("ERR unknown command '$command'")
        }
    }

    private fun handlePing(params: List<String>): RespValue {
        return if (params.isEmpty()) {
            RespValue.PONG
        } else {
            RespValue.BulkString(params[0])
        }
    }

    private fun handleEcho(params: List<String>): RespValue {
        if (params.size != 1) return RespValue.Error("ERR wrong number of arguments for 'echo' command")
        return RespValue.BulkString(params[0])
    }

    private fun handleSet(params: List<String>): RespValue {
        if (params.size < 2) return RespValue.Error("ERR wrong number of arguments for 'set' command")

        val key = params[0]
        val value = params[1]
        var expiresAt: Long? = null

        var i = 2
        while (i < params.size) {
            val option = params[i].uppercase()
            if (i + 1 >= params.size) return RespValue.Error("ERR syntax error")
            val optionValue = params[i + 1].toLongOrNull()
                ?: return RespValue.Error("ERR value is not an integer or out of range")

            expiresAt = when (option) {
                "EX" -> System.currentTimeMillis() + optionValue * 1000
                "PX" -> System.currentTimeMillis() + optionValue
                "EXAT" -> optionValue * 1000
                "PXAT" -> optionValue
                else -> return RespValue.Error("ERR syntax error")
            }
            i += 2
        }

        store.set(key, StoreValue.StringValue(value), expiresAt)
        return RespValue.OK
    }

    private fun handleGet(params: List<String>): RespValue {
        if (params.size != 1) return RespValue.Error("ERR wrong number of arguments for 'get' command")
        val entry = store.get(params[0]) ?: return RespValue.NULL_BULK_STRING
        return when (val v = entry.value) {
            is StoreValue.StringValue -> RespValue.BulkString(v.data)
            is StoreValue.ListValue -> RespValue.Error("WRONGTYPE Operation against a key holding the wrong kind of value")
        }
    }

    private fun handleExists(params: List<String>): RespValue {
        if (params.isEmpty()) return RespValue.Error("ERR wrong number of arguments for 'exists' command")
        val count = params.count { store.exists(it) }
        return RespValue.Integer(count.toLong())
    }

    private fun handleDel(params: List<String>): RespValue {
        if (params.isEmpty()) return RespValue.Error("ERR wrong number of arguments for 'del' command")
        val count = params.count { store.delete(it) }
        return RespValue.Integer(count.toLong())
    }

    private fun handleIncr(params: List<String>): RespValue {
        if (params.size != 1) return RespValue.Error("ERR wrong number of arguments for 'incr' command")
        return incrBy(params[0], 1)
    }

    private fun handleDecr(params: List<String>): RespValue {
        if (params.size != 1) return RespValue.Error("ERR wrong number of arguments for 'decr' command")
        return incrBy(params[0], -1)
    }

    private fun incrBy(key: String, delta: Long): RespValue {
        val entry = store.get(key)
        val current = if (entry == null) {
            0L
        } else {
            when (val v = entry.value) {
                is StoreValue.StringValue -> {
                    v.data.toLongOrNull()
                        ?: return RespValue.Error("ERR value is not an integer or out of range")
                }
                is StoreValue.ListValue -> {
                    return RespValue.Error("WRONGTYPE Operation against a key holding the wrong kind of value")
                }
            }
        }

        val newValue = current + delta
        store.set(key, StoreValue.StringValue(newValue.toString()))
        return RespValue.Integer(newValue)
    }

    private fun handleLpush(params: List<String>): RespValue {
        if (params.size < 2) return RespValue.Error("ERR wrong number of arguments for 'lpush' command")
        return listPush(params[0], params.subList(1, params.size), prepend = true)
    }

    private fun handleRpush(params: List<String>): RespValue {
        if (params.size < 2) return RespValue.Error("ERR wrong number of arguments for 'rpush' command")
        return listPush(params[0], params.subList(1, params.size), prepend = false)
    }

    private fun listPush(key: String, values: List<String>, prepend: Boolean): RespValue {
        val entry = store.get(key)
        val list = if (entry == null) {
            val newList = mutableListOf<String>()
            store.set(key, StoreValue.ListValue(newList))
            newList
        } else {
            when (val v = entry.value) {
                is StoreValue.ListValue -> v.data
                is StoreValue.StringValue -> {
                    return RespValue.Error("WRONGTYPE Operation against a key holding the wrong kind of value")
                }
            }
        }

        if (prepend) {
            for (value in values) {
                list.add(0, value)
            }
        } else {
            list.addAll(values)
        }

        return RespValue.Integer(list.size.toLong())
    }
}
