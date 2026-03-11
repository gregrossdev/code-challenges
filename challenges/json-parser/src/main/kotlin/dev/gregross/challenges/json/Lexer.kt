package dev.gregross.challenges.json

class Lexer(private val input: String) {
    private var pos = 0
    private var line = 1
    private var column = 1

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < input.length) {
            skipWhitespace()
            if (pos >= input.length) break
            tokens.add(nextToken())
        }
        tokens.add(Token.EOF(position()))
        return tokens
    }

    private fun position(): Position = Position(line, column)

    private fun nextToken(): Token {
        val pos = position()
        return when (val c = input[this.pos]) {
            '{' -> advance().let { Token.LeftBrace(pos) }
            '}' -> advance().let { Token.RightBrace(pos) }
            '[' -> advance().let { Token.LeftBracket(pos) }
            ']' -> advance().let { Token.RightBracket(pos) }
            ':' -> advance().let { Token.Colon(pos) }
            ',' -> advance().let { Token.Comma(pos) }
            '"' -> readString(pos)
            't', 'f' -> readBool(pos)
            'n' -> readNull(pos)
            '-', in '0'..'9' -> readNumber(pos)
            else -> throw JsonParseException("Unexpected character '$c'", pos)
        }
    }

    private fun advance(): Char {
        val c = input[pos]
        pos++
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    private fun peek(): Char? = if (pos < input.length) input[pos] else null

    private fun skipWhitespace() {
        while (pos < input.length && input[pos] in " \t\r\n") {
            advance()
        }
    }

    private fun readString(startPos: Position): Token.StringToken {
        advance() // opening quote
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = advance()
            when (c) {
                '"' -> return Token.StringToken(sb.toString(), startPos)
                '\\' -> {
                    if (pos >= input.length) throw JsonParseException("Unexpected end of string", position())
                    val escaped = advance()
                    when (escaped) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = readHex(4)
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> throw JsonParseException("Invalid escape sequence '\\$escaped'", position())
                    }
                }
                else -> {
                    if (c.code < 0x20) throw JsonParseException("Invalid control character in string", position())
                    sb.append(c)
                }
            }
        }
        throw JsonParseException("Unterminated string", startPos)
    }

    private fun readHex(count: Int): String {
        val sb = StringBuilder()
        repeat(count) {
            if (pos >= input.length) throw JsonParseException("Unexpected end of unicode escape", position())
            val c = advance()
            if (c !in '0'..'9' && c !in 'a'..'f' && c !in 'A'..'F') {
                throw JsonParseException("Invalid unicode escape character '$c'", position())
            }
            sb.append(c)
        }
        return sb.toString()
    }

    private fun readNumber(startPos: Position): Token.NumberToken {
        val sb = StringBuilder()

        // optional negative
        if (peek() == '-') sb.append(advance())

        // integer part
        if (peek() == '0') {
            sb.append(advance())
            // after leading 0, next must not be a digit
            if (peek() in '0'..'9') throw JsonParseException("Leading zeros not allowed", startPos)
        } else if (peek() in '1'..'9') {
            sb.append(advance())
            while (peek() in '0'..'9') sb.append(advance())
        } else {
            throw JsonParseException("Invalid number", startPos)
        }

        // fraction
        if (peek() == '.') {
            sb.append(advance())
            if (peek() !in '0'..'9') throw JsonParseException("Expected digit after decimal point", position())
            while (peek() in '0'..'9') sb.append(advance())
        }

        // exponent
        if (peek() == 'e' || peek() == 'E') {
            sb.append(advance())
            if (peek() == '+' || peek() == '-') sb.append(advance())
            if (peek() !in '0'..'9') throw JsonParseException("Expected digit in exponent", position())
            while (peek() in '0'..'9') sb.append(advance())
        }

        return Token.NumberToken(sb.toString().toDouble(), startPos)
    }

    private fun readBool(startPos: Position): Token.BoolToken {
        return if (tryConsume("true")) {
            Token.BoolToken(true, startPos)
        } else if (tryConsume("false")) {
            Token.BoolToken(false, startPos)
        } else {
            throw JsonParseException("Unexpected token", startPos)
        }
    }

    private fun readNull(startPos: Position): Token.NullToken {
        if (tryConsume("null")) {
            return Token.NullToken(startPos)
        }
        throw JsonParseException("Unexpected token", startPos)
    }

    private fun tryConsume(expected: String): Boolean {
        if (pos + expected.length > input.length) return false
        for (i in expected.indices) {
            if (input[pos + i] != expected[i]) return false
        }
        repeat(expected.length) { advance() }
        return true
    }
}
