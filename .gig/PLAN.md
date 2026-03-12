# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 6 — Sort Tool (v0.6.x)

> Build a Unix-like sort tool implementing four sorting algorithms (merge sort, quick sort, heap sort, radix sort) selectable at runtime, with unique filtering and random shuffle support. Each algorithm is independently testable via a `Sorter` interface. Deliver as `gig-sort` native binary with article write-up.

**Decisions:** D-6.1, D-6.2, D-6.3, D-6.4, D-6.5, D-6.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 6.1 | `0.6.1` | Module scaffold & Sorter interface | in-session | done |
| 6.2 | `0.6.2` | Merge sort & quick sort | in-session | done |
| 6.3 | `0.6.3` | Heap sort & radix sort | in-session | done |
| 6.4 | `0.6.4` | SortProcessor, unique filtering & CLI | in-session | done |
| 6.5 | `0.6.5` | Native image, integration tests & test data | in-session | done |
| 6.6 | `0.6.6` | Sort tool article write-up | in-session | done |

### Batch 6.1 — Module scaffold & Sorter interface

**Delegation:** in-session
**Decisions:** D-6.1, D-6.2
**Files:**
- `settings.gradle.kts` (modify — add `challenges:sort-tool`)
- `challenges/sort-tool/build.gradle.kts` (create)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/Sorter.kt` (create)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/MergeSorter.kt` (create — stub)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/QuickSorter.kt` (create — stub)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/HeapSorter.kt` (create — stub)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/RadixSorter.kt` (create — stub)
- `challenges/sort-tool/src/test/resources/words.txt` (create — test word list)

**Work:**
1. Add `challenges:sort-tool` to `settings.gradle.kts`.
2. Create `build.gradle.kts` applying convention plugin, setting mainClass and `imageName` to `gig-sort`.
3. Define `Sorter` interface with `fun sort(lines: List<String>): List<String>`.
4. Create stub implementations for all four sorters (return input unchanged for now).
5. Create `words.txt` test fixture — a list of unsorted words for testing.

**Test criteria:**
- `./gradlew :challenges:sort-tool:compileKotlin` — compiles cleanly.
- All four sorter stubs implement the interface.

**Acceptance:** Module compiles, interface defined, stubs in place.

### Batch 6.2 — Merge sort & quick sort

**Delegation:** in-session
**Decisions:** D-6.2
**Depends on:** Batch 6.1
**Files:**
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/MergeSorter.kt` (modify)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/QuickSorter.kt` (modify)
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/MergeSorterTest.kt` (create)
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/QuickSorterTest.kt` (create)

**Work:**
1. Implement `MergeSorter` — recursive divide-and-conquer, stable sort. Split list in half, recursively sort each half, merge in order.
2. Implement `QuickSorter` — partition-based sort. Choose pivot (median-of-three or last element), partition into less/equal/greater, recurse.
3. Write tests for both: empty list, single element, already sorted, reverse sorted, duplicates, random order. Verify merge sort is stable (equal elements maintain original relative order).

**Test criteria:**
- `./gradlew :challenges:sort-tool:test` — all merge sort and quick sort tests pass.
- Merge sort preserves relative order of equal elements.

**Acceptance:** Two sorting algorithms correctly sort all test cases.

### Batch 6.3 — Heap sort & radix sort

**Delegation:** in-session
**Decisions:** D-6.2
**Depends on:** Batch 6.1
**Files:**
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/HeapSorter.kt` (modify)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/RadixSorter.kt` (modify)
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/HeapSorterTest.kt` (create)
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/RadixSorterTest.kt` (create)

**Work:**
1. Implement `HeapSorter` — build max-heap, repeatedly extract max. Use array-based heap with sift-down.
2. Implement `RadixSorter` — LSD (least significant digit) radix sort on characters. Pad strings to equal length, sort character-by-character from rightmost to leftmost using counting sort as subroutine.
3. Write tests for both: same edge cases as batch 6.2. Radix sort also tested with varying-length strings.

**Test criteria:**
- `./gradlew :challenges:sort-tool:test` — all heap sort and radix sort tests pass.

**Acceptance:** All four sorting algorithms implemented and tested.

### Batch 6.4 — SortProcessor, unique filtering & CLI

**Delegation:** in-session
**Decisions:** D-6.1, D-6.3, D-6.4
**Depends on:** Batches 6.2, 6.3
**Files:**
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/SortProcessor.kt` (create)
- `challenges/sort-tool/src/main/kotlin/dev/gregross/challenges/sort/Main.kt` (create)
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/SortProcessorTest.kt` (create)

**Work:**
1. Implement `SortProcessor`: reads lines from `InputStream`, selects algorithm by name, sorts, applies unique filtering (sequential dedup on sorted list), writes to `PrintStream`.
2. Support `--random-sort` mode — shuffle lines using `java.util.Collections.shuffle`.
3. Implement `main()` with argument parsing: `gig-sort [file]`, `-u` (unique), `--algorithm <name>` (merge/quick/heap/radix, default merge), `--random-sort`. Reads from file or stdin.
4. Write processor tests: sort with default algorithm, sort with each algorithm flag, unique filtering removes consecutive duplicates, random-sort produces permutation.

**Test criteria:**
- `./gradlew :challenges:sort-tool:test` — all processor tests pass.
- CLI parses all flags correctly.

**Acceptance:** Full pipeline works: read → sort → unique → output.

### Batch 6.5 — Native image, integration tests & test data

**Delegation:** in-session
**Decisions:** D-6.5
**Depends on:** Batch 6.4
**Files:**
- `challenges/sort-tool/src/test/kotlin/dev/gregross/challenges/sort/IntegrationTest.kt` (create)

**Work:**
1. Write integration tests using `SortProcessor` end-to-end with `words.txt` test fixture.
2. Verify output matches expected sorted order for each algorithm.
3. Test `-u` removes duplicates from sorted output.
4. Test `--random-sort` produces a valid permutation (same elements, different order likely).
5. Build native image: `./gradlew :challenges:sort-tool:nativeCompile`.
6. Install: `./gradlew :challenges:sort-tool:install`.
7. Present manual verification commands.

**Test criteria:**
- `./gradlew :challenges:sort-tool:test` — all tests pass including integration.
- `gig-sort words.txt` outputs lines in sorted order.
- `echo -e "banana\napple\napple\ncherry" | gig-sort -u` outputs 3 unique lines.
- `gig-sort --algorithm quick words.txt` sorts using quicksort.

**Acceptance:** Native binary installed as `gig-sort`, user has manually verified.

### Batch 6.6 — Sort tool article write-up

**Delegation:** in-session
**Decisions:** D-6.6
**Depends on:** Batch 6.5
**Files:**
- `challenges/sort-tool/ARTICLE.md` (create)

**Work:**
1. Write article from `templates/article.md` template.
2. Focus areas: comparing sorting algorithms (time/space complexity, stability), interface-based design for swappable algorithms, radix sort for strings, unique filtering as post-processing.

**Test criteria:**
- `challenges/sort-tool/ARTICLE.md` exists with all sections populated.
- No placeholder text remains.

**Acceptance:** Complete, publishable article.

**Phase Acceptance Criteria:**
- [ ] `gig-sort [file]` reads file and outputs sorted lines
- [ ] Reads from stdin when no file argument given
- [ ] `-u` flag removes duplicate lines from sorted output
- [ ] `--algorithm merge` uses merge sort (default)
- [ ] `--algorithm quick` uses quick sort
- [ ] `--algorithm heap` uses heap sort
- [ ] `--algorithm radix` uses radix sort
- [ ] `--random-sort` outputs random permutation of lines
- [ ] All four algorithms produce correct sorted output
- [ ] Merge sort is stable
- [ ] Native binary installed as `gig-sort`
- [ ] Article written at `challenges/sort-tool/ARTICLE.md`

**Completion triggers Phase 7 → version `0.7.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
