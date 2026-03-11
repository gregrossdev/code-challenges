package dev.gregross.challenges.lb

import java.util.concurrent.atomic.AtomicInteger

class RoundRobin(private val backends: List<Backend>) {

    private val counter = AtomicInteger(0)

    fun next(): Backend? {
        val healthy = backends.filter { it.healthy }
        if (healthy.isEmpty()) return null
        val index = counter.getAndIncrement() % healthy.size
        return healthy[index]
    }
}
