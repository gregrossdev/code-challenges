package dev.gregross.challenges.grep

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrepProcessorTest {

    private fun captureOutput(block: (PrintStream) -> Boolean): Pair<Boolean, String> {
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos)
        val result = block(ps)
        ps.flush()
        return result to baos.toString().trimEnd('\n')
    }

    private fun testFile(name: String): File {
        return File(javaClass.classLoader.getResource(name)!!.toURI())
    }

    @Test fun `match lines in file`() {
        val processor = GrepProcessor(Matcher("hello"))
        val (matched, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps)
        }
        assertTrue(matched)
        assertEquals("hello world", output)
    }

    @Test fun `no matches returns false`() {
        val processor = GrepProcessor(Matcher("zzzzz"))
        val (matched, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps)
        }
        assertFalse(matched)
        assertEquals("", output)
    }

    @Test fun `empty pattern matches all lines`() {
        val processor = GrepProcessor(Matcher(""))
        val (matched, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps)
        }
        assertTrue(matched)
        assertEquals(9, output.lines().size)
    }

    @Test fun `filename prefix when showFilename is true`() {
        val processor = GrepProcessor(Matcher("hello"))
        val (_, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps, showFilename = true)
        }
        assertTrue(output.contains(":hello world"))
    }

    @Test fun `stdin input`() {
        val processor = GrepProcessor(Matcher("hello"))
        val input = ByteArrayInputStream("hello world\ngoodbye world\n".toByteArray())
        val (matched, output) = captureOutput { ps ->
            processor.processStream(input, ps)
        }
        assertTrue(matched)
        assertEquals("hello world", output)
    }

    @Test fun `multiple matching lines`() {
        val processor = GrepProcessor(Matcher("world"))
        val (matched, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps)
        }
        assertTrue(matched)
        assertEquals(2, output.lines().size)
        assertTrue(output.contains("hello world"))
        assertTrue(output.contains("goodbye world"))
    }

    @Test fun `binary file is skipped`() {
        val tempFile = File.createTempFile("binary", ".bin")
        try {
            tempFile.writeBytes(byteArrayOf(0x48, 0x65, 0x6C, 0x00, 0x6F))
            val processor = GrepProcessor(Matcher(""))
            val (matched, _) = captureOutput { ps ->
                processor.processFile(tempFile, ps)
            }
            assertFalse(matched)
        } finally {
            tempFile.delete()
        }
    }

    @Test fun `case insensitive via matcher`() {
        val processor = GrepProcessor(Matcher("hello", ignoreCase = true))
        val (matched, output) = captureOutput { ps ->
            processor.processFile(testFile("test.txt"), ps)
        }
        assertTrue(matched)
        assertEquals(2, output.lines().size)
        assertTrue(output.contains("hello world"))
        assertTrue(output.contains("Hello World"))
    }

    @Test fun `invert match via matcher`() {
        val processor = GrepProcessor(Matcher("world", invert = true))
        val input = ByteArrayInputStream("hello world\ngoodbye\nfoo\n".toByteArray())
        val (matched, output) = captureOutput { ps ->
            processor.processStream(input, ps)
        }
        assertTrue(matched)
        assertEquals(2, output.lines().size)
        assertTrue(output.contains("goodbye"))
        assertTrue(output.contains("foo"))
    }
}
