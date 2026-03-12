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

## 2026-03-12 — Algorithm: How to compute the diff?

**Decision:** Implement the Myers difference algorithm via the Longest Common Subsequence (LCS) approach. First compute LCS between two sequences of lines using dynamic programming (O(mn) table), then derive the edit script (additions/removals) from the LCS result. This matches the challenge's Step 1-3 progression: string LCS → line LCS → diff generation.
**Rationale:** The challenge explicitly recommends Myers/LCS. DP-based LCS is straightforward to implement and test incrementally. The O(mn) space/time is acceptable for the challenge scope — the optional Step 5 optimization can be deferred.
**Alternatives considered:** Myers' O(ND) greedy algorithm (more complex to implement, better for large files but overkill for the challenge), Hunt-Szymanski (mentioned by challenge, less common), patience diff (git's alternative, more complex).
**Status:** ACTIVE
**ID:** D-13.1

## 2026-03-12 — Architecture: How should the diff tool be structured?

**Decision:** Four components: (1) **LcsComputer** — computes the longest common subsequence between two lists of strings using DP. (2) **DiffGenerator** — takes two line arrays and the LCS, produces a list of DiffEntry (COMMON/ADDED/REMOVED with line content). (3) **DiffFormatter** — formats DiffEntry list for output using `< ` prefix for removed, `> ` for added, with `---` separator between change groups. (4) **Main** — CLI accepting two file paths, reads files, runs diff, prints to stdout.
**Rationale:** Clean separation: algorithm (LCS), diff logic (edit script), presentation (formatting), and CLI. Each component is independently testable. Follows the established pattern of processor + formatter + CLI.
**Alternatives considered:** Single monolithic function (untestable), Unix unified diff format (challenge specifies `<`/`>` format).
**Status:** ACTIVE
**ID:** D-13.2

## 2026-03-12 — Output format: What format for the diff output?

**Decision:** Use the challenge's specified format: `< ` prefix for lines only in the original file (removed), `> ` prefix for lines only in the new file (added). Separate change groups with `---`. Common lines are not printed (matching standard diff behavior). This matches the traditional Unix `diff` normal format.
**Rationale:** Challenge Step 3 explicitly shows this format. It's the classic `diff` output format used before unified diffs became popular.
**Alternatives considered:** Unified diff format (`+`/`-` with context lines — not what the challenge asks), side-by-side (different tool), just `<`/`>` without separators (less readable).
**Status:** ACTIVE
**ID:** D-13.3

## 2026-03-12 — CLI: What interface and flags?

**Decision:** CLI accepts exactly two positional arguments: `gig-diff <original> <new>`. No flags needed for the core challenge. Output goes to stdout. Exit code 0 if files are identical, 1 if differences found, 2 on error (matching GNU diff convention).
**Rationale:** Challenge Step 4 shows `ccdiff origcc.txt newcc.txt` — two positional args, output to stdout. GNU diff exit codes are standard and useful for scripting.
**Alternatives considered:** Adding `-u` for unified format (out of scope), `-q` for quiet mode (unnecessary complexity).
**Status:** ACTIVE
**ID:** D-13.4

## 2026-03-12 — Testing: What test strategy?

**Decision:** Three tiers: (1) **LcsComputerTest** — string-level LCS with the challenge's exact test cases (identical, disjoint, partial overlap, empty, subsequence), plus line-level LCS. (2) **DiffGeneratorTest** — identical files (no diff), completely different files, insertions, deletions, modifications, mixed changes. (3) **IntegrationTest** — end-to-end with test fixture files, verifying exact output format and exit codes. Use the challenge's provided test files (origcc.txt, newcc.txt) as fixtures.
**Rationale:** The challenge provides explicit LCS test cases for Step 1 — use them directly. Generator tests verify the edit script logic. Integration tests verify the full pipeline including file I/O and formatting.
**Status:** ACTIVE
**ID:** D-13.5

## 2026-03-12 — Content: Article write-up

**Decision:** Write `challenges/diff/ARTICLE.md` after implementation. Include Usage section per template. Focus on LCS algorithm (DP table construction and backtracking), edit script generation from LCS, the classic diff output format, and time/space complexity analysis.
**Rationale:** Established convention. LCS is a well-known CS algorithm worth explaining in depth.
**Status:** ACTIVE
**ID:** D-13.6
