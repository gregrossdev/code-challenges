package dev.gregross.challenges.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    private fun runShell(vararg commands: String): ProcessResult {
        val input = commands.joinToString("\n") + "\nexit\n"
        val pb = ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "dev.gregross.challenges.shell.ShellTestMainKt",
        )
        pb.redirectErrorStream(false)
        val process = pb.start()
        process.outputStream.write(input.toByteArray())
        process.outputStream.flush()
        process.outputStream.close()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, stdout, stderr)
    }

    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    @Test fun `echo outputs text`() {
        val result = runShell("echo hello")
        assertTrue(result.stdout.contains("hello"), "stdout: ${result.stdout}")
    }

    @Test fun `echo with quotes preserves spaces`() {
        val result = runShell("echo 'hello world'")
        assertTrue(result.stdout.contains("hello world"), "stdout: ${result.stdout}")
    }

    @Test fun `ls runs without error`() {
        val result = runShell("ls")
        assertEquals("", result.stderr.replace(Regex("gig-sh> "), "").trim().let {
            if (it.contains("command not found")) it else ""
        })
    }

    @Test fun `pipe works`() {
        val result = runShell("echo hello | cat")
        assertTrue(result.stdout.contains("hello"), "stdout: ${result.stdout}")
    }

    @Test fun `unknown command shows error`() {
        val result = runShell("nonexistent_command_xyz")
        assertTrue(result.stderr.contains("command not found"), "stderr: ${result.stderr}")
    }

    @Test fun `pwd shows directory`() {
        val result = runShell("pwd")
        assertTrue(result.stdout.contains("/"), "stdout: ${result.stdout}")
    }

    @Test fun `cd and pwd`() {
        val result = runShell("cd /tmp", "pwd")
        assertTrue(result.stdout.contains("/tmp") || result.stdout.contains("/private/tmp"),
            "stdout: ${result.stdout}")
    }

    @Test fun `exit terminates shell`() {
        val result = runShell("exit")
        assertEquals(0, result.exitCode)
    }
}
