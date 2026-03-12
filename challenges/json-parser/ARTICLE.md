# Building My Own JSON Parser: A Lexer and Parser in Kotlin

> Building a JSON parser from scratch — tokenization, recursive descent parsing, error reporting with line/column positions, and a pretty-printer — all in Kotlin with no external dependencies.

## The Challenge

**Source:** [Coding Challenges - Build Your Own JSON Parser](https://codingchallenges.fyi/challenges/challenge-json-parser)

JSON is everywhere. Every API, every config file, every data exchange format touches it. But how does a JSON parser actually work? This challenge asks you to build one from scratch, implementing both lexical analysis (turning raw text into tokens) and syntactic analysis (turning tokens into a structured value tree).

The challenge progresses through five steps: start with `{}`, add string key-value pairs, support all value types (numbers, booleans, null), handle nested objects and arrays, and finally validate against the standard json.org test suite. Each step builds on the last, which maps naturally to how real parsers are constructed.

This is challenge #2 on Coding Challenges, and it's a significant step up from `wc`. Instead of processing bytes and counting things, you're building a compiler's front end — the same techniques used in programming languages, query engines, and template systems.

## Usage

```bash
gig-json [--validate] [file]
```

| Flag | Description |
|------|-------------|
| `--validate` | Validate JSON without pretty-printing |

Reads from stdin if no file given. Exit code 0 on valid JSON, 1 on parse error.

```bash
gig-json data.json
gig-json --validate data.json
cat data.json | gig-json
```

## Approach

The key architectural decision was a clean two-phase design: a **Lexer** that converts raw input into a stream of typed tokens, and a **Parser** that consumes those tokens and builds a tree of `JsonValue` nodes. This separation is fundamental to compiler design and makes each phase independently testable.

Kotlin's sealed interfaces were the perfect fit for both the token types and the JSON value tree. The compiler enforces exhaustive `when` matching, so adding a new token type or value type forces you to handle it everywhere — no silent bugs from missing cases.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Architecture | Two-phase: Lexer → Parser | Each phase testable independently, mirrors real compiler design |
| Data model | Sealed interface `JsonValue` | Exhaustive pattern matching, type safety |
| Error handling | `JsonParseException` with line/column | Challenge asks for useful error messages; position tracking is the standard |
| Number storage | `Double` for all numbers | JSON spec doesn't distinguish int/float; Double covers the range |
| Key ordering | `LinkedHashMap` in `JsonObject` | Preserves insertion order for predictable pretty-printing |

## Build Log

### Step 1 — Data Model and Tokens

Defined the foundation: `Token` sealed interface with 11 variants (braces, brackets, colon, comma, string, number, bool, null, EOF), each carrying a `Position(line, column)`. The `JsonValue` sealed interface has 6 variants covering every JSON type. `JsonParseException` carries optional position context.

### Step 2 — The Lexer

The lexer scans character by character, tracking line and column position. The core loop skips whitespace, then dispatches on the current character:

- `{`, `}`, `[`, `]`, `:`, `,` → single-character structural tokens
- `"` → string with escape sequence handling
- `-` or digit → number with integer/decimal/exponent parts
- `t`, `f` → boolean literal
- `n` → null literal
- Anything else → error with position

String handling was the most complex part — supporting all JSON escape sequences (`\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, `\uXXXX`) and rejecting control characters below U+0020.

### Step 3 — The Parser

Recursive descent, consuming tokens left to right. Three core methods:

- `parseValue()` — dispatches on the current token type
- `parseObject()` — handles `{ key: value, ... }` with comma separation
- `parseArray()` — handles `[ value, ... ]` with comma separation

The parser rejects trailing commas, unquoted keys, unclosed structures, and extra tokens after the top-level value. Every error includes the position from the offending token.

### Step 4 — CLI and Pretty-Printer

The `gig-json` CLI reads from a file argument or stdin. The pretty-printer converts the `JsonValue` tree back to formatted JSON with indentation, demonstrating that the parser actually built a complete value tree (not just validated syntax). A `--validate` flag provides silent validation for shell scripting.

## Key Code

### Sealed interface for type-safe JSON values

```kotlin
sealed interface JsonValue {
    data class JsonObject(val members: LinkedHashMap<String, JsonValue>) : JsonValue
    data class JsonArray(val elements: List<JsonValue>) : JsonValue
    data class JsonString(val value: String) : JsonValue
    data class JsonNumber(val value: Double) : JsonValue
    data class JsonBool(val value: Boolean) : JsonValue
    data object JsonNull : JsonValue
}
```

Every `when` on a `JsonValue` must handle all six cases — the compiler enforces this. No `else` branch needed, no missing cases possible.

### Recursive descent parsing

```kotlin
private fun parseValue(): JsonValue {
    return when (val token = current()) {
        is Token.LeftBrace -> parseObject()
        is Token.LeftBracket -> parseArray()
        is Token.StringToken -> { advance(); JsonValue.JsonString(token.value) }
        is Token.NumberToken -> { advance(); JsonValue.JsonNumber(token.value) }
        is Token.BoolToken -> { advance(); JsonValue.JsonBool(token.value) }
        is Token.NullToken -> { advance(); JsonValue.JsonNull }
        is Token.EOF -> throw JsonParseException("Unexpected end of input", token.position)
        else -> throw JsonParseException("Unexpected token", token.position)
    }
}
```

The beauty of recursive descent: `parseObject()` calls `parseValue()` for each member's value, which might call `parseObject()` again for nested objects. The recursion handles arbitrary nesting depth naturally.

### Error reporting with position

```kotlin
class JsonParseException(
    message: String,
    val position: Position? = null,
) : Exception(
    if (position != null) "$message at $position" else message
)
```

Outputs like `gig-json: Trailing comma in object at line 1, column 17` — precise enough to find the problem immediately.

## Testing

Three tiers of tests covering the lexer, parser, and end-to-end integration.

| Test Case | Category | Expected | Result |
|-----------|----------|----------|--------|
| Empty object `{}` | Step 1 | Valid, parses to empty JsonObject | Pass |
| Empty input | Step 1 | Invalid, throws | Pass |
| String key-value | Step 2 | Valid, correct key/value | Pass |
| Trailing comma | Step 2 | Invalid, throws with position | Pass |
| Unquoted key | Step 2 | Invalid, throws | Pass |
| All value types | Step 3 | Valid, correct types | Pass |
| Capitalized `False` | Step 3 | Invalid, throws | Pass |
| Nested objects/arrays | Step 4 | Valid, correct nesting | Pass |
| Deeply nested | Step 4 | Valid, correct structure | Pass |
| Trailing comma in nested | Step 4 | Invalid, throws | Pass |
| Unicode escapes | Lexer | Correct character | Pass |
| Leading zeros in numbers | Lexer | Invalid, throws | Pass |
| Exponent notation | Lexer | Correct double value | Pass |
| Pretty-print round-trip | Integration | Re-parses to same structure | Pass |

Total: 49 tests across lexer (17), parser (20), and integration (12).

## What I Learned

- **Separating lexing from parsing is worth it every time.** When a test fails, you immediately know whether the bug is in tokenization or in structural parsing. Debugging a combined single-pass parser is much harder.

- **Kotlin's sealed interfaces are perfect for compiler-style code.** Exhaustive `when` matching means the compiler catches missing cases at build time. Adding a new token type breaks everywhere it needs to be handled — exactly what you want.

- **Position tracking is cheap to add early, expensive to add later.** Threading line/column through the lexer from the start means every error message is immediately useful. Retrofitting positions after the fact requires touching every token creation site.

- **JSON's number format is stricter than you'd think.** No leading zeros (except `0` itself), no leading `+`, no trailing decimal point. The spec is precise, and getting it right requires careful state management in the lexer.

## Running It

```bash
# Pretty-print a JSON file
gig-json path/to/file.json

# Validate silently (exit code only)
gig-json --validate path/to/file.json

# Read from stdin
echo '{"key": [1, 2, 3]}' | gig-json

# Check exit code
gig-json --validate file.json && echo "Valid" || echo "Invalid"
```

---

*Built as part of my [Coding Challenges](https://codingchallenges.fyi) series. Stack: Kotlin + Gradle.*
