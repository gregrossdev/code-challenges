package dev.gregross.challenges.cut

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals

class CutProcessorTest {

    private fun process(input: String, delimiter: Char = '\t', fields: List<Int>): String {
        val out = ByteArrayOutputStream()
        CutProcessor(delimiter, fields).process(
            ByteArrayInputStream(input.toByteArray()),
            PrintStream(out),
        )
        return out.toString().trimEnd('\n')
    }

    @Test fun `extracts single field with tab delimiter`() {
        val input = "a\tb\tc\n1\t2\t3"
        assertEquals("b\n2", process(input, fields = listOf(2)))
    }

    @Test fun `extracts multiple fields`() {
        val input = "a\tb\tc\td\n1\t2\t3\t4"
        assertEquals("a\tc\n1\t3", process(input, fields = listOf(1, 3)))
    }

    @Test fun `uses custom delimiter`() {
        val input = "a,b,c\n1,2,3"
        assertEquals("b\n2", process(input, delimiter = ',', fields = listOf(2)))
    }

    @Test fun `default delimiter is tab`() {
        val input = "x\ty\tz"
        assertEquals("y", process(input, fields = listOf(2)))
    }

    @Test fun `passes through lines without delimiter`() {
        val input = "no-delimiter-here\na\tb\tc"
        assertEquals("no-delimiter-here\nb", process(input, fields = listOf(2)))
    }

    @Test fun `handles missing fields gracefully`() {
        val input = "a\tb"
        assertEquals("", process(input, fields = listOf(5)))
    }

    @Test fun `handles field index out of range`() {
        val input = "a\tb\tc"
        assertEquals("a\tc", process(input, fields = listOf(1, 3, 10)))
    }

    @Test fun `outputs fields joined by delimiter`() {
        val input = "a,b,c,d"
        assertEquals("b,d", process(input, delimiter = ',', fields = listOf(2, 4)))
    }

    @Test fun `single field single line`() {
        val input = "one\ttwo\tthree"
        assertEquals("one", process(input, fields = listOf(1)))
    }
}
