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

## 2026-03-12 — Architecture: How should the IRC client be structured?

**Decision:** Five components: (1) **IrcMessage** — data class for parsed protocol messages (prefix, command, params). (2) **IrcParser** — parse raw IRC lines into IrcMessage, format outgoing commands. (3) **IrcConnection** — TCP socket wrapper with reader/writer threads (virtual threads), PING/PONG handling, message callbacks. (4) **IrcClient** — orchestrates connection + user command dispatch + display formatting. (5) **Main** — JLine terminal with input loop.
**Rationale:** Separating parsing from connection from client logic keeps each testable. IrcParser is pure functions (highly testable). IrcConnection manages the socket lifecycle. IrcClient ties everything together. JLine in Main follows the shell challenge pattern.
**Alternatives considered:** Single monolithic client class (harder to test parsing independently), event-driven architecture with listeners (over-engineered for a CLI client).
**Status:** ACTIVE
**ID:** D-16.1

## 2026-03-12 — Protocol: What IRC commands and features to implement?

**Decision:** Implement the full challenge scope: (1) NICK/USER registration on connect. (2) PING/PONG keepalive (automatic). (3) JOIN/PART channels via `/join #channel` and `/part`. (4) NICK change via `/nick NewName`. (5) PRIVMSG for channel messaging (text typed without `/` prefix sends to current channel). (6) QUIT via `/quit [message]`. Parse incoming server messages including numeric replies (001-004 welcome, MOTD, NAMES, errors).
**Rationale:** Matches challenge Steps 1-6. These are the core IRC commands per RFC 2812. PING/PONG must be automatic to maintain connection.
**Alternatives considered:** Supporting DMs/private messages (out of challenge scope), supporting multiple simultaneous channels (adds complexity, single active channel is sufficient).
**Status:** ACTIVE
**ID:** D-16.2

## 2026-03-12 — Networking: How to handle bidirectional communication?

**Decision:** Two virtual threads: (1) **Reader thread** — continuously reads lines from server socket, parses via IrcParser, dispatches to IrcClient for display/handling. (2) **Writer** — IrcConnection exposes a `send(String)` method that writes to the socket's output stream (synchronized). Main thread handles user input via JLine. PING/PONG handled automatically in the reader thread — when a PING is received, immediately send PONG without user interaction.
**Rationale:** Virtual threads are lightweight (established pattern from redis-server/load-balancer). Reader must be separate from user input to receive messages while user is idle. Writer synchronization prevents interleaved writes.
**Alternatives considered:** Single-threaded with non-blocking I/O (more complex, less readable), coroutines (adds kotlinx.coroutines dependency unnecessarily).
**Status:** ACTIVE
**ID:** D-16.3

## 2026-03-12 — CLI: What UX and connection parameters?

**Decision:** Usage: `gig-irc [OPTIONS]`. Flags: `--server` / `-s` (default `irc.libera.chat`), `--port` / `-p` (default `6667`), `--nick` / `-n` (required). Binary name: `gig-irc`. JLine terminal with prompt showing current channel (e.g., `[#channel]> ` or `[no channel]> `). User commands prefixed with `/`. Plain text sends PRIVMSG to current channel.
**Rationale:** freenode is defunct; Libera.Chat is the community successor. Nick is required for IRC registration. Dynamic prompt showing current channel provides context. JLine gives readline-style editing and history.
**Alternatives considered:** Hardcoded server (inflexible), positional args for server/nick (flags are clearer), ncurses-style split UI (massive over-engineering).
**Status:** ACTIVE
**ID:** D-16.4

## 2026-03-12 — Testing: What test strategy?

**Decision:** Three tiers: (1) **IrcParserTest** — parse incoming messages (prefix extraction, command, params, trailing), format outgoing commands. Pure logic, no I/O. (2) **IrcClientTest** — test command dispatch, display formatting, channel tracking using mock connection. (3) **IntegrationTest** — spin up a minimal mock IRC server on localhost, connect client, verify registration flow and PING/PONG. No tests against real IRC servers.
**Rationale:** Parser is the most critical and testable component. Client logic can be tested with a mock connection interface. Integration tests with a local mock server verify the full flow without depending on external infrastructure.
**Alternatives considered:** Testing against real Libera.Chat (flaky, rate-limited, network-dependent), skipping integration tests (misses connection lifecycle bugs).
**Status:** ACTIVE
**ID:** D-16.5
