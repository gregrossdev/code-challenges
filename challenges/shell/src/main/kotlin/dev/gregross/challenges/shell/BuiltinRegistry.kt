package dev.gregross.challenges.shell

import java.io.File
import java.io.PrintStream

class BuiltinRegistry(
    private val shell: Shell,
    private val history: History,
    private val output: PrintStream = System.out,
    private val error: PrintStream = System.err,
) {

    private val builtins = setOf("exit", "cd", "pwd", "history")

    fun isBuiltin(command: String): Boolean = command in builtins

    fun execute(args: List<String>): Int {
        return when (args[0]) {
            "exit" -> { shell.running = false; 0 }
            "cd" -> cd(args)
            "pwd" -> { output.println(shell.workingDir.absolutePath); 0 }
            "history" -> { printHistory(); 0 }
            else -> { error.println("gig-sh: ${args[0]}: unknown builtin"); 1 }
        }
    }

    private fun cd(args: List<String>): Int {
        val path = when {
            args.size < 2 -> System.getProperty("user.home")
            args[1] == "~" -> System.getProperty("user.home")
            args[1].startsWith("~/") -> System.getProperty("user.home") + args[1].substring(1)
            else -> args[1]
        }

        val target = File(path).let {
            if (it.isAbsolute) it else File(shell.workingDir, path)
        }

        val canonical = target.canonicalFile
        if (!canonical.isDirectory) {
            error.println("gig-sh: cd: ${args.getOrElse(1) { "" }}: No such directory")
            return 1
        }

        shell.workingDir = canonical
        return 0
    }

    private fun printHistory() {
        history.entries().forEachIndexed { index, entry ->
            output.println("  ${index + 1}  $entry")
        }
    }
}
