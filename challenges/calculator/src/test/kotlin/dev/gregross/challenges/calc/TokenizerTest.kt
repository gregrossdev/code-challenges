package dev.gregross.challenges.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TokenizerTest {

    private val tokenizer = Tokenizer()

    @Test fun `integer`() {
        assertEquals(listOf(Token.Number(42.0)), tokenizer.tokenize("42"))
    }

    @Test fun `decimal number`() {
        assertEquals(listOf(Token.Number(3.14)), tokenizer.tokenize("3.14"))
    }

    @Test fun `simple addition`() {
        val result = tokenizer.tokenize("1 + 2")
        assertEquals(listOf(Token.Number(1.0), Token.PLUS, Token.Number(2.0)), result)
    }

    @Test fun `all four operators`() {
        val result = tokenizer.tokenize("1 + 2 - 3 * 4 / 5")
        assertEquals(
            listOf(
                Token.Number(1.0), Token.PLUS,
                Token.Number(2.0), Token.MINUS,
                Token.Number(3.0), Token.TIMES,
                Token.Number(4.0), Token.DIVIDE,
                Token.Number(5.0),
            ),
            result,
        )
    }

    @Test fun `parentheses`() {
        val result = tokenizer.tokenize("(1 + 2)")
        assertEquals(
            listOf(Token.LeftParen, Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.RightParen),
            result,
        )
    }

    @Test fun `function name`() {
        val result = tokenizer.tokenize("sin(0)")
        assertEquals(
            listOf(Token.Function("sin"), Token.LeftParen, Token.Number(0.0), Token.RightParen),
            result,
        )
    }

    @Test fun `negative number at start`() {
        val result = tokenizer.tokenize("-5 + 3")
        assertEquals(listOf(Token.Number(-5.0), Token.PLUS, Token.Number(3.0)), result)
    }

    @Test fun `negative number after operator`() {
        val result = tokenizer.tokenize("3 * -2")
        assertEquals(listOf(Token.Number(3.0), Token.TIMES, Token.Number(-2.0)), result)
    }

    @Test fun `negative number after left paren`() {
        val result = tokenizer.tokenize("(-5 + 3)")
        assertEquals(
            listOf(Token.LeftParen, Token.Number(-5.0), Token.PLUS, Token.Number(3.0), Token.RightParen),
            result,
        )
    }

    @Test fun `whitespace is ignored`() {
        val result = tokenizer.tokenize("  1   +   2  ")
        assertEquals(listOf(Token.Number(1.0), Token.PLUS, Token.Number(2.0)), result)
    }

    @Test fun `no whitespace needed`() {
        val result = tokenizer.tokenize("1+2*3")
        assertEquals(
            listOf(Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.TIMES, Token.Number(3.0)),
            result,
        )
    }

    @Test fun `invalid character throws`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizer.tokenize("1 & 2")
        }
    }

    @Test fun `empty input`() {
        assertEquals(emptyList(), tokenizer.tokenize(""))
    }

    @Test fun `multiple functions`() {
        val result = tokenizer.tokenize("sin(0) + cos(0)")
        assertEquals(
            listOf(
                Token.Function("sin"), Token.LeftParen, Token.Number(0.0), Token.RightParen,
                Token.PLUS,
                Token.Function("cos"), Token.LeftParen, Token.Number(0.0), Token.RightParen,
            ),
            result,
        )
    }
}
