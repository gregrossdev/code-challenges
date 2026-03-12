package dev.gregross.challenges.diff

import kotlin.test.Test
import kotlin.test.assertEquals

class LcsComputerTest {

    private val lcs = LcsComputer()

    // Challenge Step 1 test cases — character-level LCS

    @Test fun `identical strings`() {
        assertEquals("ABCDEF", lcs.computeString("ABCDEF", "ABCDEF"))
    }

    @Test fun `completely different strings`() {
        assertEquals("", lcs.computeString("ABC", "XYZ"))
    }

    @Test fun `partial overlap`() {
        assertEquals("XY", lcs.computeString("AABCXY", "XYZ"))
    }

    @Test fun `both empty`() {
        assertEquals("", lcs.computeString("", ""))
    }

    @Test fun `subsequence`() {
        assertEquals("AC", lcs.computeString("ABCD", "AC"))
    }

    @Test fun `one empty string`() {
        assertEquals("", lcs.computeString("ABC", ""))
    }

    @Test fun `single character match`() {
        assertEquals("A", lcs.computeString("A", "A"))
    }

    // Line-level LCS tests

    @Test fun `identical lines`() {
        val lines = listOf("alpha", "beta", "gamma")
        assertEquals(lines, lcs.compute(lines, lines))
    }

    @Test fun `no common lines`() {
        val a = listOf("alpha", "beta")
        val b = listOf("gamma", "delta")
        assertEquals(emptyList(), lcs.compute(a, b))
    }

    @Test fun `both empty line lists`() {
        assertEquals(emptyList(), lcs.compute(emptyList(), emptyList()))
    }

    @Test fun `partial common lines`() {
        val a = listOf("alpha", "beta", "gamma", "delta")
        val b = listOf("alpha", "gamma", "epsilon")
        assertEquals(listOf("alpha", "gamma"), lcs.compute(a, b))
    }

    @Test fun `one line removed from middle`() {
        val a = listOf("first", "second", "third")
        val b = listOf("first", "third")
        assertEquals(listOf("first", "third"), lcs.compute(a, b))
    }

    @Test fun `one line added in middle`() {
        val a = listOf("first", "third")
        val b = listOf("first", "second", "third")
        assertEquals(listOf("first", "third"), lcs.compute(a, b))
    }
}
