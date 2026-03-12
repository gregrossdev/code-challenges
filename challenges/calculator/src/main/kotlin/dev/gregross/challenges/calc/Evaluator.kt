package dev.gregross.challenges.calc

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Evaluator {

    fun evaluate(postfix: List<Token>): Double {
        val stack = ArrayDeque<Double>()

        for (token in postfix) {
            when (token) {
                is Token.Number -> stack.addLast(token.value)

                is Token.Operator -> {
                    if (stack.size < 2) {
                        throw IllegalArgumentException("Insufficient operands for operator '${token.symbol}'")
                    }
                    val right = stack.removeLast()
                    val left = stack.removeLast()
                    val result = when (token.symbol) {
                        '+' -> left + right
                        '-' -> left - right
                        '*' -> left * right
                        '/' -> {
                            if (right == 0.0) throw ArithmeticException("Division by zero")
                            left / right
                        }
                        else -> throw IllegalArgumentException("Unknown operator: '${token.symbol}'")
                    }
                    stack.addLast(result)
                }

                is Token.Function -> {
                    if (stack.isEmpty()) {
                        throw IllegalArgumentException("Insufficient arguments for function '${token.name}'")
                    }
                    val arg = stack.removeLast()
                    val result = when (token.name) {
                        "sin" -> sin(arg)
                        "cos" -> cos(arg)
                        "tan" -> tan(arg)
                        else -> throw IllegalArgumentException("Unknown function: '${token.name}'")
                    }
                    stack.addLast(result)
                }

                is Token.LeftParen, is Token.RightParen -> {
                    throw IllegalArgumentException("Parentheses should not appear in postfix expression")
                }
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Invalid expression: expected single result, got ${stack.size} values")
        }

        return stack.removeLast()
    }
}
