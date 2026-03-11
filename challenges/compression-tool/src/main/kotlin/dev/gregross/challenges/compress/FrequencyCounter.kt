package dev.gregross.challenges.compress

fun countFrequencies(input: String): Map<Char, Int> {
    val frequencies = mutableMapOf<Char, Int>()
    for (char in input) {
        frequencies[char] = (frequencies[char] ?: 0) + 1
    }
    return frequencies
}
