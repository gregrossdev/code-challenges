# Code Challenges

Solutions to [codingchallenges.fyi](https://codingchallenges.fyi/challenges/intro) in Kotlin + GraalVM native images.

## README Sync Rule

When a new challenge is completed (phase archived / ready to push), update `README.md` before pushing:

1. Add a row to the Challenges table with the next sequential number.
2. Link the challenge directory, the original problem URL (`https://codingchallenges.fyi/challenges/challenge-{slug}`), and a short description.
3. Keep the table sorted by challenge number.
4. Stage `README.md` alongside the other changes — do not create a separate commit for it.

The challenge slug in the problem URL usually matches the directory name but may differ (e.g. `compression-tool` → `challenge-huffman`, `web-server` → `challenge-webserver`). Check the ARTICLE.md or prior table entries if unsure.
