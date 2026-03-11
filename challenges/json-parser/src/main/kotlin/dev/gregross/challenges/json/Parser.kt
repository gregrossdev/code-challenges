package dev.gregross.challenges.json

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    fun parse(): JsonValue {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens[0] is Token.EOF)) {
            throw JsonParseException("Empty input is not valid JSON", Position(1, 1))
        }
        val value = parseValue()
        if (current() !is Token.EOF) {
            throw JsonParseException("Unexpected token after top-level value", current().position)
        }
        return value
    }

    private fun current(): Token = tokens[pos]

    private fun advance(): Token {
        val token = tokens[pos]
        if (pos < tokens.size - 1) pos++
        return token
    }

    private fun expect(check: (Token) -> Boolean, description: String): Token {
        val token = current()
        if (!check(token)) {
            throw JsonParseException("Expected $description", token.position)
        }
        return advance()
    }

    private fun parseValue(): JsonValue {
        return when (val token = current()) {
            is Token.LeftBrace -> parseObject()
            is Token.LeftBracket -> parseArray()
            is Token.StringToken -> { advance(); JsonValue.JsonString(token.value) }
            is Token.NumberToken -> { advance(); JsonValue.JsonNumber(token.value) }
            is Token.BoolToken -> { advance(); JsonValue.JsonBool(token.value) }
            is Token.NullToken -> { advance(); JsonValue.JsonNull }
            is Token.EOF -> throw JsonParseException("Unexpected end of input", token.position)
            else -> throw JsonParseException("Unexpected token", token.position)
        }
    }

    private fun parseObject(): JsonValue.JsonObject {
        expect({ it is Token.LeftBrace }, "'{'")
        val members = LinkedHashMap<String, JsonValue>()

        if (current() is Token.RightBrace) {
            advance()
            return JsonValue.JsonObject(members)
        }

        parseMember(members)
        while (current() is Token.Comma) {
            advance() // consume comma
            if (current() is Token.RightBrace) {
                throw JsonParseException("Trailing comma in object", current().position)
            }
            parseMember(members)
        }

        expect({ it is Token.RightBrace }, "'}'")
        return JsonValue.JsonObject(members)
    }

    private fun parseMember(members: LinkedHashMap<String, JsonValue>) {
        val keyToken = expect({ it is Token.StringToken }, "string key") as Token.StringToken
        expect({ it is Token.Colon }, "':'")
        val value = parseValue()
        members[keyToken.value] = value
    }

    private fun parseArray(): JsonValue.JsonArray {
        expect({ it is Token.LeftBracket }, "'['")
        val elements = mutableListOf<JsonValue>()

        if (current() is Token.RightBracket) {
            advance()
            return JsonValue.JsonArray(elements)
        }

        elements.add(parseValue())
        while (current() is Token.Comma) {
            advance() // consume comma
            if (current() is Token.RightBracket) {
                throw JsonParseException("Trailing comma in array", current().position)
            }
            elements.add(parseValue())
        }

        expect({ it is Token.RightBracket }, "']'")
        return JsonValue.JsonArray(elements)
    }
}
