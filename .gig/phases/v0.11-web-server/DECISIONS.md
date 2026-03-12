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

## 2026-03-11 — Architecture: How should the web server be structured?

**Decision:** Five components: (1) **HttpRequest** — data class for parsed request (method, path, version, headers). (2) **HttpResponse** — data class for response (status code, reason, headers, body bytes). (3) **RequestParser** — parses raw input stream into HttpRequest. (4) **StaticFileHandler** — resolves paths to files in document root, reads content, determines MIME type, builds HttpResponse. Includes path traversal protection. (5) **WebServer** — TCP ServerSocket with virtual thread per client, delegates to parser and handler.
**Rationale:** Separating parsing from file handling from socket management makes each independently testable. Follows the Redis server pattern (RESP parser + command handler + server). Virtual threads (Java 21) are proven from the Redis challenge.
**Alternatives considered:** Single monolithic handler (harder to test), Netty/Ktor (defeats the purpose — challenge is about building from sockets).
**Status:** ACTIVE
**ID:** D-11.1

## 2026-03-11 — Protocol: What HTTP features to support?

**Decision:** HTTP/1.1 GET only. Response headers: `Content-Type`, `Content-Length`, `Connection: close`, `Server`. Status codes: 200 (OK), 400 (Bad Request), 403 (Forbidden), 404 (Not Found), 405 (Method Not Allowed). Close connection after each response (no keep-alive). Default document: `/` → `/index.html`.
**Rationale:** Challenge steps 1-4 only require GET. Connection close simplifies implementation without losing challenge value. The five status codes cover all challenge scenarios: success, bad parse, traversal blocked, missing file, wrong method.
**Alternatives considered:** Keep-alive support (adds complexity for no challenge value), HEAD method (not required), chunked transfer encoding (overkill).
**Status:** ACTIVE
**ID:** D-11.2

## 2026-03-11 — Security: How to prevent path traversal?

**Decision:** Resolve the requested path against the document root using `File.canonicalPath`. If the resolved canonical path does not start with the document root's canonical path, return 403 Forbidden. Also reject paths containing null bytes. Apply this check before any file read.
**Rationale:** `canonicalPath` resolves `..`, `.`, and symlinks to an absolute path. Prefix-checking against the document root is the standard defense. This is challenge step 4's explicit requirement.
**Alternatives considered:** Regex-based `..` stripping (incomplete — doesn't handle encoded sequences or symlinks), chroot (OS-level, not portable).
**Status:** ACTIVE
**ID:** D-11.3

## 2026-03-11 — Configuration: How should the server be configured?

**Decision:** CLI flags: `--port` (default 8080), `--docroot` (default `./www`). Port 8080 avoids requiring root. Document root is configurable per challenge step 4.
**Rationale:** Port 80 requires elevated privileges. 8080 is the standard development alternative. Configurable docroot is an explicit challenge requirement.
**Alternatives considered:** Port 80 default (needs sudo), config file (over-engineered for this challenge).
**Status:** ACTIVE
**ID:** D-11.4

## 2026-03-11 — Content types: How to determine MIME types?

**Decision:** Extension-based lookup map covering: `.html` → `text/html`, `.css` → `text/css`, `.js` → `text/javascript`, `.json` → `application/json`, `.png` → `image/png`, `.jpg`/`.jpeg` → `image/jpeg`, `.gif` → `image/gif`, `.ico` → `image/x-icon`, `.txt` → `text/plain`. Default: `application/octet-stream`. Read files as raw bytes for correct Content-Length.
**Rationale:** Extension-based lookup is simple and covers all common static assets. Reading as bytes (not text) ensures binary files (images) are served correctly and Content-Length is accurate.
**Alternatives considered:** Java's `Files.probeContentType()` (platform-dependent, unreliable on some OSes), magic-number detection (over-engineered).
**Status:** ACTIVE
**ID:** D-11.5

## 2026-03-11 — Testing: What test strategy?

**Decision:** Three tiers: (1) **RequestParserTest** — parse valid requests, malformed requests, various methods. (2) **StaticFileHandlerTest** — file serving, 404, path traversal blocked, default document, MIME types. (3) **IntegrationTest** — start server on random port, send real HTTP requests via Socket, verify responses. Test concurrency by sending parallel requests.
**Rationale:** Parser and handler are independently testable without sockets. Integration tests verify the full stack including TCP. Concurrency test validates step 3.
**Alternatives considered:** Only integration tests (can't isolate parse failures from handler failures), using HttpClient (hides protocol details).
**Status:** ACTIVE
**ID:** D-11.6

## 2026-03-11 — Content: Article write-up

**Decision:** Write `challenges/web-server/ARTICLE.md` after implementation. Focus on HTTP/1.1 request/response format, TCP socket programming, virtual threads for concurrency, path traversal defense, and MIME type serving.
**Rationale:** Established convention. Web servers are foundational — understanding the protocol layer is broadly valuable.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-11.7
