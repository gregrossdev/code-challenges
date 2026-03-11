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

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** ACTIVE | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-11 — Architecture: How should the JSON parser be structured internally?

**Decision:** Two-phase design: a `Lexer` (tokenizer) that converts raw input into a stream of tokens, and a `Parser` that consumes tokens and builds a JSON value tree. Separate classes for each concern.
**Rationale:** The challenge explicitly calls out lexical analysis and syntactic analysis as core concepts. Separating them makes each phase testable independently, mirrors real compiler architecture, and is more educational. The lexer handles character-level concerns (string escaping, number formats), the parser handles structure (nesting, commas, colons).
**Alternatives considered:** Single-pass recursive descent (simpler but conflates concerns), regex-based parsing (fragile, hard to extend), using an existing parser library (defeats the purpose).
**Status:** ACTIVE
**ID:** D-2.1

## 2026-03-11 — Data Model: How should parsed JSON values be represented?

**Decision:** Kotlin sealed interface `JsonValue` with subtypes: `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBool`, `JsonNull`. `JsonObject` wraps a `LinkedHashMap<String, JsonValue>`, `JsonArray` wraps a `List<JsonValue>`.
**Rationale:** Sealed interfaces give exhaustive `when` matching — the compiler enforces handling all JSON types. `LinkedHashMap` preserves key insertion order (useful for round-tripping). Each subtype is a value class or data class for clean equality semantics.
**Alternatives considered:** Single `JsonValue` class with a `type` enum (loses type safety), `Map<String, Any?>` (no type structure, casting everywhere).
**Status:** ACTIVE
**ID:** D-2.2

## 2026-03-11 — CLI: What should gig-json do and how should it behave?

**Decision:** `gig-json` validates JSON from a file argument or stdin. Exit code 0 for valid JSON, exit code 1 for invalid. On valid input, pretty-print the parsed JSON to stdout. On invalid input, print an error message with line/column to stderr. Support a `--validate` flag for validation-only mode (no output on success, just exit code).
**Rationale:** The challenge requires exit code behavior. Pretty-printing on success demonstrates the parser actually built a value tree (not just validation). Error messages with position info make the tool genuinely useful. `--validate` flag keeps it compatible with shell scripting (`gig-json --validate file.json && echo ok`).
**Alternatives considered:** Validation-only (doesn't prove parsing works), always-silent mode (less useful as a tool).
**Status:** ACTIVE
**ID:** D-2.3

## 2026-03-11 — Testing: What test strategy for the JSON parser?

**Decision:** Three tiers: (1) Unit tests for the Lexer (token output for specific inputs), (2) Unit tests for the Parser (JsonValue tree for specific inputs), (3) Integration tests using the challenge's step1-step4 test files plus the json.org JSON_checker test suite. Test files stored in `src/test/resources/`.
**Rationale:** The challenge provides step-by-step test files that serve as a natural progression. The json.org test suite is the standard compliance check. Unit-testing lexer and parser independently catches bugs at the right level. Three tiers give confidence that tokenization, parsing, and end-to-end all work.
**Alternatives considered:** Only integration tests (harder to debug failures), property-based testing only (misses specific edge cases the challenge tests for).
**Status:** ACTIVE
**ID:** D-2.4

## 2026-03-11 — Error Handling: How should parse errors be reported?

**Decision:** Custom `JsonParseException` with line number, column number, and descriptive message. The lexer tracks position as it scans. On error, throw with context like `"Unexpected character 'x' at line 3, column 12"` or `"Expected ':' after object key at line 1, column 8"`.
**Rationale:** The challenge specifically asks for "useful error messages." Line/column tracking is the standard for parser error reporting. A custom exception type lets the CLI distinguish parse errors from other failures cleanly.
**Alternatives considered:** Result type instead of exceptions (more functional but heavier for this scope), error messages without position (less useful).
**Status:** ACTIVE
**ID:** D-2.5

## 2026-03-11 — Content: Article write-up for JSON parser challenge

**Decision:** Write `challenges/json-parser/ARTICLE.md` after implementation using the established `templates/article.md` template. Source material from gig artifacts (decisions, batch history, test results). Focus on lexer/parser architecture, token design, error handling with positions, and the sealed interface pattern.
**Rationale:** Established convention from D-1.7 — every completed challenge gets a write-up. JSON parser is meatier than wc, so the article should highlight compiler fundamentals (lexing vs parsing) as the educational angle.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-2.6
