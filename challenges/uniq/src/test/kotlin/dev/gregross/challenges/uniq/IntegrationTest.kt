package dev.gregross.challenges.uniq

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun processResource(
        resourceName: String,
        count: Boolean = false,
        repeated: Boolean = false,
        unique: Boolean = false,
        ignoreCase: Boolean = false,
    ): String {
        val input = javaClass.classLoader.getResourceAsStream(resourceName)!!
        val baos = ByteArrayOutputStream()
        UniqProcessor(count, repeated, unique, ignoreCase).process(input, PrintStream(baos))
        return baos.toString().trimEnd('\n')
    }

    @Test fun `countries default dedup yields 246 lines`() {
        val result = processResource("countries.txt")
        assertEquals(246, result.lines().size)
    }

    @Test fun `countries with count shows counts`() {
        val result = processResource("countries.txt", count = true)
        val lines = result.lines()
        assertEquals(246, lines.size)
        assertTrue(lines.all { it.matches(Regex("^\\s+\\d+ .+$")) })
    }

    @Test fun `countries repeated only`() {
        val result = processResource("countries.txt", repeated = true)
        val lines = result.lines()
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.size < 246)
    }

    @Test fun `countries unique only`() {
        val result = processResource("countries.txt", unique = true)
        val lines = result.lines()
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.size < 246)
    }

    @Test fun `countries count and repeated combined`() {
        val result = processResource("countries.txt", count = true, repeated = true)
        val lines = result.lines()
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.all { it.matches(Regex("^\\s+[2-9]\\d* .+$|^\\s+\\d{2,} .+$")) })
    }

    @Test fun `repeated plus unique covers all lines`() {
        val repeatedResult = processResource("countries.txt", repeated = true)
        val uniqueResult = processResource("countries.txt", unique = true)
        val total = repeatedResult.lines().size + uniqueResult.lines().size
        assertEquals(246, total)
    }

    @Test fun `stdin input`() {
        val input = ByteArrayInputStream("foo\nfoo\nbar\n".toByteArray())
        val baos = ByteArrayOutputStream()
        UniqProcessor().process(input, PrintStream(baos))
        assertEquals("foo\nbar", baos.toString().trimEnd('\n'))
    }

    @Test fun `test txt fixture`() {
        val result = processResource("test.txt")
        val lines = result.lines()
        assertEquals(6, lines.size)
    }

    @Test fun `case insensitive on mixed case`() {
        val input = ByteArrayInputStream("Hello\nhello\nHELLO\nWorld\n".toByteArray())
        val baos = ByteArrayOutputStream()
        UniqProcessor(ignoreCase = true).process(input, PrintStream(baos))
        assertEquals("Hello\nWorld", baos.toString().trimEnd('\n'))
    }
}
