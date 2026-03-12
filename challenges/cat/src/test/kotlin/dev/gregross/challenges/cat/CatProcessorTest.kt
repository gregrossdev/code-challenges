package dev.gregross.challenges.cat

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals

class CatProcessorTest {

    private fun process(
        input: String,
        numberAll: Boolean = false,
        numberNonBlank: Boolean = false,
    ): String {
        val processor = CatProcessor(numberAll, numberNonBlank)
        val out = ByteArrayOutputStream()
        processor.process(input.byteInputStream(), PrintStream(out))
        return out.toString()
    }

    private fun resource(name: String): String {
        return javaClass.classLoader.getResourceAsStream(name)!!.bufferedReader().readText()
    }

    @Test fun `outputs file contents`() {
        val result = process("hello\nworld\n")
        assertEquals("hello\nworld\n", result)
    }

    @Test fun `empty input produces no output`() {
        assertEquals("", process(""))
    }

    @Test fun `single line without newline`() {
        assertEquals("hello\n", process("hello"))
    }

    @Test fun `number all lines`() {
        val result = process("alpha\nbeta\ngamma\n", numberAll = true)
        assertEquals("     1\talpha\n     2\tbeta\n     3\tgamma\n", result)
    }

    @Test fun `number all includes blank lines`() {
        val result = process("alpha\n\nbeta\n", numberAll = true)
        assertEquals("     1\talpha\n     2\t\n     3\tbeta\n", result)
    }

    @Test fun `number non-blank skips blank lines`() {
        val result = process("alpha\n\nbeta\n", numberNonBlank = true)
        assertEquals("     1\talpha\n\n     2\tbeta\n", result)
    }

    @Test fun `number non-blank with multiple blanks`() {
        val result = process("a\n\n\nb\n", numberNonBlank = true)
        assertEquals("     1\ta\n\n\n     2\tb\n", result)
    }

    @Test fun `line counter persists across calls`() {
        val processor = CatProcessor(numberAll = true)
        val out = ByteArrayOutputStream()
        val ps = PrintStream(out)
        processor.process("alpha\nbeta\n".byteInputStream(), ps)
        processor.process("gamma\n".byteInputStream(), ps)
        val result = out.toString()
        assertEquals("     1\talpha\n     2\tbeta\n     3\tgamma\n", result)
    }

    @Test fun `test fixture file`() {
        val content = resource("test.txt")
        val result = process(content)
        assertEquals(content, result)
    }

    @Test fun `blanks file with number non-blank`() {
        val result = process("Line one\n\nLine three\n\nLine five\n", numberNonBlank = true)
        assertEquals("     1\tLine one\n\n     2\tLine three\n\n     3\tLine five\n", result)
    }
}
