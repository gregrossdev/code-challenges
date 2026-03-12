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

## 2026-03-12 — Architecture: How should the URL shortener be structured?

**Decision:** Build on top of the web server from Phase 11. Six components: (1) **UrlStore** — in-memory storage with dual maps (shortCode→URL and URL→shortCode for idempotency). (2) **CodeGenerator** — generates short codes via SHA-256 hash truncated to base62, with collision retry. (3) **JsonParser** — minimal JSON body parser for extracting the `url` field from POST requests (reuse project's existing JSON parser or write a lightweight extractor). (4) **ApiHandler** — routes requests to create/redirect/delete operations, builds HTTP responses with JSON bodies. (5) **RequestParser** — reuse from Phase 11, extended to read request body via Content-Length. (6) **WebServer** — reuse from Phase 11 with virtual threads.
**Rationale:** The web server infrastructure already exists. The URL shortener is an API layer on top of it. In-memory storage is sufficient for the challenge (no database dependency). Dual maps enable O(1) idempotency checks.
**Alternatives considered:** External database (over-engineered for the challenge), new HTTP framework (defeats purpose), counter-based codes (challenge recommends hashing).
**Status:** ACTIVE
**ID:** D-12.1

## 2026-03-12 — Short code generation: How to create short codes?

**Decision:** SHA-256 hash the long URL, encode the first 48 bits as base62 (8 characters). On collision, append an incrementing counter to the URL before re-hashing. Base62 alphabet: `0-9A-Za-z`. 8 characters gives ~218 trillion combinations.
**Rationale:** Challenge explicitly recommends hashing with collision handling. SHA-256 is deterministic (same URL → same hash → natural idempotency before even checking storage). Base62 is URL-safe without encoding. 8 chars balances brevity with collision resistance.
**Alternatives considered:** Random codes (no natural idempotency), counter-based (predictable, challenge recommends hashing), MD5 (weaker but sufficient — SHA-256 is equally easy).
**Status:** ACTIVE
**ID:** D-12.2

## 2026-03-12 — API: What endpoints and behavior?

**Decision:** Three endpoints per the challenge: `POST /` (create, 201/400), `GET /{code}` (redirect, 302/404), `DELETE /{code}` (delete, 200). POST is idempotent — same URL returns existing short code. DELETE is idempotent — deleting nonexistent code returns 200. POST body is JSON `{"url": "..."}`. Responses are JSON `{"key": "...", "long_url": "...", "short_url": "..."}`. Validate URL has scheme (http/https).
**Rationale:** Matches challenge specification exactly. Idempotent POST avoids duplicate entries. 302 (not 301) ensures every redirect hits the server. URL validation prevents garbage entries.
**Alternatives considered:** 301 redirect (browsers cache, losing analytics), non-idempotent POST (creates duplicates).
**Status:** ACTIVE
**ID:** D-12.3

## 2026-03-12 — Configuration: How to configure the server?

**Decision:** CLI flags: `--port` (default 8080), `--base-url` (default `http://localhost:8080`). The base-url is used to construct `short_url` in responses (e.g., `http://localhost:8080/abc123`).
**Rationale:** Port reuses the web server convention. Base URL is needed to return fully qualified short URLs in API responses. Separate from port since in production the public URL may differ from the bind address.
**Status:** ACTIVE
**ID:** D-12.4

## 2026-03-12 — Testing: What test strategy?

**Decision:** Three tiers: (1) **CodeGeneratorTest** — deterministic hash output, collision retry, base62 encoding. (2) **ApiHandlerTest** — create/redirect/delete operations, idempotency, validation, error responses. (3) **IntegrationTest** — start server on random port, send real HTTP requests via Socket, verify full request/response cycle including redirects.
**Rationale:** Generator and handler are testable without sockets. Integration tests verify the HTTP layer. Follows established 3-tier pattern from web server and Redis phases.
**Status:** ACTIVE
**ID:** D-12.5

## 2026-03-12 — Content: Article write-up

**Decision:** Write `challenges/url-shortener/ARTICLE.md` after implementation. Include Usage section per template. Focus on hash-based code generation, idempotency, 302 vs 301 redirects, in-memory dual-map storage, and building a REST API from raw sockets.
**Rationale:** Established convention with Usage section per feedback.
**Status:** ACTIVE
**ID:** D-12.6
