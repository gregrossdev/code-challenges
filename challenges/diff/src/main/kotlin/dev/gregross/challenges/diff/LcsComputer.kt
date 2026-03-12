package dev.gregross.challenges.diff

class LcsComputer {

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

    fun computeString(a: String, b: String): String {
        val aChars = a.map { it.toString() }
        val bChars = b.map { it.toString() }
        return compute(aChars, bChars).joinToString("")
    }
}
