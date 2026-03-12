package dev.gregross.challenges.sort

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SortProcessorTest {

    private fun process(input: String, algorithm: String = "merge", unique: Boolean = false, randomSort: Boolean = false): String {
        val bais = ByteArrayInputStream(input.toByteArray())
        val baos = ByteArrayOutputStream()
        val processor = SortProcessor(algorithm = algorithm, unique = unique, randomSort = randomSort)
        processor.process(bais, PrintStream(baos))
        return baos.toString().trimEnd('\n')
    }

    @Test fun `sorts with default merge algorithm`() {
        val result = process("banana\napple\ncherry")
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `sorts with quick sort`() {
        val result = process("banana\napple\ncherry", algorithm = "quick")
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `sorts with heap sort`() {
        val result = process("banana\napple\ncherry", algorithm = "heap")
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `sorts with radix sort`() {
        val result = process("banana\napple\ncherry", algorithm = "radix")
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `unique removes consecutive duplicates`() {
        val result = process("banana\napple\nbanana\napple\ncherry", unique = true)
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `unique with no duplicates`() {
        val result = process("apple\nbanana\ncherry", unique = true)
        assertEquals("apple\nbanana\ncherry", result)
    }

    @Test fun `empty input`() {
        val result = process("")
        assertEquals("", result)
    }

    @Test fun `random sort produces valid permutation`() {
        val input = "banana\napple\ncherry\ndate\nelderberry"
        val result = process(input, randomSort = true)
        val resultLines = result.split("\n").toSet()
        val inputLines = input.split("\n").toSet()
        assertEquals(inputLines, resultLines)
    }

    @Test fun `single line`() {
        val result = process("hello")
        assertEquals("hello", result)
    }
}
