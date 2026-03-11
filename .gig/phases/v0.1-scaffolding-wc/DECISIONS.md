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

## 2026-03-10 — Structure: How should the repository be organized?

**Decision:** Gradle multi-module monorepo. Each challenge is a standalone subproject under a `challenges/` directory (e.g., `challenges/wc`, `challenges/json-parser`). A root `build.gradle.kts` defines shared conventions via a `buildSrc` convention plugin.
**Rationale:** Monorepo keeps all challenges together with shared build config, avoids duplicating Gradle wrapper and settings across dozens of repos. Multi-module lets each challenge have independent dependencies and run configurations while sharing Kotlin version, test framework, and code style settings.
**Alternatives considered:** Separate repositories per challenge (too much overhead), flat modules at root level (clutters root directory at 100+ challenges).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-10 — Stack: What Kotlin and Gradle versions to target?

**Decision:** Kotlin 2.1.x (latest stable) on Gradle 8.12+ with Kotlin DSL (`build.gradle.kts`). Target JVM 21 (latest LTS).
**Rationale:** Kotlin 2.1 brings the stable K2 compiler with better performance. JVM 21 is the current LTS with virtual threads and modern APIs useful for networking challenges. Gradle 8.12+ has full Kotlin 2.1 support.
**Alternatives considered:** Kotlin 1.9 (outdated), JVM 17 (misses virtual threads and newer APIs useful for server/networking challenges).
**Status:** REVISED
**ID:** D-1.2

## 2026-03-10 — Stack: What Kotlin and Gradle versions to target? (revised)

**Decision:** Kotlin 2.3.10 (latest stable) on Gradle 9.4.0 with Kotlin DSL (`build.gradle.kts`). Target JVM 21 (latest LTS). Use `jvmToolchain(21)` for toolchain configuration.
**Rationale:** Research found Kotlin 2.3.10 and Gradle 9.4.0 are the current stable releases as of March 2026. Original proposal of 2.1.x/8.12+ was outdated. JVM 21 LTS target unchanged.
**Alternatives considered:** Kotlin 2.1.21 (last 2.1.x, still supported but older), Gradle 8.14.4 (last 8.x, no longer latest line).
**Status:** REVISED
**ID:** D-1.2r

## 2026-03-11 — Stack: Actual Kotlin and Gradle versions (governance correction)

**Decision:** Kotlin 2.1.20 on Gradle 9.3.1 with Kotlin DSL (`build.gradle.kts`). Target JVM 21 (latest LTS). GraalVM 24 (Oracle) for native image compilation.
**Rationale:** D-1.2r contained hallucinated versions from subagent research (2.3.10 / 9.4.0 don't exist). Actual latest stable: Kotlin 2.1.20, Gradle 9.3.1. JVM 21 toolchain for compilation, GraalVM 24 toolchain for native-image.
**Alternatives considered:** N/A — correcting to match reality.
**Status:** ACTIVE
**ID:** D-1.2r2

## 2026-03-10 — Testing: What test framework to use?

**Decision:** JUnit 5 with kotlin-test assertions. Add kotest property-testing library as optional per-module dependency for challenges that benefit from it (parsers, encoders).
**Rationale:** JUnit 5 is the Gradle/Kotlin standard — zero friction, excellent IDE support, familiar to most developers. kotlin-test provides idiomatic assertion syntax. Kotest property testing is valuable for parser/encoder challenges but not needed everywhere.
**Alternatives considered:** Kotest full framework (heavier setup, less conventional), TestNG (no Kotlin advantage).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-10 — CLI: How should challenge CLIs handle argument parsing?

**Decision:** Start with raw `args` parsing for simple challenges (wc, cat, head). Introduce Clikt library when a challenge needs subcommands, flags, or complex options.
**Rationale:** Simple challenges like `wc` only need 1-2 positional args — a CLI framework adds unnecessary complexity. Clikt is the idiomatic Kotlin CLI library and will be valuable for more complex tools (curl, jq, redis-cli). Escalating complexity matches the challenge progression.
**Alternatives considered:** Picocli (annotation-heavy, more Java-idiomatic), Clikt everywhere (overkill for simple tools).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-10 — First Challenge: Which challenge to tackle first?

**Decision:** Challenge #1 — Build your own `wc` (word count tool).
**Rationale:** It's the first challenge on the site, exercises core Kotlin skills (file I/O, string processing, CLI args), is self-contained, and establishes the project scaffolding pattern that all subsequent challenges will follow. Perfect for validating the monorepo setup.
**Alternatives considered:** JSON Parser (#2, more complex — better as second challenge), cat (#15, too trivial to validate the setup properly).
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-10 — Conventions: How should challenge modules be named and structured?

**Decision:** Module directory: `challenges/{tool-name}` (kebab-case). Package: `dev.gregross.challenges.{toolname}`. Each module contains `src/main/kotlin/` and `src/test/kotlin/`. Entry point is a `main()` function in `Main.kt`. Each module has its own `build.gradle.kts` that applies the shared convention plugin.
**Rationale:** Consistent naming makes navigation predictable. Package mirrors the directory. Convention plugin ensures every new challenge module only needs to declare its unique dependencies.
**Alternatives considered:** Numbered directories like `01-wc` (couples to site ordering, hard to insert), flat `src/` with packages only (loses Gradle module isolation).
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-10 — Content: How should challenge write-ups be structured?

**Decision:** Create a reusable Markdown article template at `templates/article.md`. Each completed challenge gets a write-up in `challenges/{tool-name}/ARTICLE.md` using that template. The template is structured to pull from gig artifacts: decisions become the "Approach" section, batch history becomes "Build Log", test criteria become "Testing", and the final result becomes "What I Learned". Written after governance completes, using `.gig/` data as the primary source.
**Rationale:** A reusable template ensures consistent, publishable articles across all challenges. Writing post-completion means the gig artifacts (DECISIONS.md, PLAN.md, batch history in STATE.md) are the raw material — no extra note-taking during implementation. The template lives in `templates/` at the repo root so it's shared across all challenges.
**Alternatives considered:** Freeform write-ups per challenge (inconsistent quality/structure), writing during implementation (distracts from building), separate blog repo (disconnects article from code).
**Status:** ACTIVE
**ID:** D-1.7
