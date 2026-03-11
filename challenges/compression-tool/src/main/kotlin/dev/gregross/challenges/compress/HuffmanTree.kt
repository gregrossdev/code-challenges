package dev.gregross.challenges.compress

import java.util.PriorityQueue

fun buildHuffmanTree(frequencies: Map<Char, Int>): HuffmanNode {
    require(frequencies.isNotEmpty()) { "Cannot build Huffman tree from empty frequency map" }

    if (frequencies.size == 1) {
        val (char, freq) = frequencies.entries.first()
        return HuffmanNode.Branch(
            left = HuffmanNode.Leaf(char, freq),
            right = HuffmanNode.Leaf(char, freq),
            frequency = freq * 2,
        )
    }

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
