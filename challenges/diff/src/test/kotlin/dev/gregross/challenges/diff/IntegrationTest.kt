package dev.gregross.challenges.diff

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun resource(name: String): String {
        return javaClass.classLoader.getResource(name)!!.file
    }

    private fun runDiff(vararg args: String): ProcessResult {
        val pb = ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "dev.gregross.challenges.diff.MainKt", *args,
        )
        pb.redirectErrorStream(false)
        val process = pb.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, stdout, stderr)
    }

    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    @Test fun `identical files exit 0 with no output`() {
        val result = runDiff(resource("original.txt"), resource("identical.txt"))
        assertEquals(0, result.exitCode)
        assertEquals("", result.stdout)
    }

    @Test fun `different files exit 1 with diff output`() {
        val result = runDiff(resource("original.txt"), resource("modified.txt"))
        assertEquals(1, result.exitCode)
        assertTrue(result.stdout.contains("< This is the second line."))
        assertTrue(result.stdout.contains("> This is the second line, modified."))
        assertTrue(result.stdout.contains("< This is the fourth line."))
        assertTrue(result.stdout.contains("> This is an added line."))
    }

    @Test fun `missing file exits 2`() {
        val result = runDiff(resource("original.txt"), "/nonexistent/file.txt")
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("No such file"))
    }

    @Test fun `wrong number of args exits 2`() {
        val result = runDiff("only-one-arg")
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("Usage"))
    }

    @Test fun `no args exits 2`() {
        val result = runDiff()
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("Usage"))
    }

    @Test fun `diff output has separator between removed and added`() {
        val result = runDiff(resource("original.txt"), resource("modified.txt"))
        assertTrue(result.stdout.contains("---"))
    }

    @Test fun `diff contains correct change groups`() {
        val original = resource("original.txt")
        val modified = resource("modified.txt")
        val result = runDiff(original, modified)

        val lines = result.stdout.lines().filter { it.isNotEmpty() }
        // Should have two change groups:
        // Group 1: second line modified
        // Group 2: fourth line replaced with added line
        val removedLines = lines.filter { it.startsWith("< ") }
        val addedLines = lines.filter { it.startsWith("> ") }
        assertEquals(2, removedLines.size)
        assertEquals(2, addedLines.size)
    }
}
