package dev.gregross.challenges.grep

class Matcher(
    pattern: String,
    ignoreCase: Boolean = false,
    private val invert: Boolean = false,
) {
    private val regex: Regex = if (ignoreCase) {
        Regex(pattern, RegexOption.IGNORE_CASE)
    } else {
        Regex(pattern)
    }

    fun matches(line: String): Boolean {
        val found = regex.containsMatchIn(line)
        return if (invert) !found else found
    }
}
