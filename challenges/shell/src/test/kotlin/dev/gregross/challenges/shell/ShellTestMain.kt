package dev.gregross.challenges.shell

/**
 * Test-only entry point that uses BufferedReader instead of JLine.
 * JLine requires a real terminal, which isn't available in subprocess tests.
 */
fun main() {
    val shell = Shell()
    shell.run()
}
