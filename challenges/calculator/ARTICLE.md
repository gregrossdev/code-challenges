# Building My Own Calculator: The Shunting-Yard Algorithm in Kotlin

> Parsing and evaluating mathematical expressions using Dijkstra's shunting-yard algorithm — tokenization, infix-to-postfix conversion, and stack-based evaluation with operator precedence, parentheses, and trigonometric functions.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Calculator](https://codingchallenges.fyi/challenges/challenge-calculator)

Every developer uses a calculator, but few have thought about what happens between typing `(1 + 2) * 3` and seeing `9`. The expression looks simple to a human, but a computer needs to figure out what to do first — the addition inside the parentheses, then the multiplication. It needs to handle operator precedence (`*` before `+`), grouping (parentheses), and the actual arithmetic.

This challenge asks you to build a command-line calculator that correctly evaluates expressions like `1 + 2 * 3` (returning `7`, not `9`), handles parentheses, and supports trigonometric functions. The recommended approach: Dijkstra's shunting-yard algorithm.

## Approach

A three-stage pipeline with clean separation between concerns:

1. **Tokenizer** — converts the input string into a list of typed tokens
2. **Shunting-yard** — converts infix tokens to postfix (Reverse Polish Notation)
3. **Evaluator** — evaluates the postfix expression using a stack

Each stage has a single responsibility and is independently testable. The token model uses Kotlin's sealed interface for type safety.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Algorithm | Shunting-yard (Dijkstra's) | Challenge recommends it; clean infix-to-postfix conversion |
| Token model | Sealed interface with 5 subtypes | Type-safe, exhaustive when-expressions |
| Precedence encoding | Baked into Operator token | Simplifies shunting-yard logic |
| Trig functions | sin, cos, tan (radians) | Challenge Step 3; radians is standard |
| Output format | Integers without `.0`, decimals as-is | Clean output: `3` not `3.0` |

## Stage 1: Tokenization

The tokenizer walks the input string character by character, producing typed tokens:

```kotlin
sealed interface Token {
    data class Number(val value: Double) : Token
    data class Operator(val symbol: Char, val precedence: Int, val leftAssociative: Boolean) : Token
    data object LeftParen : Token
    data object RightParen : Token
    data class Function(val name: String) : Token
}
```

The interesting part is encoding operator precedence directly in the token. `+` and `-` get precedence 1, `*` and `/` get precedence 2. This means the shunting-yard algorithm doesn't need a separate precedence table — it just reads the token.

Unary minus (negative numbers) needs special handling. `-5 + 3` starts with a minus that isn't subtraction — it's negation. The rule: a minus is unary if it appears at the start of input, after an operator, or after a left parenthesis. In these cases, the tokenizer reads it as part of the number rather than as a separate operator.

## Stage 2: The Shunting-Yard Algorithm

This is the heart of the calculator. Dijkstra's algorithm converts infix notation (what humans write) to postfix notation (what's easy to evaluate). It uses two data structures: an **output queue** and an **operator stack**.

The algorithm processes tokens left to right:

| Token type | Action |
|-----------|--------|
| Number | Push directly to output queue |
| Operator | Pop higher-precedence operators from stack to output, then push this operator |
| Left paren `(` | Push to operator stack |
| Right paren `)` | Pop operators to output until matching `(` |
| Function | Push to operator stack; pop to output when `)` is found |

### Walkthrough: `1 + 2 * 3`

| Step | Token | Output Queue | Operator Stack | Action |
|------|-------|-------------|----------------|--------|
| 1 | `1` | `1` | | Number → output |
| 2 | `+` | `1` | `+` | Push operator |
| 3 | `2` | `1 2` | `+` | Number → output |
| 4 | `*` | `1 2` | `+ *` | `*` has higher precedence than `+`, so push (don't pop) |
| 5 | `3` | `1 2 3` | `+ *` | Number → output |
| 6 | end | `1 2 3 * +` | | Pop remaining operators |

Result: `1 2 3 * +` — multiply 2 and 3 first, then add 1. Correct!

### Walkthrough: `(1 + 2) * 3`

| Step | Token | Output Queue | Operator Stack | Action |
|------|-------|-------------|----------------|--------|
| 1 | `(` | | `(` | Push left paren |
| 2 | `1` | `1` | `(` | Number → output |
| 3 | `+` | `1` | `( +` | Push operator |
| 4 | `2` | `1 2` | `( +` | Number → output |
| 5 | `)` | `1 2 +` | | Pop until `(` found |
| 6 | `*` | `1 2 +` | `*` | Push operator |
| 7 | `3` | `1 2 + 3` | `*` | Number → output |
| 8 | end | `1 2 + 3 *` | | Pop remaining |

Result: `1 2 + 3 *` — add 1 and 2 first, then multiply by 3. The parentheses worked!

### The Precedence Rule

The key line in the implementation:

```kotlin
if (top.precedence > token.precedence ||
    (top.precedence == token.precedence && token.leftAssociative))
```

When pushing an operator, we pop operators that have **strictly higher precedence**, or **equal precedence if left-associative**. This handles cases like `1 - 2 - 3` correctly: subtraction is left-associative, so we evaluate left-to-right as `(1 - 2) - 3 = -4`, not `1 - (2 - 3) = 2`.

## Stage 3: Stack Evaluation

Evaluating postfix is trivially simple. Walk left to right:
- **Number**: push onto stack
- **Operator**: pop two operands, compute, push result
- **Function**: pop one operand, compute, push result

```kotlin
for (token in postfix) {
    when (token) {
        is Token.Number -> stack.addLast(token.value)
        is Token.Operator -> {
            val right = stack.removeLast()
            val left = stack.removeLast()
            stack.addLast(apply(token.symbol, left, right))
        }
        is Token.Function -> {
            val arg = stack.removeLast()
            stack.addLast(apply(token.name, arg))
        }
    }
}
```

For `1 2 3 * +`: push 1, push 2, push 3, pop 3 and 2 → multiply → push 6, pop 6 and 1 → add → push 7. Done.

## What I Learned

**The shunting-yard algorithm is elegant.** It solves a genuinely hard problem — parsing nested, precedence-aware expressions — with just two data structures and a handful of rules. No recursion, no grammar definitions, no parser generators.

**Encoding precedence in tokens simplifies everything.** Instead of a lookup table or switch statement in the algorithm, the precedence travels with the operator. The shunting-yard code just reads `token.precedence` and compares.

**Sealed interfaces make the pipeline type-safe.** Each stage produces and consumes `List<Token>`, but the exhaustive `when` expressions ensure every token type is handled. The compiler catches missing cases at compile time.

**Unary minus is the hardest part.** The actual algorithm is straightforward once you have clean tokens. The tricky bit is distinguishing `-` as subtraction from `-` as negation during tokenization. Context-dependent lexing (looking at the previous token) is the simplest solution.

**Three stages is the right decomposition.** Tokenizer tests catch lexing bugs. Shunting-yard tests verify precedence ordering. Evaluator tests confirm arithmetic. When something breaks, the stage tells you where to look.
