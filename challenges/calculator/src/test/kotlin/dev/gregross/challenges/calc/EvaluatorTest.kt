package dev.gregross.challenges.calc

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluatorTest {

    private val tokenizer = Tokenizer()
    private val shuntingYard = ShuntingYard()
    private val evaluator = Evaluator()

    private fun eval(expression: String): Double {
        val tokens = tokenizer.tokenize(expression)
        val postfix = shuntingYard.toPostfix(tokens)
        return evaluator.evaluate(postfix)
    }

    @Test fun `basic addition`() {
        assertEquals(3.0, eval("1 + 2"))
    }

    @Test fun `basic subtraction`() {
        assertEquals(1.0, eval("2 - 1"))
    }

    @Test fun `basic multiplication`() {
        assertEquals(6.0, eval("2 * 3"))
    }

    @Test fun `basic division`() {
        assertEquals(1.5, eval("3 / 2"))
    }

    @Test fun `operator precedence`() {
        assertEquals(7.0, eval("1 + 2 * 3"))
    }

    @Test fun `parentheses override precedence`() {
        assertEquals(10.0, eval("(1 + 1) * 5"))
    }

    @Test fun `nested parentheses`() {
        assertEquals(15.0, eval("((2 + 3) * (4 - 1))"))
    }

    @Test fun `left associativity subtraction`() {
        assertEquals(-4.0, eval("1 - 2 - 3"))
    }

    @Test fun `left associativity division`() {
        assertEquals(2.0, eval("12 / 3 / 2"))
    }

    @Test fun `sin of zero`() {
        assertEquals(0.0, eval("sin(0)"), 1e-10)
    }

    @Test fun `cos of zero`() {
        assertEquals(1.0, eval("cos(0)"), 1e-10)
    }

    @Test fun `tan of zero`() {
        assertEquals(0.0, eval("tan(0)"), 1e-10)
    }

    @Test fun `sin of pi over 2`() {
        assertEquals(1.0, eval("sin(${PI / 2})"), 1e-10)
    }

    @Test fun `complex expression`() {
        // 2 + 3 * 4 - 1 = 2 + 12 - 1 = 13
        assertEquals(13.0, eval("2 + 3 * 4 - 1"))
    }

    @Test fun `negative number`() {
        assertEquals(-2.0, eval("-5 + 3"))
    }

    @Test fun `division by zero throws`() {
        assertFailsWith<ArithmeticException> {
            eval("1 / 0")
        }
    }

    @Test fun `decimal arithmetic`() {
        assertEquals(3.5, eval("1.5 + 2.0"))
    }

    @Test fun `function in expression`() {
        // sin(0) + 1 = 0 + 1 = 1
        assertEquals(1.0, eval("sin(0) + 1"), 1e-10)
    }

    @Test fun `single number`() {
        assertEquals(42.0, eval("42"))
    }
}
