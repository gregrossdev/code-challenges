package dev.gregross.challenges.lb

import java.util.concurrent.atomic.AtomicBoolean

data class Backend(
    val url: String,
) {
    private val _healthy = AtomicBoolean(true)

    var healthy: Boolean
        get() = _healthy.get()
        set(value) { _healthy.set(value) }

    override fun toString(): String = "$url (${if (healthy) "healthy" else "unhealthy"})"
}
