package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FrequencyCounterTest {

    @Test fun `counts single character`() {
        val freq = countFrequencies("aaa")
        assertEquals(mapOf('a' to 3), freq)
    }

    @Test fun `counts multiple characters`() {
        val freq = countFrequencies("abcab")
        assertEquals(3, freq.size)
        assertEquals(2, freq['a'])
        assertEquals(2, freq['b'])
        assertEquals(1, freq['c'])
    }

    @Test fun `counts whitespace and punctuation`() {
        val freq = countFrequencies("a b!")
        assertEquals(4, freq.size)
        assertEquals(1, freq['a'])
        assertEquals(1, freq[' '])
        assertEquals(1, freq['b'])
        assertEquals(1, freq['!'])
    }

    @Test fun `empty input returns empty map`() {
        val freq = countFrequencies("")
        assertEquals(emptyMap(), freq)
    }

    @Test fun `les miserables X frequency is 333`() {
        val text = this::class.java.getResourceAsStream("/lesmiserables.txt")!!
            .bufferedReader().readText()
        val freq = countFrequencies(text)
        assertEquals(333, freq['X'])
    }

    @Test fun `les miserables t frequency`() {
        val text = this::class.java.getResourceAsStream("/lesmiserables.txt")!!
            .bufferedReader().readText()
        val freq = countFrequencies(text)
        // Challenge says ~223,000 occurrences of 't'
        val tFreq = freq['t']!!
        assert(tFreq in 220000..226000) { "Expected ~223000 't' occurrences, got $tFreq" }
    }
}
