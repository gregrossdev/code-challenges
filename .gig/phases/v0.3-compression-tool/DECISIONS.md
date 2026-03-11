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

## 2026-03-11 — Architecture: How should the compression tool be structured?

**Decision:** Four-phase pipeline: (1) `FrequencyCounter` — character frequency analysis, (2) `HuffmanTree` — builds the binary tree from frequencies using a priority queue, (3) `CodeTable` — traverses the tree to generate prefix-free bit codes, (4) `BitWriter`/`BitReader` — handles bit-level packing/unpacking for binary I/O. Each phase is a separate class.
**Rationale:** The challenge is explicitly structured as a pipeline (Steps 1→5 for encoding, Steps 6→7 for decoding). Separating concerns makes each phase independently testable — same principle that worked well in the JSON parser. The bit-level I/O is its own concern because packing variable-length codes into fixed-size bytes is tricky and error-prone.
**Alternatives considered:** Single monolithic encoder/decoder (harder to test, harder to debug), streaming approach (more complex, not needed for this scope).
**Status:** ACTIVE
**ID:** D-3.1

## 2026-03-11 — Data Model: How should the Huffman tree be represented?

**Decision:** Sealed interface `HuffmanNode` with two subtypes: `Leaf(char: Char, frequency: Int)` and `Branch(left: HuffmanNode, right: HuffmanNode, frequency: Int)`. Both carry frequency for priority queue ordering. The code table is a `Map<Char, String>` where the String is a bit pattern like `"0110"`.
**Rationale:** Sealed interface continues the project's established pattern (Token, JsonValue). Two subtypes match the textbook Huffman tree definition exactly. Representing bit codes as strings during construction is simple and debuggable; they're only packed into actual bits during file writing.
**Alternatives considered:** Single node class with nullable children (loses type safety), generic tree with decorator (over-engineered), `BitSet` for codes during construction (premature optimization, harder to debug).
**Status:** ACTIVE
**ID:** D-3.2

## 2026-03-11 — File Format: How should the compressed file be structured?

**Decision:** Binary file with three sections: (1) a 4-byte magic number `0x48554646` ("HUFF"), (2) a serialized frequency table as `[count: Int][char: Char, freq: Int]*`, (3) the Huffman-encoded bit stream with a trailing bit count in the final byte header to handle padding. The frequency table is stored rather than the tree because it's simpler to serialize and the tree can be deterministically rebuilt.
**Rationale:** Magic number identifies the file type and catches corruption/wrong-file errors early. Frequency table serialization is straightforward (write count, then char-freq pairs) versus tree serialization which requires a more complex encoding scheme. Storing the original byte count before the bit stream lets the decoder know exactly when to stop, avoiding ambiguity from padding bits in the last byte.
**Alternatives considered:** Serializing the tree directly (more complex encoding, canonical traversal needed), storing the code table (larger, redundant), no header/magic (fragile, no validation).
**Status:** ACTIVE
**ID:** D-3.3

## 2026-03-11 — CLI: What should gig-compress do and how should it behave?

**Decision:** `gig-compress` with two modes: `gig-compress <input> -o <output>` for compression (default), `gig-compress -d <input> -o <output>` for decompression. Exit code 0 on success, 1 on error. Print compression ratio on success (e.g., "Compressed: 342190 → 198732 bytes (42% reduction)"). Error messages to stderr.
**Rationale:** Mirrors standard compression tool UX (gzip uses `-d` for decompress). The `-o` flag for output avoids overwriting the input file accidentally. Compression ratio output is useful feedback and validates the tool is actually compressing. Follows established CLI patterns from gig-wc and gig-json.
**Alternatives considered:** Separate binaries for compress/decompress (unnecessary split), in-place compression like gzip (riskier, less beginner-friendly), stdout output (binary on stdout is problematic).
**Status:** ACTIVE
**ID:** D-3.4

## 2026-03-11 — Testing: What test strategy for the compression tool?

**Decision:** Four tiers: (1) Unit tests for frequency counting, (2) Unit tests for tree building and code generation (using known examples from the challenge — 'X' appears 333 times, 't' appears 223,000 times in the test file), (3) Unit tests for bit packing/unpacking round-trips, (4) Integration test: compress → decompress → diff against original for both a small test string and the Les Miserables test file. The Les Miserables file is downloaded and stored in `src/test/resources/`.
**Rationale:** The challenge provides specific frequency checkpoints (333 X's, 223K t's) which are perfect test assertions. Bit packing is the trickiest part and needs isolated testing. The round-trip integration test is the ultimate validation — if compress→decompress produces the original, everything works.
**Alternatives considered:** Only integration tests (harder to debug which phase failed), skipping the large file test (misses real-world validation).
**Status:** ACTIVE
**ID:** D-3.5

## 2026-03-11 — Content: Article write-up for compression challenge

**Decision:** Write `challenges/compression-tool/ARTICLE.md` after implementation using the established template. Focus on Huffman algorithm visualization (tree building step-by-step), bit packing challenges, file format design decisions, and compression ratio analysis.
**Rationale:** Established convention from prior phases. The Huffman algorithm is visual and educational — the article should walk through a small example tree construction to make it accessible.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-3.6
