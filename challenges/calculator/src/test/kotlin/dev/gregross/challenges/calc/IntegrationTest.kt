package dev.gregross.challenges.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegrationTest {

    private val tokenizer = Tokenizer()
    private val shuntingYard = ShuntingYard()
    private val evaluator = Evaluator()

    private fun calc(expression: String): Double {
        val tokens = tokenizer.tokenize(expression)
        val postfix = shuntingYard.toPostfix(tokens)
        return evaluator.evaluate(postfix)
    }

    private fun format(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    }

    // Challenge Step 1 — Basic operations
    @Test fun `1 + 2 = 3`() = assertEquals("3", format(calc("1 + 2")))
    @Test fun `2 - 1 = 1`() = assertEquals("1", format(calc("2 - 1")))
    @Test fun `2 * 3 = 6`() = assertEquals("6", format(calc("2 * 3")))
    @Test fun `3 div 2 = 1_5`() = assertEquals("1.5", format(calc("3 / 2")))

    // Challenge Step 2 — Precedence and parentheses
    @Test fun `1 + 2 * 3 = 7`() = assertEquals("7", format(calc("1 + 2 * 3")))
    @Test fun `(1 + 1) * 5 = 10`() = assertEquals("10", format(calc("(1 + 1) * 5")))
    @Test fun `((2 + 3) * (4 - 1)) = 15`() = assertEquals("15", format(calc("((2 + 3) * (4 - 1))")))

    // Challenge Step 3 — Functions
    @Test fun `sin(0) = 0`() = assertEquals(0.0, calc("sin(0)"), 1e-10)
    @Test fun `cos(0) = 1`() = assertEquals(1.0, calc("cos(0)"), 1e-10)
    @Test fun `tan(0) = 0`() = assertEquals(0.0, calc("tan(0)"), 1e-10)

    // Edge cases
    @Test fun `negative number`() = assertEquals("-2", format(calc("-5 + 3")))
    @Test fun `decimal result`() = assertEquals("2.5", format(calc("5 / 2")))
    @Test fun `chained operations`() = assertEquals("10", format(calc("2 + 3 + 5")))
    @Test fun `mixed precedence`() = assertEquals("14", format(calc("2 + 3 * 4")))
    @Test fun `complex nested`() = assertEquals("21", format(calc("(2 + 5) * (1 + 2)")))

    // Error cases
    @Test fun `division by zero`() {
        assertFailsWith<ArithmeticException> { calc("1 / 0") }
    }

    @Test fun `mismatched parens`() {
        assertFailsWith<IllegalArgumentException> { calc("(1 + 2") }
    }

    @Test fun `invalid character`() {
        assertFailsWith<IllegalArgumentException> { calc("1 & 2") }
    }

    @Test fun `unknown function`() {
        assertFailsWith<IllegalArgumentException> { calc("foo(1)") }
    }
}
