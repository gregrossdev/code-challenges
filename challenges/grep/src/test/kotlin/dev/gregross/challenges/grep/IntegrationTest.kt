package dev.gregross.challenges.grep

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun testFile(name: String): File {
        return File(javaClass.classLoader.getResource(name)!!.toURI())
    }

    private fun grep(
        pattern: String,
        filePaths: List<String>,
        ignoreCase: Boolean = false,
        invert: Boolean = false,
        recursive: Boolean = false,
    ): Pair<Boolean, String> {
        val matcher = Matcher(pattern, ignoreCase = ignoreCase, invert = invert)
        val processor = GrepProcessor(matcher)
        val searcher = FileSearcher()
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos)

        val files = searcher.findFiles(filePaths, recursive)
        val showFilename = files.size > 1 || recursive
        val matched = files.fold(false) { acc, file ->
            processor.processFile(file, ps, showFilename) || acc
        }
        ps.flush()
        return matched to baos.toString().trimEnd('\n')
    }

    @Test fun `step 1 - empty pattern matches all lines`() {
        val (matched, output) = grep("", listOf(testFile("test.txt").absolutePath))
        assertTrue(matched)
        assertEquals(9, output.lines().size)
    }

    @Test fun `step 2 - single character match`() {
        val (matched, output) = grep("x", listOf(testFile("test.txt").absolutePath))
        assertTrue(matched)
        assertTrue(output.contains("mixed Case Line"))
    }

    @Test fun `step 3 - recursive search`() {
        val (matched, output) = grep(
            "apple",
            listOf(testFile("testdir").absolutePath),
            recursive = true,
        )
        assertTrue(matched)
        assertEquals(2, output.lines().size)
        assertTrue(output.lines().all { it.contains(":") })
    }

    @Test fun `step 4 - invert match`() {
        val (matched, output) = grep(
            "world",
            listOf(testFile("test.txt").absolutePath),
            invert = true,
        )
        assertTrue(matched)
        assertTrue(output.lines().none { it.contains("world") })
    }

    @Test fun `step 5 - digit and word classes`() {
        val (matched, output) = grep("\\d", listOf(testFile("test.txt").absolutePath))
        assertTrue(matched)
        assertTrue(output.contains("123"))
        assertTrue(output.lines().size == 1)
    }

    @Test fun `step 6 - anchors`() {
        val (matched, output) = grep("^start", listOf(testFile("test.txt").absolutePath))
        assertTrue(matched)
        assertEquals("start of line", output)
    }

    @Test fun `step 6 - end anchor`() {
        val (matched, output) = grep("line$", listOf(testFile("test.txt").absolutePath))
        assertTrue(matched)
        assertTrue(output.contains("end of the line"))
        assertTrue(output.contains("start of line"))
    }

    @Test fun `step 7 - case insensitive`() {
        val (matched, output) = grep(
            "hello",
            listOf(testFile("test.txt").absolutePath),
            ignoreCase = true,
        )
        assertTrue(matched)
        assertEquals(2, output.lines().size)
        assertTrue(output.contains("hello world"))
        assertTrue(output.contains("Hello World"))
    }

    @Test fun `multi-file shows filename prefix`() {
        val (matched, output) = grep(
            "apple",
            listOf(
                testFile("testdir/a.txt").absolutePath,
                testFile("testdir/sub/c.txt").absolutePath,
            ),
        )
        assertTrue(matched)
        assertTrue(output.lines().all { ":" in it })
    }
}
