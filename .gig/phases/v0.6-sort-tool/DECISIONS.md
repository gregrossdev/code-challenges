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

## 2026-03-11 — Architecture: How should the sort tool be structured?

**Decision:** A `Sorter` interface with multiple implementations (MergeSorter, QuickSorter, HeapSorter, RadixSorter). A `SortProcessor` orchestrates: reads lines, selects algorithm, applies unique filtering, writes output. Algorithm selection via CLI flag. Each sorter implements `fun sort(lines: List<String>): List<String>`.
**Rationale:** The challenge explicitly asks for multiple sorting algorithms selectable at runtime. An interface makes them interchangeable and independently testable. The processor handles I/O concerns separately from sorting logic.
**Alternatives considered:** Single class with algorithm as a parameter (less clean), functional approach with lambdas (harder to test individually), using stdlib `sorted()` only (doesn't meet the challenge).
**Status:** ACTIVE
**ID:** D-6.1

## 2026-03-11 — Algorithms: Which sorting algorithms to implement?

**Decision:** Four algorithms as the challenge specifies: (1) Merge sort — stable, O(n log n) guaranteed, (2) Quick sort — fast in practice, O(n log n) average, (3) Heap sort — O(n log n) in-place, (4) Radix sort — O(nk) for string sorting. Default algorithm is merge sort (stable, predictable).
**Rationale:** The challenge lists all four. Merge sort as default because it's stable (preserves relative order of equal elements) and has guaranteed O(n log n). Each algorithm teaches different concepts — divide-and-conquer, partitioning, heap property, digit-by-digit sorting.
**Alternatives considered:** Only implementing two (doesn't fully meet challenge), adding bubble/insertion sort (too trivial, not requested).
**Status:** ACTIVE
**ID:** D-6.2

## 2026-03-11 — CLI: What flags should gig-sort support?

**Decision:** `gig-sort [file]` for basic lexicographic sort, `-u` for unique output, `--algorithm <name>` to select sorting algorithm (merge, quick, heap, radix), `--random-sort` for random permutation. Read from file argument or stdin. Output to stdout.
**Rationale:** Matches the challenge steps: basic sort (Step 1), `-u` (Step 2), algorithm selection (Step 3), `--random-sort` (Step 4). The `--algorithm` flag is cleaner than individual flags per algorithm. Stdin support follows established CLI patterns from prior challenges.
**Alternatives considered:** Separate flags per algorithm like `--merge-sort` (more flags to parse), no algorithm selection (doesn't meet Step 3).
**Status:** ACTIVE
**ID:** D-6.3

## 2026-03-11 — Unique: How should -u be implemented?

**Decision:** Apply unique filtering after sorting. Use a simple sequential dedup — iterate sorted output and skip consecutive duplicates. This is O(n) post-sort and matches how Unix `sort -u` works.
**Rationale:** The challenge asks to consider timing (during, before, after). Post-sort dedup is simplest, works with any algorithm, and is O(n) since the list is already sorted. Pre-sort dedup (HashSet) would change ordering semantics. During-sort dedup adds complexity to each algorithm.
**Alternatives considered:** HashSet pre-filter (changes ordering, wasteful for already-sorted), during-sort integration (couples dedup to each algorithm implementation).
**Status:** ACTIVE
**ID:** D-6.4

## 2026-03-11 — Testing: What test strategy for the sort tool?

**Decision:** Three tiers: (1) Unit tests for each sorting algorithm independently — verify correct ordering, stability, edge cases (empty, single element, already sorted, reverse sorted, duplicates). (2) Unit tests for unique filtering. (3) Integration tests with a test word list — verify output matches `sort` and `sort -u` expected results.
**Rationale:** Each algorithm needs independent verification since they have different failure modes. Integration tests with real word lists catch edge cases in line reading/writing. Testing stability is important for merge sort.
**Alternatives considered:** Only integration tests (can't tell which algorithm failed), property-based testing only (misses specific edge cases).
**Status:** ACTIVE
**ID:** D-6.5

## 2026-03-11 — Content: Article write-up for sort tool challenge

**Decision:** Write `challenges/sort-tool/ARTICLE.md` after implementation. Focus on comparing sorting algorithms — time complexity, space complexity, stability, and practical performance differences on the test data.
**Rationale:** Established convention. The educational value here is comparing algorithms side-by-side, which is a classic CS topic.
**Alternatives considered:** N/A — following established convention.
**Status:** ACTIVE
**ID:** D-6.6
