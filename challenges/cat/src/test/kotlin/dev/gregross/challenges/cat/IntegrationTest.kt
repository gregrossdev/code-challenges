package dev.gregross.challenges.cat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun resource(name: String): String {
        return javaClass.classLoader.getResource(name)!!.file
    }

    private fun runCat(vararg args: String, stdin: String = ""): ProcessResult {
        val pb = ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "dev.gregross.challenges.cat.MainKt", *args,
        )
        pb.redirectErrorStream(false)
        val process = pb.start()
        if (stdin.isNotEmpty()) {
            process.outputStream.write(stdin.toByteArray())
        }
        process.outputStream.close()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, stdout, stderr)
    }

    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    @Test fun `single file output`() {
        val result = runCat(resource("test.txt"))
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("First line"))
        assertTrue(result.stdout.contains("Third line"))
    }

    @Test fun `multiple files concatenated`() {
        val result = runCat(resource("test.txt"), resource("test2.txt"))
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("First line"))
        assertTrue(result.stdout.contains("Fifth line"))
    }

    @Test fun `stdin via dash`() {
        val result = runCat("-", stdin = "hello from stdin\n")
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("hello from stdin"))
    }

    @Test fun `no args reads stdin`() {
        val result = runCat(stdin = "piped input\n")
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("piped input"))
    }

    @Test fun `number all lines`() {
        val result = runCat("-n", resource("test.txt"))
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("     1\tFirst line"))
        assertTrue(result.stdout.contains("     3\tThird line"))
    }

    @Test fun `number non-blank lines`() {
        val result = runCat("-b", resource("blanks.txt"))
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("     1\tLine one"))
        assertTrue(result.stdout.contains("     2\tLine three"))
        assertTrue(result.stdout.contains("     3\tLine five"))
    }

    @Test fun `missing file prints error and continues`() {
        val result = runCat("/nonexistent/file.txt", resource("test.txt"))
        assertEquals(1, result.exitCode)
        assertTrue(result.stderr.contains("No such file or directory"))
        assertTrue(result.stdout.contains("First line"))
    }

    @Test fun `number persists across files`() {
        val result = runCat("-n", resource("test.txt"), resource("test2.txt"))
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("     1\tFirst line"))
        assertTrue(result.stdout.contains("     4\tFourth line"))
    }
}
