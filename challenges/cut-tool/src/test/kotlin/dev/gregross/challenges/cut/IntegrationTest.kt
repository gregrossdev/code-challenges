package dev.gregross.challenges.cut

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun processResource(path: String, delimiter: Char = '\t', fields: List<Int>): String {
        val input = this::class.java.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Test file not found: $path")
        val out = ByteArrayOutputStream()
        CutProcessor(delimiter, fields).process(input, PrintStream(out))
        return out.toString()
    }

    @Test fun `step1 - extract field 2 from sample tsv`() {
        val result = processResource("/sample.tsv", fields = listOf(2))
        val lines = result.trim().lines()
        assertEquals("f1", lines[0])
        assertEquals("1", lines[1])
        assertEquals("6", lines[2])
        assertEquals("11", lines[3])
    }

    @Test fun `step2 - extract field 1 from fourchords csv`() {
        val result = processResource("/fourchords.csv", delimiter = ',', fields = listOf(1))
        val lines = result.trim().lines()
        // First line is the BOM + header
        assertTrue(lines[0].endsWith("Song title"))
    }

    @Test fun `step2 - tab default still works for tsv`() {
        val result = processResource("/sample.tsv", fields = listOf(1))
        val lines = result.trim().lines()
        assertEquals("f0", lines[0])
        assertEquals("0", lines[1])
    }

    @Test fun `step3 - extract multiple fields from fourchords csv`() {
        val result = processResource("/fourchords.csv", delimiter = ',', fields = listOf(1, 2))
        val lines = result.trim().lines()
        // Second data line: "10000 Reasons (Bless the Lord)",Matt Redman and Jonas Myrin
        assertTrue(lines.size > 1)
        // Each line should have exactly one comma (two fields)
        for (line in lines.drop(1).take(5)) {
            val commaCount = line.count { it == ',' }
            assertTrue(commaCount >= 1, "Expected at least 1 comma in '$line'")
        }
    }

    @Test fun `sample tsv has correct field count`() {
        val result = processResource("/sample.tsv", fields = listOf(1, 2, 3, 4, 5))
        val lines = result.trim().lines()
        assertEquals(6, lines.size)
        assertEquals("f0\tf1\tf2\tf3\tf4", lines[0])
    }

    @Test fun `fourchords csv has many rows`() {
        val result = processResource("/fourchords.csv", delimiter = ',', fields = listOf(1))
        val lines = result.trim().lines()
        assertTrue(lines.size > 100, "Expected many rows in fourchords.csv, got ${lines.size}")
    }
}
