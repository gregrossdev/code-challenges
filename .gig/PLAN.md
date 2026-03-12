# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 16 — IRC Client (v0.16.x)

> Build an IRC client that connects to an IRC server via TCP, registers with NICK/USER, handles PING/PONG keepalive, supports joining/parting channels, sending/receiving messages, nickname changes, and graceful disconnect. Deliver as `gig-irc` native binary with article write-up.

**Decisions:** D-16.1, D-16.2, D-16.3, D-16.4, D-16.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 16.1 | `0.16.1` | Module scaffold, IrcMessage & IrcParser | in-session | done |
| 16.2 | `0.16.2` | IrcConnection — TCP socket with reader/writer threads | in-session | done |
| 16.3 | `0.16.3` | IrcClient — command dispatch, display & channel tracking | in-session | done |
| 16.4 | `0.16.4` | CLI, JLine terminal & native image | in-session | done |
| 16.5 | `0.16.5` | Integration tests with mock IRC server | in-session | done |
| 16.6 | `0.16.6` | IRC client article write-up | in-session | done |

### Batch 16.1 — Module scaffold, IrcMessage & IrcParser

**Delegation:** in-session
**Decisions:** D-16.1, D-16.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:irc-client`)
- `challenges/irc-client/build.gradle.kts` (create)
- `challenges/irc-client/src/main/kotlin/dev/gregross/challenges/irc/IrcMessage.kt` (create)
- `challenges/irc-client/src/main/kotlin/dev/gregross/challenges/irc/IrcParser.kt` (create)
- `challenges/irc-client/src/test/kotlin/dev/gregross/challenges/irc/IrcParserTest.kt` (create)

**Work:**
1. Add `challenges:irc-client` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass, `imageName` to `gig-irc`. Add JLine 3.28.0 dependency.
3. Implement `IrcMessage(prefix: String?, command: String, params: List<String>)` data class. Include helper properties: `nick` (extract from prefix), `trailing` (last param).
4. Implement `IrcParser`:
   - `parse(raw: String): IrcMessage` — parse RFC 2812 format: optional `:prefix`, command, space-separated params, trailing `:param`.
   - `formatNick(nick: String): String` — `NICK <nick>\r\n`
   - `formatUser(user: String, realName: String): String` — `USER <user> 0 * :<realName>\r\n`
   - `formatJoin(channel: String): String` — `JOIN <channel>\r\n`
   - `formatPart(channel: String, message: String?): String` — `PART <channel> [:<message>]\r\n`
   - `formatPrivmsg(target: String, text: String): String` — `PRIVMSG <target> :<text>\r\n`
   - `formatPong(server: String): String` — `PONG :<server>\r\n`
   - `formatQuit(message: String?): String` — `QUIT [:<message>]\r\n`
5. Write `IrcParserTest`: parse server messages (with/without prefix), parse numeric replies, parse PRIVMSG with nick extraction, format all outgoing commands, edge cases (empty trailing, no prefix).

**Test criteria:**
- `./gradlew :challenges:irc-client:test` — all tests pass.
- Parser correctly handles `:nick!user@host PRIVMSG #channel :hello world` → prefix, command, params.
- All format methods produce valid IRC protocol strings ending with `\r\n`.

**Acceptance:** IrcMessage and IrcParser fully tested with comprehensive parse/format coverage.

### Batch 16.2 — IrcConnection — TCP socket with reader/writer threads

**Delegation:** in-session
**Decisions:** D-16.1, D-16.3
**Depends on:** Batch 16.1
**Files:**
- `challenges/irc-client/src/main/kotlin/dev/gregross/challenges/irc/IrcConnection.kt` (create)

**Work:**
1. Implement `IrcConnection(host: String, port: Int)`:
   - `connect()` — open TCP socket, create BufferedReader/PrintWriter.
   - `send(message: String)` — synchronized write to socket output stream.
   - `onMessage(handler: (IrcMessage) -> Unit)` — set callback for incoming messages.
   - `startReading()` — launch virtual thread that reads lines, parses via IrcParser, auto-responds to PING with PONG, then dispatches to message handler.
   - `disconnect()` — close socket and streams cleanly.
   - `val isConnected: Boolean` — tracks connection state.

**Test criteria:**
- Class compiles and integrates with IrcParser.
- PING/PONG auto-response logic is correct.

**Acceptance:** IrcConnection manages TCP lifecycle with virtual thread reader and synchronized writer.

### Batch 16.3 — IrcClient — command dispatch, display & channel tracking

**Delegation:** in-session
**Decisions:** D-16.1, D-16.2, D-16.4
**Depends on:** Batch 16.2
**Files:**
- `challenges/irc-client/src/main/kotlin/dev/gregross/challenges/irc/IrcClient.kt` (create)
- `challenges/irc-client/src/test/kotlin/dev/gregross/challenges/irc/IrcClientTest.kt` (create)

