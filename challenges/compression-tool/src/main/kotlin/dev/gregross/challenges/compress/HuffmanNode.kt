package dev.gregross.challenges.compress

sealed interface HuffmanNode {
    val frequency: Int

    data class Leaf(val char: Char, override val frequency: Int) : HuffmanNode
    data class Branch(val left: HuffmanNode, val right: HuffmanNode, override val frequency: Int) : HuffmanNode
}
