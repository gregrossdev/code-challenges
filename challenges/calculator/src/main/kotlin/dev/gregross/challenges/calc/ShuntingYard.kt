package dev.gregross.challenges.calc

class ShuntingYard {

    fun toPostfix(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)

                is Token.Function -> stack.addLast(token)

                is Token.Operator -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.Operator &&
                            (top.precedence > token.precedence ||
                                (top.precedence == token.precedence && token.leftAssociative))
                        ) {
                            output.add(stack.removeLast())
                        } else {
                            break
                        }
                    }
                    stack.addLast(token)
                }

                is Token.LeftParen -> stack.addLast(token)

                is Token.RightParen -> {
                    while (stack.isNotEmpty() && stack.last() !is Token.LeftParen) {
                        output.add(stack.removeLast())
                    }
                    if (stack.isEmpty()) {
                        throw IllegalArgumentException("Mismatched parentheses: missing '('")
                    }
                    stack.removeLast() // Remove the left paren
                    // If a function is on top of the stack, pop it to output
                    if (stack.isNotEmpty() && stack.last() is Token.Function) {
                        output.add(stack.removeLast())
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LeftParen) {
                throw IllegalArgumentException("Mismatched parentheses: missing ')'")
            }
            output.add(top)
        }

        return output
    }
}
