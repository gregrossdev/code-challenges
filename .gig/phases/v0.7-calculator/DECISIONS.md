# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

## 2026-03-11 — Architecture: How should the calculator be structured?

**Decision:** Three-stage pipeline: (1) **Tokenizer** — converts input string into a list of tokens (numbers, operators, parentheses, functions). (2) **Shunting-yard converter** — converts infix tokens to postfix (Reverse Polish Notation) using Dijkstra's algorithm. (3) **Evaluator** — evaluates the postfix token list using a stack. A `Token` sealed interface models all token types.
**Rationale:** The challenge explicitly recommends the shunting-yard algorithm. This three-stage pipeline gives clean separation — tokenizer handles lexing, shunting-yard handles precedence/parentheses, evaluator handles arithmetic. Each stage is independently testable. Matches the pattern established in Phase 2 (JSON parser) with sealed interfaces for type-safe token modeling.
**Alternatives considered:** Recursive descent parser (more code, overkill for this grammar), Pratt parser (steeper learning curve, better suited for language parsers), direct eval without intermediate representation (can't handle precedence correctly).
**Status:** ACTIVE
**ID:** D-7.1

## 2026-03-11 — Parsing: What token types are needed?

**Decision:** Sealed interface `Token` with: `Number(value: Double)`, `Operator(symbol: Char, precedence: Int, leftAssociative: Boolean)`, `LeftParen`, `RightParen`, `Function(name: String)`. Operators: `+` (prec 1), `-` (prec 1), `*` (prec 2), `/` (prec 2). Functions: `sin`, `cos`, `tan`.
**Rationale:** Covers all three challenge steps — basic arithmetic (Step 1), precedence/parentheses (Step 2), and trig functions (Step 3). Encoding precedence and associativity directly in the operator token simplifies the shunting-yard logic. Sealed interface ensures exhaustive when-expressions.
**Alternatives considered:** Separate enum for operators (less flexible for precedence encoding), string-based tokens (lose type safety), including power operator (not requested by challenge).
**Status:** ACTIVE
**ID:** D-7.2

## 2026-03-11 — Algorithm: How should the shunting-yard algorithm work?

**Decision:** Standard Dijkstra shunting-yard: process tokens left-to-right. Numbers go directly to output queue. Left parens push to operator stack. Right parens pop to output until matching left paren. Operators pop higher-or-equal precedence (for left-associative) operators from stack before pushing. Functions push to operator stack and pop to output when their closing paren is found. At end, pop remaining operators to output.
**Rationale:** This is the textbook algorithm. It correctly handles operator precedence, left associativity, parenthesized subexpressions, and function calls — all requirements of the challenge. Well-documented and predictable.
**Alternatives considered:** Modified algorithm with right-associativity support (not needed — all our operators are left-associative), building an AST instead of postfix (unnecessary indirection for evaluation).
**Status:** ACTIVE
**ID:** D-7.3

## 2026-03-11 — CLI: What interface should gig-calc provide?

**Decision:** `gig-calc '<expression>'` — takes a quoted expression as a single argument, prints the numeric result to stdout. Supports integers, decimals, operators (`+`, `-`, `*`, `/`), parentheses, and functions (`sin`, `cos`, `tan`). Trig functions take radians. Error messages to stderr for invalid input.
**Rationale:** Matches the challenge spec: `calc '2 * 3'` returns `6`. Single-argument design avoids shell interpretation of `*` and parentheses. Radians is the standard mathematical convention.
**Alternatives considered:** REPL mode (not requested), degree-based trig (non-standard), multi-argument parsing (shell would eat operators).
**Status:** ACTIVE
**ID:** D-7.4

## 2026-03-11 — Error handling: How should invalid input be handled?

**Decision:** Throw descriptive exceptions at each stage: tokenizer errors for invalid characters/malformed numbers, shunting-yard errors for mismatched parentheses, evaluator errors for division by zero or insufficient operands. CLI catches and prints to stderr with exit code 1.
**Rationale:** The challenge says "validate input for errors." Stage-specific errors make debugging straightforward. Clean error messages help the user understand what went wrong.
**Alternatives considered:** Result type (too heavy for a CLI tool), silent fallback values (hides bugs), single catch-all error (unhelpful messages).
**Status:** ACTIVE
**ID:** D-7.5

## 2026-03-11 — Testing: What test strategy for the calculator?

**Decision:** Three tiers matching the three stages: (1) Tokenizer tests — valid tokens, invalid characters, decimal numbers, negative numbers, function names. (2) Shunting-yard tests — precedence ordering, parentheses, nested expressions, functions. (3) Evaluator/integration tests — full expressions from the challenge: `1 + 2 = 3`, `2 * 3 = 6`, `(1 + 1) * 5 = 10`, `sin(0) = 0`, etc.
**Rationale:** Each stage has different failure modes. Tokenizer can fail on bad input, shunting-yard on mismatched parens, evaluator on division by zero. Testing each independently isolates bugs. Integration tests verify the full pipeline matches challenge expectations.
**Alternatives considered:** Only integration tests (can't pinpoint failures), property-based testing (overkill for deterministic math).
**Status:** ACTIVE
**ID:** D-7.6

## 2026-03-11 — Content: Article write-up for calculator challenge

**Decision:** Write `challenges/calculator/ARTICLE.md` after implementation. Focus on the shunting-yard algorithm — how infix-to-postfix conversion works, why operator precedence matters, and how stack-based evaluation produces the result.
**Rationale:** Established convention. The shunting-yard algorithm is the educational centerpiece — it's an elegant solution to a problem every developer encounters.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-7.7
