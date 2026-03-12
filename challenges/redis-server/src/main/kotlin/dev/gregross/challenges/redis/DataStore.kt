package dev.gregross.challenges.redis

import java.util.concurrent.ConcurrentHashMap

class DataStore {

    private val store = ConcurrentHashMap<String, StoreEntry>()

    fun get(key: String): StoreEntry? {
        val entry = store[key] ?: return null
        if (entry.isExpired()) {
            store.remove(key)
            return null
        }
        return entry
    }

    fun set(key: String, value: StoreValue, expiresAt: Long? = null) {
        store[key] = StoreEntry(value, expiresAt)
    }

    fun delete(key: String): Boolean {
        return store.remove(key) != null
    }

    fun exists(key: String): Boolean {
        val entry = store[key] ?: return false
        if (entry.isExpired()) {
            store.remove(key)
            return false
        }
        return true
    }
}

data class StoreEntry(
    val value: StoreValue,
    val expiresAt: Long?,
) {
    fun isExpired(): Boolean {
        return expiresAt != null && System.currentTimeMillis() > expiresAt
    }
}

sealed interface StoreValue {
    data class StringValue(val data: String) : StoreValue
    data class ListValue(val data: MutableList<String>) : StoreValue
}
