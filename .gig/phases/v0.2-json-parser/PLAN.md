# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 2 — JSON Parser (v0.2.x)

> Build a JSON parser from scratch with a two-phase lexer/parser architecture, supporting all JSON types (objects, arrays, strings, numbers, booleans, null), nested structures, and useful error messages with line/column positions. Deliver as `gig-json` native binary with article write-up.

**Decisions:** D-2.1, D-2.2, D-2.3, D-2.4, D-2.5, D-2.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 2.1 | `0.2.1` | Module scaffold, data model & test fixtures | in-session | done |
| 2.2 | `0.2.2` | Lexer implementation & tests | in-session | done |
| 2.3 | `0.2.3` | Parser implementation & tests | in-session | done |
| 2.4 | `0.2.4` | CLI, native image & integration tests | in-session | done |
| 2.5 | `0.2.5` | JSON parser article write-up | in-session | done |

### Batch 2.1 — Module scaffold, data model & test fixtures

**Delegation:** in-session
**Decisions:** D-2.2, D-2.4
**Files:**
- `settings.gradle.kts` (modify — add `challenges:json-parser`)
- `challenges/json-parser/build.gradle.kts` (create)
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/JsonValue.kt` (create)
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/JsonParseException.kt` (create)
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/Token.kt` (create)
- `challenges/json-parser/src/test/resources/step1/` (create — valid.json, invalid.json)
- `challenges/json-parser/src/test/resources/step2/` (create — valid/invalid files)
- `challenges/json-parser/src/test/resources/step3/` (create — valid/invalid files)
- `challenges/json-parser/src/test/resources/step4/` (create — valid/invalid files)

**Work:**
1. Add `challenges:json-parser` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-json`.
3. Define `Token` sealed interface: `LeftBrace`, `RightBrace`, `LeftBracket`, `RightBracket`, `Colon`, `Comma`, `StringToken`, `NumberToken`, `BoolToken`, `NullToken`, `EOF`. Each token carries line/column.
4. Define `JsonValue` sealed interface with subtypes per D-2.2.
5. Define `JsonParseException` per D-2.5.
6. Download/create test fixture files for steps 1-4 from the challenge.

**Test criteria:**
- `./gradlew :challenges:json-parser:build` compiles.
- Test fixture files exist in correct directories.

**Acceptance:** Module compiles, data model defined, test fixtures ready.

### Batch 2.2 — Lexer implementation & tests

**Delegation:** in-session
**Decisions:** D-2.1, D-2.5
**Depends on:** Batch 2.1
**Files:**
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/Lexer.kt` (create)
- `challenges/json-parser/src/test/kotlin/dev/gregross/challenges/json/LexerTest.kt` (create)

**Work:**
1. Implement `Lexer` class that takes a `String` input and produces a `List<Token>`.
2. Track line/column position during scanning.
3. Handle: whitespace skipping, string literals (with escape sequences: `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, `\uXXXX`), numbers (integer, decimal, exponent, negative), `true`, `false`, `null`, structural characters `{}[]:,`.
4. Throw `JsonParseException` with position for invalid characters or malformed tokens.
5. Write lexer unit tests: empty object tokens, string with escapes, all number formats, booleans, null, invalid input errors with position.

**Test criteria:**
- `./gradlew :challenges:json-parser:test` — all lexer tests pass.
- Lexer produces correct tokens for `{}`, `{"key": "value"}`, `{"n": 42, "b": true, "x": null}`.
- Lexer throws with line/column for invalid input.

**Acceptance:** Lexer tokenizes all JSON token types correctly. Error messages include position.

### Batch 2.3 — Parser implementation & tests

**Delegation:** in-session
**Decisions:** D-2.1, D-2.2, D-2.5
**Depends on:** Batch 2.2
**Files:**
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/Parser.kt` (create)
- `challenges/json-parser/src/test/kotlin/dev/gregross/challenges/json/ParserTest.kt` (create)

**Work:**
1. Implement `Parser` class that takes `List<Token>` and produces a `JsonValue`.
2. Recursive descent: `parseValue()`, `parseObject()`, `parseArray()`.
3. Handle all nesting levels, trailing comma rejection, duplicate key handling.
4. Throw `JsonParseException` with position for structural errors.
5. Write parser unit tests: empty object, string key-value, all value types, nested objects/arrays, invalid structures (missing colon, trailing comma, unclosed brace).

**Test criteria:**
- `./gradlew :challenges:json-parser:test` — all parser tests pass.
- Parser produces correct `JsonValue` tree for each step's valid files.
- Parser throws descriptive errors for each step's invalid files.

**Acceptance:** Parser builds correct value trees for all valid JSON. Rejects all invalid JSON with useful errors.

### Batch 2.4 — CLI, native image & integration tests

**Delegation:** in-session
**Decisions:** D-2.3, D-2.4
**Depends on:** Batch 2.3
**Files:**
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/PrettyPrinter.kt` (create)
- `challenges/json-parser/src/main/kotlin/dev/gregross/challenges/json/Main.kt` (create)
- `challenges/json-parser/src/test/kotlin/dev/gregross/challenges/json/IntegrationTest.kt` (create)

**Work:**
1. Implement `PrettyPrinter` that converts `JsonValue` back to formatted JSON string.
2. Implement `main()` with args: optional `--validate` flag, file argument or stdin.
   - Valid JSON: exit 0, pretty-print to stdout (or silent with `--validate`).
   - Invalid JSON: exit 1, error message to stderr.
3. Write integration tests running through all step1-step4 test files — assert valid files parse, invalid files fail.
4. Build native image and install: `./gradlew :challenges:json-parser:install`.
5. Manually verify `gig-json` works from PATH.

**Test criteria:**
- `./gradlew :challenges:json-parser:test` — all tests pass (lexer + parser + integration).
- `gig-json valid.json` → exit 0, pretty-printed output.
- `gig-json invalid.json` → exit 1, error message with position.
- `echo '{"a":1}' | gig-json` → works from stdin.
- `gig-json --validate valid.json` → exit 0, no output.

**Acceptance:** CLI works with files and stdin. Exit codes correct. Native binary installed as `gig-json`.

### Batch 2.5 — JSON parser article write-up

**Delegation:** in-session
**Decisions:** D-2.6
**Depends on:** Batch 2.4
**Files:**
- `challenges/json-parser/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template using gig artifacts.
2. Focus areas: lexer/parser architecture (two-phase design), token design with sealed interfaces, recursive descent parsing, error handling with positions, Kotlin patterns (sealed interfaces, exhaustive when).

**Test criteria:**
- `challenges/json-parser/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-json` parses all valid JSON (objects, arrays, strings, numbers, booleans, null, nested)
- [ ] `gig-json` rejects all invalid JSON with useful error messages including line/column
- [ ] Exit code 0 for valid, 1 for invalid
- [ ] Pretty-prints valid JSON to stdout
- [ ] `--validate` flag for silent validation
- [ ] Reads from file argument or stdin
- [ ] All challenge step1-step4 test files pass
- [ ] Native binary installed as `gig-json`
- [ ] Article written at `challenges/json-parser/ARTICLE.md`

**Completion triggers Phase 3 → version `0.3.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
