package dev.gregross.challenges.uniq

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals

class UniqProcessorTest {

    private fun process(
        input: String,
        count: Boolean = false,
        repeated: Boolean = false,
        unique: Boolean = false,
        ignoreCase: Boolean = false,
    ): String {
        val baos = ByteArrayOutputStream()
        UniqProcessor(count, repeated, unique, ignoreCase).process(
            ByteArrayInputStream(input.toByteArray()),
            PrintStream(baos),
        )
        return baos.toString().trimEnd('\n')
    }

    @Test fun `default removes adjacent duplicates`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\n"
        assertEquals("alpha\nbeta\ngamma", process(input))
    }

    @Test fun `non-adjacent duplicates are kept`() {
        val input = "alpha\nbeta\nalpha\n"
        assertEquals("alpha\nbeta\nalpha", process(input))
    }

    @Test fun `count flag prefixes with right-justified count`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\ngamma\n"
        val result = process(input, count = true)
        val lines = result.lines()
        assertEquals("      2 alpha", lines[0])
        assertEquals("      1 beta", lines[1])
        assertEquals("      3 gamma", lines[2])
    }

    @Test fun `repeated flag shows only repeated groups`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\n"
        val result = process(input, repeated = true)
        assertEquals("alpha\ngamma", result)
    }

    @Test fun `unique flag shows only non-repeated groups`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\n"
        assertEquals("beta", process(input, unique = true))
    }

    @Test fun `count and repeated combined`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\ngamma\n"
        val result = process(input, count = true, repeated = true)
        val lines = result.lines()
        assertEquals(2, lines.size)
        assertEquals("      2 alpha", lines[0])
        assertEquals("      3 gamma", lines[1])
    }

    @Test fun `repeated and unique together outputs nothing`() {
        val input = "alpha\nalpha\nbeta\n"
        assertEquals("", process(input, repeated = true, unique = true))
    }

    @Test fun `case insensitive comparison`() {
        val input = "Alpha\nalpha\nALPHA\nbeta\n"
        val result = process(input, ignoreCase = true)
        assertEquals("Alpha\nbeta", result)
    }

    @Test fun `case insensitive preserves first occurrence`() {
        val input = "HELLO\nhello\nHeLLo\n"
        assertEquals("HELLO", process(input, ignoreCase = true))
    }

    @Test fun `case insensitive with count`() {
        val input = "Alpha\nalpha\nALPHA\nbeta\n"
        val result = process(input, count = true, ignoreCase = true)
        val lines = result.lines()
        assertEquals("      3 Alpha", lines[0])
        assertEquals("      1 beta", lines[1])
    }

    @Test fun `empty input produces empty output`() {
        assertEquals("", process(""))
    }

    @Test fun `single line`() {
        assertEquals("hello", process("hello\n"))
    }

    @Test fun `all identical lines`() {
        val input = "same\nsame\nsame\nsame\n"
        assertEquals("same", process(input))
    }

    @Test fun `all identical with count`() {
        val input = "same\nsame\nsame\n"
        assertEquals("      3 same", process(input, count = true))
    }

    @Test fun `blank lines are adjacent duplicates`() {
        val input = "a\n\n\nb\n"
        val result = process(input)
        assertEquals("a\n\nb", result)
    }

    @Test fun `count unique only`() {
        val input = "alpha\nalpha\nbeta\ngamma\ngamma\n"
        val result = process(input, count = true, unique = true)
        assertEquals("      1 beta", result)
    }
}
