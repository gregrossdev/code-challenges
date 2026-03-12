# Building My Own Compression Tool: Huffman Coding in Kotlin

> Building a Huffman encoder and decoder from scratch — character frequency analysis, binary tree construction, prefix-free code generation, bit packing, and a self-contained binary file format — all in Kotlin with no external dependencies.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Compression Tool](https://codingchallenges.fyi/challenges/challenge-huffman)

Compression is one of those things you use every day without thinking about it. ZIP files, PNG images, HTTP responses — they all rely on compression algorithms. Huffman coding is one of the foundational algorithms: it assigns shorter bit codes to more frequent characters and longer codes to rarer ones, producing a lossless compressed representation.

The challenge walks you through building a complete compression tool: analyze character frequencies, build a Huffman tree, generate prefix-free codes, pack bits into bytes, write a self-contained file format, then reverse the whole process for decompression. The test file is Les Miserables by Victor Hugo — 3.3 MB of text that compresses nicely.

This is challenge #3 on Coding Challenges, and it's a step up from the JSON parser. Instead of text processing, you're working with binary data, bit-level operations, tree data structures, and the greedy algorithm behind Huffman coding.

## Usage

```bash
gig-compress [-d] <input> -o <output>
```

| Flag | Description |
|------|-------------|
| `-d` | Decompress mode (compress by default) |
| `-o` | Output file (required) |

```bash
gig-compress input.txt -o compressed.huff
gig-compress -d compressed.huff -o recovered.txt
```

## Approach

The architecture mirrors how real compression tools work: a pipeline of discrete transformations. Each stage has a single responsibility and can be tested independently. The key insight is that encoding and decoding are mirror images — encoding builds a tree from frequencies and traverses it to generate codes, while decoding rebuilds the same tree from the stored frequency table and walks it to decode bits back to characters.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Architecture | Four-phase pipeline: FrequencyCounter → HuffmanTree → CodeTable → BitWriter/BitReader | Each phase testable independently, mirrors the challenge's step structure |
| Data model | Sealed `HuffmanNode` (Leaf/Branch) | Exhaustive `when` matching, textbook Huffman representation |
| File format | Magic number + frequency table + bit stream | Frequency table is simpler to serialize than the tree itself |
| Bit codes | `Map<Char, String>` during construction | String bit patterns are readable and debuggable; packed to real bits only at write time |
| Padding | Store padding bit count + original text length | Decoder knows exactly when to stop, no ambiguity from partial final byte |

## Build Log

### Step 1 — Character Frequency Analysis

The simplest step: iterate through the input string and count each character. A `Map<Char, Int>` is all you need. The challenge provides checkpoints: 'X' appears 333 times and 't' appears roughly 223,000 times in Les Miserables. These made perfect test assertions.

### Step 2 — Building the Huffman Tree

The core algorithm. Start by creating a leaf node for each unique character with its frequency. Put them all in a priority queue (min-heap) ordered by frequency. Then repeatedly pull the two lowest-frequency nodes, combine them into a branch node with their summed frequency, and push it back. When one node remains, that's the root.

For example, with frequencies `a=5, b=2, c=1`:
1. Queue: `[c:1, b:2, a:5]`
2. Merge c+b → branch(3). Queue: `[branch:3, a:5]`
3. Merge branch+a → root(8)

The tree naturally puts high-frequency characters near the root (shorter codes) and low-frequency ones deeper (longer codes).

### Step 3 — Prefix-Free Code Generation

Traverse the tree: go left = append "0", go right = append "1". When you hit a leaf, the accumulated string is that character's code. Because the tree is a proper binary tree, no code is a prefix of another — this is what makes Huffman codes unambiguously decodable without delimiters.

### Step 4 — Bit Packing and File Format

This was the trickiest part. Variable-length bit codes need to be packed into fixed-size bytes. The `BitWriter` accumulates bits into a byte buffer, flushing each complete byte. The last byte gets zero-padded, and the padding count is stored in the header so the decoder knows how many trailing bits to ignore.

The file format: `[HUFF magic: 4 bytes][original length: 4 bytes][padding bits: 1 byte][freq table count: 4 bytes][char+freq pairs][encoded bit stream]`.

### Step 5 — Decoding

The decoder reads the header, rebuilds the frequency table, constructs the same Huffman tree, then walks the tree bit by bit. Each time it hits a leaf, it outputs that character and restarts from the root. The original length field tells it exactly how many characters to decode.

## Key Code

### Sealed interface for the Huffman tree

```kotlin
sealed interface HuffmanNode {
    val frequency: Int

    data class Leaf(val char: Char, override val frequency: Int) : HuffmanNode
    data class Branch(val left: HuffmanNode, val right: HuffmanNode, override val frequency: Int) : HuffmanNode
}
```

Two cases, exhaustive matching, frequency on both — the priority queue just needs `node.frequency` without caring about the type.

### Tree construction with a priority queue

```kotlin
fun buildHuffmanTree(frequencies: Map<Char, Int>): HuffmanNode {
    val queue = PriorityQueue<HuffmanNode>(compareBy { it.frequency })
    for ((char, freq) in frequencies) {
        queue.add(HuffmanNode.Leaf(char, freq))
    }

    while (queue.size > 1) {
        val left = queue.poll()
        val right = queue.poll()
        queue.add(HuffmanNode.Branch(left, right, left.frequency + right.frequency))
    }

    return queue.poll()
}
```

The entire Huffman algorithm in 10 lines. The priority queue handles the greedy selection automatically — always merging the two least frequent nodes.

### Bit-level decoding

```kotlin
repeat(originalLength) {
    var node: HuffmanNode = tree
    while (node is HuffmanNode.Branch) {
        val bit = reader.readBit()
        node = if (bit == 0) node.left else node.right
    }
    result.append((node as HuffmanNode.Leaf).char)
}
```

Walk the tree one bit at a time. When you reach a leaf, you've decoded one character. The smart cast from `HuffmanNode` to `Branch` in the while condition, and the cast to `Leaf` after the loop, is classic Kotlin pattern matching.

## Testing

Four tiers of tests covering frequency counting, tree building, bit packing, and end-to-end integration.

| Test Case | Category | Expected | Result |
|-----------|----------|----------|--------|
| Frequency of 'X' in Les Mis | Frequency | 333 | Pass |
| Frequency of 't' in Les Mis | Frequency | ~223,000 | Pass |
| Empty input frequencies | Frequency | Empty map | Pass |
| Tree root frequency = sum of all | Tree | Total frequency | Pass |
| Leaf count = unique characters | Tree | Matching count | Pass |
| Higher frequency = shorter code depth | Tree | Depth ordering | Pass |
| Codes are prefix-free | CodeTable | No code prefixes another | Pass |
| Codes contain only 0 and 1 | CodeTable | Binary only | Pass |
| Full byte packing (8 bits) | BitWriter | Correct byte value | Pass |
| Padding calculation | BitWriter | Correct padding count | Pass |
| Encoded output starts with magic | Encoder | 0x48554646 | Pass |
| Repetitive text compresses | Encoder | Smaller output | Pass |
| Round-trip simple string | Decoder | Identical output | Pass |
| Round-trip all printable ASCII | Decoder | Identical output | Pass |
| Rejects invalid magic number | Decoder | Throws exception | Pass |
| Round-trip small test file | Integration | Identical output | Pass |
| Round-trip Les Miserables | Integration | Identical output | Pass |
| Les Mis compressed is smaller | Integration | Smaller file | Pass |
| Full pipeline with freq checks | Integration | All steps verified | Pass |

Total: 38 tests across frequency (6), tree (7), code table (5), bit writer (5), encoder (5), decoder (5), and integration (5).

## What I Learned

- **Bit packing is where the bugs hide.** The logic for accumulating bits into bytes, handling the final partial byte, and tracking padding is simple in concept but easy to get wrong by one. Testing bit-level operations in isolation (before integration) caught issues early.

- **Storing the frequency table beats storing the tree.** My first instinct was to serialize the tree structure, but storing the frequency table is simpler (just char-int pairs) and the tree can be deterministically rebuilt. The decoder rebuilds the exact same tree because the priority queue produces the same result for the same frequencies.

- **Huffman coding's elegance is in the greedy property.** The algorithm is remarkably simple — just a priority queue and a loop — yet it produces provably optimal prefix-free codes. The priority queue does all the heavy lifting by always picking the two least frequent nodes.

- **File format design matters more than you'd expect.** The magic number catches wrong-file errors immediately. Storing the original text length eliminates ambiguity from padding bits. These are small decisions that make the difference between a fragile tool and a robust one.

## Running It

```bash
# Compress a file
gig-compress path/to/file.txt -o file.huff

# Decompress it
gig-compress -d file.huff -o file-restored.txt

# Verify round-trip
diff path/to/file.txt file-restored.txt

# Check compression ratio (printed on success)
gig-compress lesmiserables.txt -o lesmis.huff
# Output: Compressed: 3369045 → 1932876 bytes (42% reduction)
```

---

*Built as part of my [Coding Challenges](https://codingchallenges.fyi) series. Stack: Kotlin + Gradle.*