**Work:**
1. Implement `IrcClient(connection: IrcConnection, nick: String)`:
   - `register()` — send NICK + USER.
   - `processInput(input: String)` — dispatch user commands:
     - `/join #channel` → send JOIN, update currentChannel.
     - `/part [message]` → send PART, clear currentChannel.
     - `/nick NewName` → send NICK, update nick.
     - `/quit [message]` → send QUIT, disconnect.
     - Plain text → send PRIVMSG to currentChannel (error if no channel).
   - `handleMessage(msg: IrcMessage)` — format and display incoming messages:
     - PRIVMSG → `<nick> message`
     - JOIN → `* nick has joined #channel`
     - PART → `* nick has left #channel`
     - QUIT → `* nick has quit (message)`
     - NICK → `* oldnick is now known as newnick`
     - Numeric replies → display text content.
   - `currentChannel: String?` — tracks active channel.
   - `prompt: String` — returns `[#channel]> ` or `[no channel]> `.
   - Display output via a `displayCallback: (String) -> Unit` (injectable for testing).
2. Write `IrcClientTest`: test command parsing, display formatting, channel tracking, error when messaging with no channel.

**Test criteria:**
- `./gradlew :challenges:irc-client:test` — all tests pass.
- `/join #test` sends correct JOIN and updates channel.
- Incoming PRIVMSG displays as `<nick> message`.
- Plain text without active channel produces error message.

**Acceptance:** IrcClient handles all user commands and formats all incoming message types.

### Batch 16.4 — CLI, JLine terminal & native image

**Delegation:** in-session
**Decisions:** D-16.4
**Depends on:** Batch 16.3
**Files:**
- `challenges/irc-client/src/main/kotlin/dev/gregross/challenges/irc/Main.kt` (create)

**Work:**
1. Implement `main(args)`:
   - Parse flags: `--server`/`-s` (default `irc.libera.chat`), `--port`/`-p` (default `6667`), `--nick`/`-n` (required).
   - Create JLine terminal and LineReader.
   - Create IrcConnection, set display callback to write to terminal.
   - Connect, register, start reader thread.
   - Input loop: `lineReader.readLine(client.prompt)` → `client.processInput(line)`.
   - Handle UserInterruptException (Ctrl+C) → print newline, continue.
   - Handle EndOfFileException (Ctrl+D) → quit gracefully.
   - Shutdown hook to close terminal and disconnect.
2. Build native image: `./gradlew :challenges:irc-client:nativeCompile`.
3. Install: `./gradlew :challenges:irc-client:install`.
4. Present manual verification commands for user to test against a real IRC server.

**Test criteria:**
- Native binary builds successfully.
- `gig-irc --help` or missing `--nick` shows usage.
- Manual connection to IRC server works.

**Acceptance:** Native binary installed as `gig-irc`, user has manually verified connection to a real IRC server.

### Batch 16.5 — Integration tests with mock IRC server

**Delegation:** in-session
**Decisions:** D-16.5
**Depends on:** Batch 16.3
**Files:**
- `challenges/irc-client/src/test/kotlin/dev/gregross/challenges/irc/MockIrcServer.kt` (create)
- `challenges/irc-client/src/test/kotlin/dev/gregross/challenges/irc/IntegrationTest.kt` (create)

**Work:**
1. Implement `MockIrcServer` — minimal TCP server on random port that:
   - Accepts one client connection.
   - Reads NICK/USER and sends 001 welcome reply.
   - Responds to PING with PONG.
   - Echoes JOIN confirmation when client joins.
   - Relays PRIVMSG back as if from another user.
   - Tracks received commands for assertion.
2. Write `IntegrationTest`:
   - Registration flow: connect → NICK/USER → receive 001.
   - PING/PONG: server sends PING, verify client responds with PONG.
   - JOIN: client joins channel, verify server receives JOIN command.
   - PRIVMSG: client sends message, verify server receives PRIVMSG.
   - QUIT: client quits, verify server receives QUIT and connection closes.

**Test criteria:**
- `./gradlew :challenges:irc-client:test` — all tests pass.
- Integration tests verify full client-server communication cycle.

**Acceptance:** Mock server tests verify registration, keepalive, messaging, and disconnect.

### Batch 16.6 — IRC client article write-up

**Delegation:** in-session
**Decisions:** D-16.4
**Depends on:** Batch 16.5
**Files:**
- `challenges/irc-client/ARTICLE.md` (create)

**Work:**
1. Write article from template.
2. Include Usage section with connection examples.
3. Focus areas: IRC protocol parsing, bidirectional TCP communication, virtual thread concurrency, PING/PONG keepalive, interactive terminal UX.

**Test criteria:**
- `challenges/irc-client/ARTICLE.md` exists with all sections populated.

**Acceptance:** Complete article with Usage section.

**Phase Acceptance Criteria:**
- [ ] Connects to IRC server via TCP
- [ ] Registers with NICK/USER, receives welcome (001)
- [ ] Auto-responds to PING with PONG
- [ ] `/join #channel` joins a channel
- [ ] `/part` leaves current channel
- [ ] `/nick NewName` changes nickname
- [ ] Plain text sends PRIVMSG to current channel
- [ ] Displays incoming messages in readable format
- [ ] `/quit` disconnects gracefully
- [ ] Dynamic prompt shows current channel
- [ ] Native binary installed as `gig-irc`
- [ ] Article written at `challenges/irc-client/ARTICLE.md` with Usage section

**Completion triggers Phase 17 → version `0.17.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
