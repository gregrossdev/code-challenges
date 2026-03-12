# Building My Own IRC Client: Real-Time Chat over Raw TCP

> An IRC client built from sockets up — RFC 2812 message parsing, bidirectional TCP with virtual threads, PING/PONG keepalive, channel management, and an interactive terminal with JLine.

## The Challenge

**Source:** [Coding Challenges - Build Your Own IRC Client](https://codingchallenges.fyi/challenges/challenge-irc)

IRC (Internet Relay Chat) is one of the oldest real-time communication protocols still in active use. It predates the web, yet its design — text-based, line-oriented, built on raw TCP — makes it an excellent protocol to implement from scratch.

The challenge walks through building a client step by step: connecting to a server, handling the registration handshake, responding to keepalive pings, joining and leaving channels, sending and receiving messages, changing nicknames, and disconnecting gracefully.

What makes this interesting is the bidirectional nature of the problem. Unlike HTTP where the client sends a request and waits for a response, IRC requires the client to simultaneously listen for server messages and accept user input. This pushes you into concurrent I/O territory.

## Usage

```bash
gig-irc -n <nick> [-s <server>] [-p <port>]
```

| Flag | Default | Description |
|------|---------|-------------|
| `-n`, `--nick` | (required) | Nickname to use on the server |
| `-s`, `--server` | `irc.libera.chat` | IRC server hostname |
| `-p`, `--port` | `6667` | Server port |

```bash
# Connect to Libera.Chat
gig-irc -n mynick

# Connect to a custom server
gig-irc -n mynick -s irc.example.com -p 6667
```

**In-client commands:**

| Command | Description |
|---------|-------------|
| `/join #channel` | Join a channel |
| `/part [message]` | Leave current channel |
| `/nick NewName` | Change nickname |
| `/msg <target> <text>` | Send a private message |
| `/quit [message]` | Disconnect from server |
| *(plain text)* | Send message to current channel |

Disconnect with `/quit` or `Ctrl+D`.

## Approach

Five components, each with a single responsibility:

1. **IrcMessage** — data class representing a parsed IRC protocol message (prefix, command, params)
2. **IrcParser** — stateless parser: raw line in, IrcMessage out. Also formats outgoing commands.
3. **IrcConnection** — TCP socket wrapper with a virtual thread reader and synchronized writer
4. **IrcClient** — orchestrates command dispatch, incoming message display, and channel state
5. **Main** — JLine terminal with the input loop

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Concurrency model | Virtual threads | Lightweight, no external dependencies, proven pattern from prior challenges |
| Terminal library | JLine 3 | Readline-style editing, history, signal handling — same as the shell challenge |
| Default server | `irc.libera.chat` | Freenode is defunct; Libera.Chat is the community successor |
| Parser design | Stateless object | IRC parsing is pure: one line in, one message out. No state needed. |
| PING/PONG | Auto-handled in reader thread | Must respond immediately — user shouldn't need to intervene |

## IRC Protocol Parsing

IRC messages follow RFC 2812's line format: an optional `:prefix`, a command, space-separated parameters, and an optional trailing parameter prefixed with `:` that can contain spaces. Each message ends with `\r\n`.

```kotlin
object IrcParser {
    fun parse(raw: String): IrcMessage {
        var line = raw.trimEnd('\r', '\n')
        var prefix: String? = null

        if (line.startsWith(':')) {
            val spaceIdx = line.indexOf(' ')
            prefix = line.substring(1, spaceIdx)
            line = line.substring(spaceIdx + 1)
        }

        val trailingIdx = line.indexOf(" :")
        val trailing: String?
        val front: String

        if (trailingIdx != -1) {
            trailing = line.substring(trailingIdx + 2)
            front = line.substring(0, trailingIdx)
        } else {
            trailing = null
            front = line
        }

        val parts = front.split(' ').filter { it.isNotEmpty() }
        val command = parts.first()
        val params = parts.drop(1).toMutableList()
        if (trailing != null) params.add(trailing)

        return IrcMessage(prefix, command, params)
    }
}
```

The parser handles the three key parts: prefix extraction (the source of the message — server or `nick!user@host`), command identification (PRIVMSG, JOIN, numeric codes like 001), and parameter splitting with special handling for the trailing `:` parameter that allows spaces in message text.

The `IrcMessage` data class provides helper properties — `nick` extracts just the nickname from a `nick!user@host` prefix, and `trailing` returns the last parameter (typically the message body).

## Bidirectional TCP with Virtual Threads

The core challenge of an IRC client is that data flows in both directions simultaneously. The server can send messages at any time (other users chatting, PING keepalive), while the user can type commands at any time.

```kotlin
open class IrcConnection(private val host: String, private val port: Int) {
    fun startReading() {
        readerThread = Thread.startVirtualThread {
            try {
                val r = reader ?: return@startVirtualThread
                while (isConnected) {
                    val line = r.readLine() ?: break
                    val msg = IrcParser.parse(line)
                    if (msg.command == "PING") {
                        send(IrcParser.formatPong(msg.params.firstOrNull() ?: ""))
                    } else {
                        messageHandler?.invoke(msg)
                    }
                }
            } catch (_: Exception) {
            } finally {
                isConnected = false
            }
        }
    }

    @Synchronized
    open fun send(message: String) {
        writer?.print(message)
        writer?.flush()
    }
}
```

The reader thread runs continuously on a virtual thread, parsing every line from the server. PING messages get an immediate PONG response — this happens automatically without user interaction, which is critical because IRC servers will disconnect clients that don't respond to pings within a timeout window.

The `send` method is synchronized because both the reader thread (for PONG) and the main thread (for user commands) can write to the socket concurrently. Without synchronization, interleaved writes could produce malformed protocol messages.

## Channel State and Command Dispatch

The client tracks a single active channel and dispatches user input based on whether it starts with `/`:

```kotlin
fun processInput(input: String) {
    if (input.startsWith("/")) {
        val parts = input.substring(1).split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)
        when (cmd) {
            "join" -> connection.send(IrcParser.formatJoin(arg))
            "part" -> { connection.send(IrcParser.formatPart(currentChannel)); currentChannel = null }
            "nick" -> connection.send(IrcParser.formatNick(arg))
            "quit" -> { connection.send(IrcParser.formatQuit(arg)); running = false }
            else -> display("Unknown command: /$cmd")
        }
    } else {
        connection.send(IrcParser.formatPrivmsg(currentChannel, input))
    }
}
```

The prompt dynamically reflects the current state — `[#channel]>` when in a channel, `[no channel]>` when not. This gives the user immediate context about where their messages will go.

Incoming messages are formatted for readability: `<alice> hello` for chat messages, `* alice has joined #test` for join notifications. The raw protocol is never shown to the user.

## What I Learned

**Bidirectional protocols require concurrent design from the start.** Unlike request-response protocols like HTTP, IRC demands that you read and write simultaneously. The reader thread must run independently of user input — messages arrive whether or not the user is typing. Virtual threads make this trivial to implement without heavyweight threading infrastructure.

**PING/PONG is the heartbeat of a persistent connection.** IRC servers send PING at regular intervals and expect PONG within a timeout. If you miss the window, the server disconnects you. Handling this automatically in the reader thread — before dispatching to the message handler — ensures the client stays connected even when the user is idle.

**Protocol parsing is the most testable layer.** The IRC message format is well-defined and stateless — one line in, one structured message out. This made it possible to write 25 parser tests covering every message type without needing a network connection. When the parser is solid, everything built on top of it inherits that reliability.

**The display layer is a natural seam for testing.** By injecting the display callback (`(String) -> Unit`), the client can be tested without a terminal. Tests collect output into a list and assert against it. This same pattern worked for the shell challenge — separating "what to show" from "how to show it" makes the business logic fully testable.

**Text protocols are forgiving to implement but demanding to get right.** IRC's line-based format looks simple, but the trailing `:` parameter, the prefix format, numeric reply codes, and the distinction between commands and their parameters all require careful parsing. Getting the edge cases right — empty trailing parameters, messages without prefixes, server-originated vs user-originated messages — is where the real work lives.
