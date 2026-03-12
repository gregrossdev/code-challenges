package dev.gregross.challenges.sort

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private val wordsResource = javaClass.getResourceAsStream("/words.txt")!!

    private fun sortWords(algorithm: String = "merge", unique: Boolean = false): List<String> {
        val input = wordsResource.bufferedReader().readText()
        val bais = ByteArrayInputStream(input.toByteArray())
        val baos = ByteArrayOutputStream()
        val processor = SortProcessor(algorithm = algorithm, unique = unique)
        processor.process(bais, PrintStream(baos))
        return baos.toString().trimEnd('\n').split("\n")
    }

    @Test fun `merge sort produces correct sorted output`() {
        val result = sortWords("merge")
        val expected = result.toMutableList()
        expected.sort()
        assertEquals(expected, result)
    }

    @Test fun `quick sort produces correct sorted output`() {
        val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
        val bais = ByteArrayInputStream(wordsInput.toByteArray())
        val baos = ByteArrayOutputStream()
        SortProcessor(algorithm = "quick").process(bais, PrintStream(baos))
        val result = baos.toString().trimEnd('\n').split("\n")
        val expected = result.toMutableList()
        expected.sort()
        assertEquals(expected, result)
    }

    @Test fun `heap sort produces correct sorted output`() {
        val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
        val bais = ByteArrayInputStream(wordsInput.toByteArray())
        val baos = ByteArrayOutputStream()
        SortProcessor(algorithm = "heap").process(bais, PrintStream(baos))
        val result = baos.toString().trimEnd('\n').split("\n")
        val expected = result.toMutableList()
        expected.sort()
        assertEquals(expected, result)
    }

    @Test fun `radix sort produces correct sorted output`() {
        val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
        val bais = ByteArrayInputStream(wordsInput.toByteArray())
        val baos = ByteArrayOutputStream()
        SortProcessor(algorithm = "radix").process(bais, PrintStream(baos))
        val result = baos.toString().trimEnd('\n').split("\n")
        val expected = result.toMutableList()
        expected.sort()
        assertEquals(expected, result)
    }

    @Test fun `unique removes duplicates from words list`() {
        val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
        val bais = ByteArrayInputStream(wordsInput.toByteArray())
        val baos = ByteArrayOutputStream()
        SortProcessor(unique = true).process(bais, PrintStream(baos))
        val result = baos.toString().trimEnd('\n').split("\n")
        // words.txt has 35 lines with some duplicates (apple, banana, cherry, grape, lemon repeated)
        assertEquals(result.size, result.toSet().size, "All lines should be unique")
        assertEquals(30, result.size, "Should have 30 unique words")
    }

    @Test fun `all algorithms produce same result`() {
        val algorithms = listOf("merge", "quick", "heap", "radix")
        val results = algorithms.map { algo ->
            val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
            val bais = ByteArrayInputStream(wordsInput.toByteArray())
            val baos = ByteArrayOutputStream()
            SortProcessor(algorithm = algo).process(bais, PrintStream(baos))
            baos.toString()
        }
        // All should produce identical output
        for (i in 1 until results.size) {
            assertEquals(results[0], results[i], "Algorithm ${algorithms[i]} differs from merge sort")
        }
    }

    @Test fun `random sort produces valid permutation of words`() {
        val wordsInput = javaClass.getResourceAsStream("/words.txt")!!.bufferedReader().readText()
        val inputLines = wordsInput.trimEnd('\n').split("\n").sorted()
        val bais = ByteArrayInputStream(wordsInput.toByteArray())
        val baos = ByteArrayOutputStream()
        SortProcessor(randomSort = true).process(bais, PrintStream(baos))
        val result = baos.toString().trimEnd('\n').split("\n").sorted()
        assertEquals(inputLines, result, "Random sort should be a permutation of input")
    }
}
