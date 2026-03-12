package dev.gregross.challenges.calc

sealed interface Token {
    data class Number(val value: Double) : Token
    data class Operator(val symbol: Char, val precedence: Int, val leftAssociative: Boolean) : Token
    data object LeftParen : Token
    data object RightParen : Token
    data class Function(val name: String) : Token

    companion object {
        val PLUS = Operator('+', 1, true)
        val MINUS = Operator('-', 1, true)
        val TIMES = Operator('*', 2, true)
        val DIVIDE = Operator('/', 2, true)

        fun operatorFor(symbol: Char): Operator = when (symbol) {
            '+' -> PLUS
            '-' -> MINUS
            '*' -> TIMES
            '/' -> DIVIDE
            else -> throw IllegalArgumentException("Unknown operator: $symbol")
        }
    }
}
