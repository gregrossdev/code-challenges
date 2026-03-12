package dev.gregross.challenges.calc

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: gig-calc '<expression>'")
        System.exit(1)
    }

    val expression = args.joinToString(" ")

    try {
        val tokenizer = Tokenizer()
        val shuntingYard = ShuntingYard()
        val evaluator = Evaluator()

        val tokens = tokenizer.tokenize(expression)
        val postfix = shuntingYard.toPostfix(tokens)
        val result = evaluator.evaluate(postfix)

        if (result == result.toLong().toDouble()) {
            println(result.toLong())
        } else {
            println(result)
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        System.exit(1)
    }
}
