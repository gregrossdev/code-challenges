# Building My Own Diff Tool: Longest Common Subsequence in Kotlin

> A diff tool built from the LCS algorithm — dynamic programming table construction, backtracking to extract common subsequences, edit script generation, and classic diff output formatting.

## The Challenge

**Source:** [Coding Challenges - Build Your Own Diff Tool](https://codingchallenges.fyi/challenges/challenge-diff)

Every developer uses `diff` daily — in git diffs, code reviews, merge conflicts. But few have implemented the algorithm that powers it. At its core, diff is about finding what two files have in common, then reporting what changed.

The challenge walks through building a diff tool step by step: first computing the Longest Common Subsequence (LCS) between two strings, then extending it to work on lines of text, then generating a human-readable edit script showing additions and removals.

## Usage

```bash
gig-diff <original> <new>
```

```bash
# Compare two files
gig-diff original.txt modified.txt

# Exit codes follow GNU diff conventions
gig-diff file1.txt file2.txt; echo $?
# 0 = identical, 1 = differences found, 2 = error
```

## Approach

Four components, each handling one concern:

1. **LcsComputer** — dynamic programming algorithm to find the longest common subsequence
2. **DiffGenerator** — walks both files and the LCS to classify each line as common, added, or removed
3. **DiffFormatter** — renders the edit script in classic diff format (`< ` / `> ` / `---`)
4. **Main** — CLI that reads two files and prints the diff

### Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Algorithm | DP-based LCS | Challenge recommends LCS/Myers; DP is straightforward and testable |
| Output format | Classic diff (`< ` / `> ` / `---`) | Challenge specifies this format explicitly |
| Exit codes | 0/1/2 (GNU diff convention) | Standard behavior developers expect |
| Complexity | O(mn) time and space | Acceptable for challenge scope; optimizations deferred |

## The LCS Algorithm

The Longest Common Subsequence is the foundation of diff. Given two sequences, the LCS is the longest sequence of elements that appears in both, in order, but not necessarily contiguously.

For example, the LCS of `"ABCD"` and `"AC"` is `"AC"` — both A and C appear in order in both strings.

The algorithm uses dynamic programming. Build a table where `dp[i][j]` holds the LCS length of the first `i` elements of sequence A and the first `j` elements of sequence B:

```kotlin
fun compute(a: List<String>, b: List<String>): List<String> {
    val m = a.size
    val n = b.size
    val dp = Array(m + 1) { IntArray(n + 1) }

    for (i in 1..m) {
        for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1] + 1
            } else {
                maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    // Backtrack to find the LCS
    val result = mutableListOf<String>()
    var i = m
    var j = n
    while (i > 0 && j > 0) {
        when {
            a[i - 1] == b[j - 1] -> {
                result.add(a[i - 1])
                i--
                j--
            }
            dp[i - 1][j] >= dp[i][j - 1] -> i--
            else -> j--
        }
    }

    return result.reversed()
}
```

Two phases: **fill** and **backtrack**. The fill phase builds the table bottom-up. At each cell, if the elements match, the LCS length is one more than the diagonal predecessor. If they don't match, take the maximum of the cell above or to the left.

The backtrack phase starts at `dp[m][n]` and walks backward. When elements match, that element is part of the LCS — add it and move diagonally. Otherwise, move toward the larger neighbor. The result comes out in reverse order, so we reverse it at the end.

For the string `"ABCD"` vs `"AC"`, the table looks like:

```
    ""  A  C
""   0  0  0
A    0  1  1
B    0  1  1
C    0  1  2
D    0  1  2
```

Backtracking from `dp[4][2] = 2`: D≠C so move up, C=C so add C and go diagonal, B≠A so move up, A=A so add A and go diagonal. Result (reversed): `["A", "C"]`.

## Edit Script Generation

Once the LCS is known, generating the diff is a three-pointer walk through both input sequences and the common subsequence:

```kotlin
fun generate(original: List<String>, modified: List<String>): List<DiffEntry> {
    val common = lcs.compute(original, modified)
    val entries = mutableListOf<DiffEntry>()

    var oi = 0; var mi = 0; var ci = 0

    while (ci < common.size) {
        while (oi < original.size && original[oi] != common[ci]) {
            entries.add(DiffEntry.Removed(original[oi++]))
        }
        while (mi < modified.size && modified[mi] != common[ci]) {
            entries.add(DiffEntry.Added(modified[mi++]))
        }
        entries.add(DiffEntry.Common(common[ci]))
        oi++; mi++; ci++
    }

    while (oi < original.size) entries.add(DiffEntry.Removed(original[oi++]))
    while (mi < modified.size) entries.add(DiffEntry.Added(modified[mi++]))

    return entries
}
```

The logic: advance through both files in parallel, guided by the LCS. Lines in the original that aren't in the LCS were removed. Lines in the modified that aren't in the LCS were added. Lines in both (the LCS itself) are common — unchanged.

This produces a flat list of entries: `Common("first")`, `Removed("old")`, `Added("new")`, `Common("third")`, etc.

## Output Formatting

The formatter groups consecutive changes and renders them in classic diff format:

```kotlin
for (line in removed) {
    sb.appendLine("< $line")
}
if (removed.isNotEmpty() && added.isNotEmpty()) {
    sb.appendLine("---")
}
for (line in added) {
    sb.appendLine("> $line")
}
```

Each change group collects contiguous removed and added lines. If a group has both removals and additions (a modification), they're separated by `---`. Pure removals or pure additions stand alone. Common lines produce no output.

For example, changing line 2 and line 4:

```
< This is the second line.
---
> This is the second line, modified.

< This is the fourth line.
---
> This is an added line.
```

## What I Learned

**LCS is the foundation of diff, not diff itself.** The LCS tells you what stayed the same. The diff is everything else — the lines that aren't in the common subsequence. Finding what's common is the hard part; reporting what changed is just bookkeeping.

**Dynamic programming makes the impossible tractable.** A brute-force approach to LCS would be exponential — checking every possible subsequence. The DP table solves it in O(mn) by building on previously computed subproblems. Each cell depends only on its three neighbors.

**Backtracking recovers the solution from the table.** The DP table only stores lengths, not the actual subsequence. The backtracking phase reconstructs the LCS by following the decisions that led to each cell's value. This is a common DP pattern — compute the optimal value forward, reconstruct the solution backward.

**Three pointers are enough to generate an edit script.** Once you have the LCS, walking through both files and the common subsequence with three indices classifies every line. No second algorithm needed — just careful pointer management.

**Exit codes are part of the interface.** GNU diff's convention (0 = same, 1 = different, 2 = error) is used by scripts and CI pipelines. Getting the exit codes right matters as much as getting the output right — tools that wrap diff depend on these values.
