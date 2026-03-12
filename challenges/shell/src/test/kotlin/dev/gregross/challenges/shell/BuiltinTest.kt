package dev.gregross.challenges.shell

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltinTest {

    private fun createShell(): Triple<Shell, ByteArrayOutputStream, ByteArrayOutputStream> {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val shell = Shell(
            input = "".byteInputStream().bufferedReader(),
            output = PrintStream(stdout),
            error = PrintStream(stderr),
        )
        return Triple(shell, stdout, stderr)
    }

    @Test fun `exit sets running to false`() {
        val (shell, _, _) = createShell()
        assertTrue(shell.running)
        shell.builtins.execute(listOf("exit"))
        assertFalse(shell.running)
    }

    @Test fun `pwd prints working directory`() {
        val (shell, stdout, _) = createShell()
        shell.builtins.execute(listOf("pwd"))
        assertEquals(shell.workingDir.absolutePath, stdout.toString().trim())
    }

    @Test fun `cd changes directory`() {
        val (shell, _, _) = createShell()
        val original = shell.workingDir
        shell.builtins.execute(listOf("cd", "/tmp"))
        assertEquals(File("/tmp").canonicalFile, shell.workingDir)
        shell.workingDir = original
    }

    @Test fun `cd with no args goes to home`() {
        val (shell, _, _) = createShell()
        shell.builtins.execute(listOf("cd"))
        assertEquals(File(System.getProperty("user.home")).canonicalFile, shell.workingDir)
    }

    @Test fun `cd tilde goes to home`() {
        val (shell, _, _) = createShell()
        shell.builtins.execute(listOf("cd", "~"))
        assertEquals(File(System.getProperty("user.home")).canonicalFile, shell.workingDir)
    }

    @Test fun `cd to nonexistent directory returns error`() {
        val (shell, _, stderr) = createShell()
        val result = shell.builtins.execute(listOf("cd", "/nonexistent_dir_xyz"))
        assertEquals(1, result)
        assertTrue(stderr.toString().contains("No such directory"))
    }

    @Test fun `history tracks commands`() {
        val (shell, stdout, _) = createShell()
        shell.history.add("echo hello")
        shell.history.add("ls -la")
        shell.builtins.execute(listOf("history"))
        val output = stdout.toString()
        assertTrue(output.contains("1  echo hello"))
        assertTrue(output.contains("2  ls -la"))
    }

    @Test fun `history load and save roundtrip`() {
        val history = History()
        history.add("cmd1")
        history.add("cmd2")

        val tmpFile = File.createTempFile("gig-sh-test", ".history")
        tmpFile.deleteOnExit()
        history.save(tmpFile)

        val loaded = History()
        loaded.load(tmpFile)
        assertEquals(listOf("cmd1", "cmd2"), loaded.entries())
    }

    @Test fun `isBuiltin recognizes builtins`() {
        val (shell, _, _) = createShell()
        assertTrue(shell.builtins.isBuiltin("exit"))
        assertTrue(shell.builtins.isBuiltin("cd"))
        assertTrue(shell.builtins.isBuiltin("pwd"))
        assertTrue(shell.builtins.isBuiltin("history"))
        assertFalse(shell.builtins.isBuiltin("ls"))
        assertFalse(shell.builtins.isBuiltin("echo"))
    }
}
