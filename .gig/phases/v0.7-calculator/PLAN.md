# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 7 — Calculator (v0.7.x)

> Build a command-line calculator that parses and evaluates mathematical expressions using Dijkstra's shunting-yard algorithm. Supports basic arithmetic with operator precedence, parenthesized subexpressions, and trigonometric functions. Three-stage pipeline: tokenizer → shunting-yard (infix to postfix) → stack evaluator. Deliver as `gig-calc` native binary with article write-up.

**Decisions:** D-7.1, D-7.2, D-7.3, D-7.4, D-7.5, D-7.6, D-7.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 7.1 | `0.7.1` | Module scaffold & Token model | in-session | done |
| 7.2 | `0.7.2` | Tokenizer | in-session | done |
| 7.3 | `0.7.3` | Shunting-yard & evaluator | in-session | done |
| 7.4 | `0.7.4` | CLI, native image & integration tests | in-session | done |
| 7.5 | `0.7.5` | Calculator article write-up | in-session | done |

### Batch 7.1 — Module scaffold & Token model

**Delegation:** in-session
**Decisions:** D-7.1, D-7.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:calculator`)
- `challenges/calculator/build.gradle.kts` (create)
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/Token.kt` (create)
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/Main.kt` (create — stub)

**Work:**
1. Add `challenges:calculator` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-calc`.
3. Define `Token` sealed interface with: `Number(value: Double)`, `Operator(symbol: Char, precedence: Int, leftAssociative: Boolean)`, `LeftParen`, `RightParen`, `Function(name: String)`.
4. Add companion/factory for standard operators: `+` (prec 1), `-` (prec 1), `*` (prec 2), `/` (prec 2).
5. Create Main.kt stub.

**Test criteria:**
- `./gradlew :challenges:calculator:compileKotlin` — compiles cleanly.
- Token sealed interface has all required subtypes.

**Acceptance:** Module compiles, token model defined.

### Batch 7.2 — Tokenizer

**Delegation:** in-session
**Decisions:** D-7.2, D-7.5
**Depends on:** Batch 7.1
**Files:**
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/Tokenizer.kt` (create)
- `challenges/calculator/src/test/kotlin/dev/gregross/challenges/calc/TokenizerTest.kt` (create)

**Work:**
1. Implement `Tokenizer` with `fun tokenize(input: String): List<Token>`. Walk input char-by-char: accumulate digits/dots into numbers, recognize operators, match parentheses, match function names (alphabetic sequences followed by `(`).
2. Handle whitespace (skip), negative numbers (unary minus after operator or at start), decimal numbers.
3. Throw descriptive errors for invalid characters.
4. Write tests: integers, decimals, operators, parentheses, function names, whitespace handling, negative numbers, invalid input errors.

**Test criteria:**
- `./gradlew :challenges:calculator:test` — all tokenizer tests pass.
- `"1 + 2"` tokenizes to `[Number(1.0), Operator(+), Number(2.0)]`.
- `"sin(0)"` tokenizes to `[Function(sin), LeftParen, Number(0.0), RightParen]`.

**Acceptance:** Tokenizer correctly lexes all expression types from the challenge.

### Batch 7.3 — Shunting-yard & evaluator

**Delegation:** in-session
**Decisions:** D-7.1, D-7.3, D-7.5
**Depends on:** Batch 7.2
**Files:**
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/ShuntingYard.kt` (create)
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/Evaluator.kt` (create)
- `challenges/calculator/src/test/kotlin/dev/gregross/challenges/calc/ShuntingYardTest.kt` (create)
- `challenges/calculator/src/test/kotlin/dev/gregross/challenges/calc/EvaluatorTest.kt` (create)

**Work:**
1. Implement `ShuntingYard` with `fun toPostfix(tokens: List<Token>): List<Token>`. Standard Dijkstra algorithm: output queue + operator stack. Handle operators by precedence/associativity, parens by matching, functions by pushing/popping.
2. Throw errors for mismatched parentheses.
3. Implement `Evaluator` with `fun evaluate(postfix: List<Token>): Double`. Stack-based: push numbers, pop operands for operators/functions, push result. Handle division by zero.
4. Write shunting-yard tests: verify postfix ordering for `1 + 2 * 3`, `(1 + 2) * 3`, nested parens, functions.
5. Write evaluator tests: basic arithmetic, precedence, parentheses, trig functions, division by zero error.

**Test criteria:**
- `./gradlew :challenges:calculator:test` — all shunting-yard and evaluator tests pass.
- `1 + 2 * 3` evaluates to `7.0` (not `9.0`).
- `(1 + 1) * 5` evaluates to `10.0`.
- `sin(0)` evaluates to `0.0`.

**Acceptance:** Full pipeline correctly evaluates expressions with precedence, parens, and functions.

### Batch 7.4 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-7.4, D-7.6
**Depends on:** Batch 7.3
**Files:**
- `challenges/calculator/src/main/kotlin/dev/gregross/challenges/calc/Main.kt` (modify)
- `challenges/calculator/src/test/kotlin/dev/gregross/challenges/calc/IntegrationTest.kt` (create)

**Work:**
1. Implement `main()`: take expression as single argument, run through tokenizer → shunting-yard → evaluator, print result. Format output: integers as integers (no `.0`), decimals with reasonable precision.
2. Handle errors: catch exceptions, print to stderr, exit 1.
3. Write integration tests: all challenge examples plus edge cases.
4. Build native image: `./gradlew :challenges:calculator:nativeCompile`.
5. Install: `./gradlew :challenges:calculator:install`.
6. Present manual verification commands with symlink confirmation.

**Test criteria:**
- `./gradlew :challenges:calculator:test` — all tests pass.
- `gig-calc '1 + 2'` → `3`
- `gig-calc '2 * 3'` → `6`
- `gig-calc '(1 + 1) * 5'` → `10`
- `gig-calc 'sin(0)'` → `0`
- `gig-calc '3 / 2'` → `1.5`

**Acceptance:** Native binary installed as `gig-calc`, user has manually verified.

### Batch 7.5 — Calculator article write-up

**Delegation:** in-session
**Decisions:** D-7.7
**Depends on:** Batch 7.4
**Files:**
- `challenges/calculator/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template.
2. Focus areas: shunting-yard algorithm walkthrough (infix → postfix conversion), stack-based evaluation, operator precedence handling, how parentheses override precedence, extending with functions.

**Test criteria:**
- `challenges/calculator/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-calc '1 + 2'` returns `3`
- [ ] `gig-calc '2 - 1'` returns `1`
- [ ] `gig-calc '2 * 3'` returns `6`
- [ ] `gig-calc '3 / 2'` returns `1.5`
- [ ] Operator precedence: `1 + 2 * 3` returns `7`
- [ ] Parentheses: `(1 + 1) * 5` returns `10`
- [ ] Nested parens: `((2 + 3) * (4 - 1))` returns `15`
- [ ] Trig functions: `sin(0)` returns `0`, `cos(0)` returns `1`
- [ ] Error on invalid input (stderr, exit 1)
- [ ] Error on mismatched parentheses
- [ ] Error on division by zero
- [ ] Native binary installed as `gig-calc`
- [ ] Article written at `challenges/calculator/ARTICLE.md`

**Completion triggers Phase 8 → version `0.8.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
