package dev.gregross.challenges.shell

class InputParser {

    private enum class State { NORMAL, SINGLE_QUOTED, DOUBLE_QUOTED }

    fun parsePipeline(input: String): List<List<String>> {
        val segments = splitOnPipes(input)
        return segments.map { parseTokens(it) }.filter { it.isNotEmpty() }
    }

    fun parseTokens(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var state = State.NORMAL
        var inToken = false

        for (ch in input) {
            when (state) {
                State.NORMAL -> when (ch) {
                    '\'' -> { state = State.SINGLE_QUOTED; inToken = true }
                    '"' -> { state = State.DOUBLE_QUOTED; inToken = true }
                    ' ', '\t' -> {
                        if (inToken) {
                            tokens.add(current.toString())
                            current.clear()
                            inToken = false
                        }
                    }
                    else -> { current.append(ch); inToken = true }
                }
                State.SINGLE_QUOTED -> when (ch) {
                    '\'' -> state = State.NORMAL
                    else -> current.append(ch)
                }
                State.DOUBLE_QUOTED -> when (ch) {
                    '"' -> state = State.NORMAL
                    else -> current.append(ch)
                }
            }
        }

        if (inToken) {
            tokens.add(current.toString())
        }

        return tokens
    }

    private fun splitOnPipes(input: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var state = State.NORMAL

        for (ch in input) {
            when (state) {
                State.NORMAL -> when (ch) {
                    '\'' -> { state = State.SINGLE_QUOTED; current.append(ch) }
                    '"' -> { state = State.DOUBLE_QUOTED; current.append(ch) }
                    '|' -> {
                        segments.add(current.toString())
                        current.clear()
                    }
                    else -> current.append(ch)
                }
                State.SINGLE_QUOTED -> {
                    current.append(ch)
                    if (ch == '\'') state = State.NORMAL
                }
                State.DOUBLE_QUOTED -> {
                    current.append(ch)
                    if (ch == '"') state = State.NORMAL
                }
            }
        }

        if (current.isNotEmpty() || segments.isNotEmpty()) {
            segments.add(current.toString())
        }

        return segments
    }
}
