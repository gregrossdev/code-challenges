package dev.gregross.challenges.wc

import kotlin.test.Test
import kotlin.test.assertEquals

class WordCountTest {

    private fun testFileCounts(): Counts {
        val input = this::class.java.getResourceAsStream("/test.txt")!!
        return countFromStream(input)
    }

    @Test
    fun `count bytes matches expected`() {
        val counts = testFileCounts()
        assertEquals(342190L, counts.bytes)
    }

    @Test
    fun `count lines matches expected`() {
        val counts = testFileCounts()
        assertEquals(7145L, counts.lines)
    }

    @Test
    fun `count words matches expected`() {
        val counts = testFileCounts()
        assertEquals(58164L, counts.words)
    }

    @Test
    fun `count chars matches expected`() {
        val counts = testFileCounts()
        assertEquals(339292L, counts.chars)
    }

    @Test
    fun `empty input returns all zeros`() {
        val counts = countFromStream("".byteInputStream())
        assertEquals(Counts(lines = 0, words = 0, bytes = 0, chars = 0), counts)
    }

    @Test
    fun `single line no newline`() {
        val counts = countFromStream("hello world".byteInputStream())
        assertEquals(0L, counts.lines)
        assertEquals(2L, counts.words)
        assertEquals(11L, counts.bytes)
        assertEquals(11L, counts.chars)
    }

    @Test
    fun `multibyte characters counted correctly`() {
        val text = "café\n"
        val counts = countFromStream(text.byteInputStream())
        assertEquals(1L, counts.lines)
        assertEquals(1L, counts.words)
        assertEquals(5L, counts.chars)
        assertEquals(6L, counts.bytes) // é is 2 bytes in UTF-8
    }
}
