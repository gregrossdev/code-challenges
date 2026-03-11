package dev.gregross.challenges.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LexerTest {

    @Test
    fun `tokenizes empty object`() {
        val tokens = Lexer("{}").tokenize()
        assertEquals(3, tokens.size)
        assertIs<Token.LeftBrace>(tokens[0])
        assertIs<Token.RightBrace>(tokens[1])
        assertIs<Token.EOF>(tokens[2])
    }

    @Test
    fun `tokenizes string key value`() {
        val tokens = Lexer("""{"key": "value"}""").tokenize()
        assertEquals(6, tokens.size)
        assertIs<Token.LeftBrace>(tokens[0])
        val key = tokens[1] as Token.StringToken
        assertEquals("key", key.value)
        assertIs<Token.Colon>(tokens[2])
        val value = tokens[3] as Token.StringToken
        assertEquals("value", value.value)
        assertIs<Token.RightBrace>(tokens[4])
    }

    @Test
    fun `tokenizes all value types`() {
        val tokens = Lexer("""{"a": true, "b": false, "c": null, "d": 42, "e": "str"}""").tokenize()
        val values = tokens.filter { it is Token.BoolToken || it is Token.NullToken || it is Token.NumberToken || it is Token.StringToken }
        // 5 keys + 5 values = 10 string/value tokens
        assertEquals(10, values.size)
    }

    @Test
    fun `tokenizes integer number`() {
        val tokens = Lexer("42").tokenize()
        val num = tokens[0] as Token.NumberToken
        assertEquals(42.0, num.value)
    }

    @Test
    fun `tokenizes negative number`() {
        val tokens = Lexer("-17").tokenize()
        val num = tokens[0] as Token.NumberToken
        assertEquals(-17.0, num.value)
    }

    @Test
    fun `tokenizes decimal number`() {
        val tokens = Lexer("3.14").tokenize()
        val num = tokens[0] as Token.NumberToken
        assertEquals(3.14, num.value)
    }

    @Test
    fun `tokenizes exponent number`() {
        val tokens = Lexer("1e10").tokenize()
        val num = tokens[0] as Token.NumberToken
        assertEquals(1e10, num.value)
    }

    @Test
    fun `tokenizes negative exponent`() {
        val tokens = Lexer("2.5E-3").tokenize()
        val num = tokens[0] as Token.NumberToken
        assertEquals(2.5e-3, num.value)
    }

    @Test
    fun `rejects leading zeros`() {
        val ex = assertFailsWith<JsonParseException> { Lexer("01").tokenize() }
        assertEquals(1, ex.position?.line)
    }

    @Test
    fun `tokenizes string with escape sequences`() {
        val tokens = Lexer(""""hello\nworld\t\\\"end"""").tokenize()
        val str = tokens[0] as Token.StringToken
        assertEquals("hello\nworld\t\\\"end", str.value)
    }

    @Test
    fun `tokenizes unicode escape`() {
        val tokens = Lexer("\"\\u0041\"").tokenize()
        val str = tokens[0] as Token.StringToken
        assertEquals("A", str.value)
    }

    @Test
    fun `rejects unterminated string`() {
        assertFailsWith<JsonParseException> { Lexer(""""hello""").tokenize() }
    }

    @Test
    fun `rejects invalid escape`() {
        assertFailsWith<JsonParseException> { Lexer(""""hello\x"""").tokenize() }
    }

    @Test
    fun `rejects unexpected character`() {
        val ex = assertFailsWith<JsonParseException> { Lexer("@").tokenize() }
        assertEquals(1, ex.position?.line)
        assertEquals(1, ex.position?.column)
    }

    @Test
    fun `tracks line and column`() {
        val tokens = Lexer("{\n  \"key\": 1\n}").tokenize()
        val rbrace = tokens.last { it is Token.RightBrace } as Token.RightBrace
        assertEquals(3, rbrace.position.line)
        assertEquals(1, rbrace.position.column)
    }

    @Test
    fun `tokenizes empty array`() {
        val tokens = Lexer("[]").tokenize()
        assertIs<Token.LeftBracket>(tokens[0])
        assertIs<Token.RightBracket>(tokens[1])
    }

    @Test
    fun `tokenizes true false null`() {
        val tokens = Lexer("[true, false, null]").tokenize()
        assertIs<Token.BoolToken>(tokens[1])
        assertEquals(true, (tokens[1] as Token.BoolToken).value)
        assertIs<Token.BoolToken>(tokens[3])
        assertEquals(false, (tokens[3] as Token.BoolToken).value)
        assertIs<Token.NullToken>(tokens[5])
    }
}
