# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 3 — Compression Tool (v0.3.x)

> Build a Huffman compression tool from scratch — character frequency analysis, binary tree construction, prefix-free code generation, bit-level packing, and a self-contained binary file format — all in Kotlin with no external dependencies. Deliver as `gig-compress` native binary with article write-up.

**Decisions:** D-3.1, D-3.2, D-3.3, D-3.4, D-3.5, D-3.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 3.1 | `0.3.1` | Module scaffold, data model & test file | in-session | pending |
| 3.2 | `0.3.2` | Frequency counter & Huffman tree | in-session | pending |
| 3.3 | `0.3.3` | Code table, bit packing & encoder | in-session | pending |
| 3.4 | `0.3.4` | Decoder, CLI & integration tests | in-session | pending |
| 3.5 | `0.3.5` | Native image & manual verification | in-session | pending |
| 3.6 | `0.3.6` | Compression tool article write-up | in-session | pending |

### Batch 3.1 — Module scaffold, data model & test file

**Delegation:** in-session
**Decisions:** D-3.2, D-3.5
**Files:**
- `settings.gradle.kts` (modify — add `challenges:compression-tool`)
- `challenges/compression-tool/build.gradle.kts` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/HuffmanNode.kt` (create)
- `challenges/compression-tool/src/test/resources/test.txt` (create — small test text)
- `challenges/compression-tool/src/test/resources/lesmiserables.txt` (create — downloaded from challenge)

**Work:**
1. Add `challenges:compression-tool` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-compress`.
3. Define `HuffmanNode` sealed interface with `Leaf(char, frequency)` and `Branch(left, right, frequency)` subtypes per D-3.2.
4. Create a small test text file (a few paragraphs) for quick unit tests.
5. Download the Les Miserables test file for integration tests.

**Test criteria:**
- `./gradlew :challenges:compression-tool:build` compiles.
- Test files exist in correct directories.

**Acceptance:** Module compiles, data model defined, test files ready.

### Batch 3.2 — Frequency counter & Huffman tree

**Delegation:** in-session
**Decisions:** D-3.1, D-3.2
**Depends on:** Batch 3.1
**Files:**
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/FrequencyCounter.kt` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/HuffmanTree.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/FrequencyCounterTest.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/HuffmanTreeTest.kt` (create)

**Work:**
1. Implement `FrequencyCounter` that takes a `String` and returns `Map<Char, Int>`.
2. Implement `HuffmanTree` that takes a frequency map and builds the tree using a `PriorityQueue`.
3. Write frequency counter tests: known string frequencies, empty input, single character.
4. Write tree tests: verify tree structure for known inputs, verify leaf count matches unique characters, verify higher-frequency chars are closer to root.
5. Test against Les Miserables checkpoint: 'X' appears 333 times, 't' appears 223,000 times.

**Test criteria:**
- `./gradlew :challenges:compression-tool:test` — all frequency and tree tests pass.
- Frequency of 'X' in Les Miserables = 333.

**Acceptance:** Frequency counting and tree building work correctly with verified checkpoints.

### Batch 3.3 — Code table, bit packing & encoder

**Delegation:** in-session
**Decisions:** D-3.1, D-3.2, D-3.3
**Depends on:** Batch 3.2
**Files:**
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/CodeTable.kt` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/BitWriter.kt` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/Encoder.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/CodeTableTest.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/BitWriterTest.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/EncoderTest.kt` (create)

**Work:**
1. Implement `CodeTable` — traverse `HuffmanNode` tree to generate `Map<Char, String>` of prefix-free codes.
2. Verify codes are prefix-free (no code is a prefix of another).
3. Implement `BitWriter` — packs bit strings into `ByteArray`, tracks padding.
4. Implement `Encoder` — orchestrates the full pipeline: frequency → tree → codes → header + bit stream → `ByteArray`.
5. File format per D-3.3: magic number, frequency table, original size, encoded bit stream.
6. Write tests for code generation (prefix-free property), bit packing round-trips, and encoder output.

**Test criteria:**
- `./gradlew :challenges:compression-tool:test` — all code table, bit writer, and encoder tests pass.
- Generated codes are prefix-free for any input.
- Bit packing round-trips correctly (pack then unpack = original bits).

**Acceptance:** Encoding pipeline produces valid compressed output with correct file format.

### Batch 3.4 — Decoder, CLI & integration tests

**Delegation:** in-session
**Decisions:** D-3.1, D-3.3, D-3.4, D-3.5
**Depends on:** Batch 3.3
**Files:**
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/BitReader.kt` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/Decoder.kt` (create)
- `challenges/compression-tool/src/main/kotlin/dev/gregross/challenges/compress/Main.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/DecoderTest.kt` (create)
- `challenges/compression-tool/src/test/kotlin/dev/gregross/challenges/compress/IntegrationTest.kt` (create)

**Work:**
1. Implement `BitReader` — reads bits from `ByteArray`, handles padding.
2. Implement `Decoder` — reads header (magic, frequency table), rebuilds tree, decodes bit stream to original text.
3. Implement `main()` per D-3.4: compress mode (default), decompress mode (`-d`), `-o` for output file, compression ratio output.
4. Write decoder tests: small string round-trips, header parsing.
5. Write integration tests: compress → decompress → compare for small text and Les Miserables. Verify compressed size < original size.

**Test criteria:**
- `./gradlew :challenges:compression-tool:test` — all tests pass.
- Round-trip: compress then decompress produces byte-identical output.
- Les Miserables compressed file is smaller than original.
- CLI exits 0 on success, 1 on error.

**Acceptance:** Full compress/decompress round-trip works. CLI handles files, errors, and reports compression ratio.

### Batch 3.5 — Native image & manual verification

**Delegation:** in-session
**Decisions:** D-3.4
**Depends on:** Batch 3.4
**Files:**
- (no new files — build and install)

**Work:**
1. Build native image: `./gradlew :challenges:compression-tool:nativeCompile`.
2. Install: `./gradlew :challenges:compression-tool:install`.
3. Present manual verification commands for user to run.

**Test criteria:**
- `gig-compress test.txt -o test.huff` → exit 0, prints compression ratio.
- `gig-compress -d test.huff -o test-out.txt` → exit 0.
- `diff test.txt test-out.txt` → no differences.

**Acceptance:** Native binary installed as `gig-compress`, user has manually verified.

### Batch 3.6 — Compression tool article write-up

**Delegation:** in-session
**Decisions:** D-3.6
**Depends on:** Batch 3.5
**Files:**
- `challenges/compression-tool/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template using gig artifacts.
2. Focus areas: Huffman algorithm walkthrough (small example tree), priority queue for tree building, prefix-free code property, bit packing challenges, file format design, compression ratio analysis.

**Test criteria:**
- `challenges/compression-tool/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-compress` compresses text files using Huffman coding
- [ ] `gig-compress -d` decompresses back to identical original
- [ ] Compressed output is smaller than input for typical text
- [ ] File format includes magic number, frequency table, and bit stream
- [ ] Compression ratio printed on success
- [ ] Exit code 0 for success, 1 for error
- [ ] Round-trip verified with Les Miserables test file (byte-identical)
- [ ] Frequency checkpoints verified (X=333, t≈223000)
- [ ] Native binary installed as `gig-compress`
- [ ] Article written at `challenges/compression-tool/ARTICLE.md`

**Completion triggers Phase 4 → version `0.4.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
