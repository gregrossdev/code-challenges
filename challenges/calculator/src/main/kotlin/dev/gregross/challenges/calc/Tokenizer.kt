package dev.gregross.challenges.calc

class Tokenizer {

    fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < input.length) {
            val c = input[i]

            when {
                c.isWhitespace() -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    val numStr = input.substring(start, i)
                    val value = numStr.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid number: $numStr")
                    tokens.add(Token.Number(value))
                }

                c == '-' && isUnaryMinus(tokens) -> {
                    i++
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    if (i == start) throw IllegalArgumentException("Expected number after unary minus")
                    val numStr = input.substring(start, i)
                    val value = numStr.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid number: -$numStr")
                    tokens.add(Token.Number(-value))
                }

                c in "+-*/" -> {
                    tokens.add(Token.operatorFor(c))
                    i++
                }

                c == '(' -> {
                    tokens.add(Token.LeftParen)
                    i++
                }

                c == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }

                c.isLetter() -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    val name = input.substring(start, i)
                    tokens.add(Token.Function(name))
                }

                else -> throw IllegalArgumentException("Unexpected character: '$c'")
            }
        }

        return tokens
    }

    private fun isUnaryMinus(tokens: List<Token>): Boolean {
        if (tokens.isEmpty()) return true
        return when (tokens.last()) {
            is Token.Operator, Token.LeftParen -> true
            else -> false
        }
    }
}
