# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.2.0` |
| **Phase** | 2 — JSON Parser |
| **Status** | `GATHERED` |
| **Last Batch** | — |
| **Last Updated** | 2026-03-11 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.1.4 | 1 | GraalVM native image support | UNPLANNED | done | 2026-03-11 |
| 0.1.3 | 1 | wc challenge article write-up | PLANNED | done | 2026-03-11 |
| 0.1.2 | 1 | wc tool implementation & tests | PLANNED | done | 2026-03-11 |
| 0.1.1 | 1 | Gradle monorepo scaffolding & article template | PLANNED | done | 2026-03-11 |
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-10 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

- D-2.1: Two-phase parser — Lexer (tokens) → Parser (value tree)
- D-2.2: Sealed interface `JsonValue` with typed subtypes
- D-2.3: `gig-json` CLI — exit codes 0/1, pretty-print, `--validate` flag
- D-2.4: 3-tier testing — lexer unit, parser unit, integration with challenge + json.org suites
- D-2.5: `JsonParseException` with line/column/message
- D-2.6: Article write-up post-implementation

---

## Open Flags

<!-- Items that need human attention -->

_None._

---

## Working Memory

<!-- Key context: file paths, patterns, naming conventions, gotchas.
     Updated during plan and apply. Keep under 100 lines. -->

---

## Open Issues

<!-- Summary of deferred issues from ISSUES.md -->

_None._

---

## Session Recovery

1. Read this file — current state
2. Read `PLAN.md` — what's next
3. Read `DECISIONS.md` — what's been decided
4. Read `ISSUES.md` — open/deferred issues
5. Resume from next batch
