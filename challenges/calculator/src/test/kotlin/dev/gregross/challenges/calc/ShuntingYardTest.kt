package dev.gregross.challenges.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShuntingYardTest {

    private val sy = ShuntingYard()

    @Test fun `simple addition`() {
        // 1 + 2 -> 1 2 +
        val tokens = listOf(Token.Number(1.0), Token.PLUS, Token.Number(2.0))
        val result = sy.toPostfix(tokens)
        assertEquals(listOf(Token.Number(1.0), Token.Number(2.0), Token.PLUS), result)
    }

    @Test fun `precedence - multiply before add`() {
        // 1 + 2 * 3 -> 1 2 3 * +
        val tokens = listOf(
            Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.TIMES, Token.Number(3.0),
        )
        val result = sy.toPostfix(tokens)
        assertEquals(
            listOf(Token.Number(1.0), Token.Number(2.0), Token.Number(3.0), Token.TIMES, Token.PLUS),
            result,
        )
    }

    @Test fun `parentheses override precedence`() {
        // (1 + 2) * 3 -> 1 2 + 3 *
        val tokens = listOf(
            Token.LeftParen, Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.RightParen,
            Token.TIMES, Token.Number(3.0),
        )
        val result = sy.toPostfix(tokens)
        assertEquals(
            listOf(Token.Number(1.0), Token.Number(2.0), Token.PLUS, Token.Number(3.0), Token.TIMES),
            result,
        )
    }

    @Test fun `nested parentheses`() {
        // ((1 + 2) * (3 - 1)) -> 1 2 + 3 1 - *
        val tokens = listOf(
            Token.LeftParen, Token.LeftParen, Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.RightParen,
            Token.TIMES,
            Token.LeftParen, Token.Number(3.0), Token.MINUS, Token.Number(1.0), Token.RightParen, Token.RightParen,
        )
        val result = sy.toPostfix(tokens)
        assertEquals(
            listOf(
                Token.Number(1.0), Token.Number(2.0), Token.PLUS,
                Token.Number(3.0), Token.Number(1.0), Token.MINUS,
                Token.TIMES,
            ),
            result,
        )
    }

    @Test fun `function`() {
        // sin(0) -> 0 sin
        val tokens = listOf(Token.Function("sin"), Token.LeftParen, Token.Number(0.0), Token.RightParen)
        val result = sy.toPostfix(tokens)
        assertEquals(listOf(Token.Number(0.0), Token.Function("sin")), result)
    }

    @Test fun `function with expression argument`() {
        // sin(1 + 2) -> 1 2 + sin
        val tokens = listOf(
            Token.Function("sin"), Token.LeftParen,
            Token.Number(1.0), Token.PLUS, Token.Number(2.0),
            Token.RightParen,
        )
        val result = sy.toPostfix(tokens)
        assertEquals(
            listOf(Token.Number(1.0), Token.Number(2.0), Token.PLUS, Token.Function("sin")),
            result,
        )
    }

    @Test fun `left associativity`() {
        // 1 - 2 - 3 -> 1 2 - 3 - (not 1 2 3 - -)
        val tokens = listOf(
            Token.Number(1.0), Token.MINUS, Token.Number(2.0), Token.MINUS, Token.Number(3.0),
        )
        val result = sy.toPostfix(tokens)
        assertEquals(
            listOf(Token.Number(1.0), Token.Number(2.0), Token.MINUS, Token.Number(3.0), Token.MINUS),
            result,
        )
    }

    @Test fun `mismatched right paren throws`() {
        val tokens = listOf(Token.Number(1.0), Token.PLUS, Token.Number(2.0), Token.RightParen)
        assertFailsWith<IllegalArgumentException> {
            sy.toPostfix(tokens)
        }
    }

    @Test fun `mismatched left paren throws`() {
        val tokens = listOf(Token.LeftParen, Token.Number(1.0), Token.PLUS, Token.Number(2.0))
        assertFailsWith<IllegalArgumentException> {
            sy.toPostfix(tokens)
        }
    }
}
