package dev.gregross.challenges.json

data class Position(val line: Int, val column: Int) {
    override fun toString(): String = "line $line, column $column"
}

sealed interface Token {
    val position: Position

    data class LeftBrace(override val position: Position) : Token
    data class RightBrace(override val position: Position) : Token
    data class LeftBracket(override val position: Position) : Token
    data class RightBracket(override val position: Position) : Token
    data class Colon(override val position: Position) : Token
    data class Comma(override val position: Position) : Token
    data class StringToken(val value: String, override val position: Position) : Token
    data class NumberToken(val value: Double, override val position: Position) : Token
    data class BoolToken(val value: Boolean, override val position: Position) : Token
    data class NullToken(override val position: Position) : Token
    data class EOF(override val position: Position) : Token
}
