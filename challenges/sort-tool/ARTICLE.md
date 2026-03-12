# Building My Own Sort Tool: Four Sorting Algorithms in Kotlin

> Implementing merge sort, quick sort, heap sort, and radix sort from scratch — selectable at runtime via a common interface, with unique filtering and random shuffle — all in Kotlin as a Unix-like CLI tool.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Sort Tool](https://codingchallenges.fyi/challenges/challenge-sort)

Sorting is the most studied problem in computer science. Every developer uses `sort` daily, but few have implemented the algorithms themselves. This challenge asks you to build a Unix-like sort tool that reads lines from a file or stdin, sorts them lexicographically, and outputs the result — but with a twist: implement multiple sorting algorithms and let the user choose which one to use.

This is challenge #6 on Coding Challenges, and it's the most algorithmic one so far. Instead of I/O parsing or networking, the core work is implementing four distinct sorting algorithms, each with different trade-offs in time complexity, space usage, and stability.

## Approach

An interface-based design with clean separation: a **`Sorter` interface** defines the contract (`fun sort(lines: List<String>): List<String>`), four implementations provide the algorithms, and a **`SortProcessor`** orchestrates the pipeline — read lines, select algorithm, sort, optionally deduplicate, write output. The CLI parses flags and wires everything together.

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Architecture | `Sorter` interface + implementations | Each algorithm independently testable, swappable at runtime |
| Default algorithm | Merge sort | Stable, guaranteed O(n log n), predictable performance |
| Unique filtering | Post-sort sequential dedup | O(n) on sorted data, matches Unix `sort -u` behavior |
| Radix sort variant | LSD (least significant digit) | Natural fit for string sorting — process character by character |
| Random shuffle | `Collections.shuffle` | Not a "sort" per se, but challenge requires it |

## The Algorithms

### Merge Sort — The Reliable Workhorse

```kotlin
class MergeSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        if (lines.size <= 1) return lines
        val mid = lines.size / 2
        val left = sort(lines.subList(0, mid))
        val right = sort(lines.subList(mid, lines.size))
        return merge(left, right)
    }
}
```

**Time:** O(n log n) guaranteed. **Space:** O(n) for the merge buffer. **Stability:** Yes — equal elements maintain their original relative order.

Merge sort is the default because it has no worst case. Quick sort can degrade to O(n²), heap sort isn't stable, and radix sort's performance depends on string length. Merge sort just works.

The key insight is in the merge step: when two elements compare equal (`left[i] <= right[j]`), we take from the left half first. This preserves the original ordering of equal elements — that's what makes it stable.

### Quick Sort — Fast in Practice

```kotlin
class QuickSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        if (lines.size <= 1) return lines
        val pivot = lines[lines.size / 2]
        val less = lines.filter { it < pivot }
        val equal = lines.filter { it == pivot }
        val greater = lines.filter { it > pivot }
        return sort(less) + equal + sort(greater)
    }
}
```

**Time:** O(n log n) average, O(n²) worst case. **Space:** O(n) for the partitions. **Stability:** This three-way partition variant is stable.

The functional three-way partition (less/equal/greater) is clean and avoids the traditional in-place partitioning complexity. The middle-element pivot selection avoids the worst case on already-sorted input, which would hit O(n²) with a first/last element pivot.

In production sort implementations, quick sort is often preferred because its cache behavior is excellent — sequential access patterns play well with CPU caches. Our functional version trades some of that for clarity.

### Heap Sort — In-Place Guaranteed

```kotlin
class HeapSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        val arr = lines.toMutableList()
        // Build max heap, then repeatedly extract max
        for (i in arr.size / 2 - 1 downTo 0) siftDown(arr, arr.size, i)
        for (i in arr.size - 1 downTo 1) {
            swap(arr, 0, i)
            siftDown(arr, i, 0)
        }
        return arr
    }
}
```

**Time:** O(n log n) guaranteed. **Space:** O(1) extra (in-place). **Stability:** No.

Heap sort's selling point is that it's both guaranteed O(n log n) *and* in-place — merge sort needs O(n) extra space, and quick sort can degrade to O(n²). The trade-off is poor cache locality (the heap's parent-child relationships jump around in memory) and instability.

The algorithm has two phases: build a max-heap in O(n) using bottom-up sift-down, then repeatedly swap the root (maximum) to the end and restore the heap property. The `siftDown` operation is the core — it ensures the heap invariant by comparing a node with its children and swapping downward.

### Radix Sort — A Different Paradigm

```kotlin
class RadixSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        val maxLen = lines.maxOf { it.length }
        var result = lines.toList()
        for (pos in maxLen - 1 downTo 0) {
            result = countingSortByChar(result, pos)
        }
        return result
    }
}
```

**Time:** O(n × k) where k is the maximum string length. **Space:** O(n + alphabet size). **Stability:** Yes (when using stable counting sort).

Radix sort is fundamentally different from the other three — it's not comparison-based. Instead of comparing pairs of elements, it sorts by examining individual characters. LSD (least significant digit) processes from right to left, using counting sort as a stable subroutine at each position.

For strings of varying length, shorter strings need special handling. The counting sort uses 257 buckets: bucket 0 for strings that have no character at the current position (shorter strings), and buckets 1–256 for character values. This places shorter strings before longer strings with matching prefixes, which is correct lexicographic behavior.

## Algorithm Comparison

| Algorithm | Time (avg) | Time (worst) | Space | Stable | Best for |
|-----------|-----------|-------------|-------|--------|----------|
| Merge sort | O(n log n) | O(n log n) | O(n) | Yes | General purpose, stability needed |
| Quick sort | O(n log n) | O(n²) | O(n) | Yes* | Fast average case, good cache behavior |
| Heap sort | O(n log n) | O(n log n) | O(1) | No | Memory-constrained environments |
| Radix sort | O(nk) | O(nk) | O(n) | Yes | Short, fixed-length strings |

*Our three-way partition variant is stable; traditional in-place quick sort is not.

## The Pipeline

The `SortProcessor` keeps I/O concerns separate from sorting logic:

```kotlin
class SortProcessor(
    private val algorithm: String = "merge",
    private val unique: Boolean = false,
    private val randomSort: Boolean = false,
) {
    fun process(input: InputStream, output: PrintStream) {
        val lines = input.bufferedReader().readLines()
        val sorted = if (randomSort) lines.shuffled()
                     else sorters[algorithm]!!.sort(lines)
        val result = if (unique) deduplicate(sorted) else sorted
        for (line in result) output.println(line)
    }
}
```

The unique filter runs *after* sorting — it walks the sorted list and skips consecutive duplicates. This is O(n) and matches how Unix `sort -u` works. The alternative (HashSet pre-filter) would remove duplicates before sorting but would change the semantics — you'd lose information about which duplicate to keep.

## What I Learned

**Interface-based design pays off for algorithms.** Having four implementations of the same interface made testing trivial — the same test cases run against all four sorters. It also made the processor simple: just look up the algorithm by name and call `sort()`.

**Radix sort is surprisingly elegant for strings.** The LSD approach with counting sort subroutines is conceptually simple once you see it. The key insight is that stability of the subroutine sort is what makes the whole thing work — each pass preserves the ordering established by previous passes.

**Stability matters more than you'd think.** For this challenge, stability means that lines comparing equal maintain their file order. Merge sort and radix sort provide this naturally; heap sort doesn't. Quick sort depends on the implementation — our three-way partition variant happens to be stable, but the traditional in-place version isn't.

**The algorithms teach different paradigms.** Merge sort is divide-and-conquer. Quick sort is partitioning. Heap sort uses the heap data structure. Radix sort sidesteps comparison entirely. Each one is a different way of thinking about the same problem, and implementing all four makes the trade-offs concrete rather than theoretical.
