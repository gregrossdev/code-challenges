package dev.gregross.challenges.compress

import java.nio.ByteBuffer

object Decoder {

    private const val MAGIC = 0x48554646

    fun decode(data: ByteArray): String {
        var offset = 0

        // Read magic number
        val magic = ByteBuffer.wrap(data, offset, 4).int
        offset += 4
        require(magic == MAGIC) { "Invalid file format: expected HUFF magic number" }

        // Read original text length
        val originalLength = ByteBuffer.wrap(data, offset, 4).int
        offset += 4

        // Read padding bits
        val paddingBits = data[offset].toInt() and 0xFF
        offset += 1

        // Read frequency table
        val freqCount = ByteBuffer.wrap(data, offset, 4).int
        offset += 4

        val frequencies = mutableMapOf<Char, Int>()
        repeat(freqCount) {
            val char = ByteBuffer.wrap(data, offset, 2).char
            offset += 2
            val freq = ByteBuffer.wrap(data, offset, 4).int
            offset += 4
            frequencies[char] = freq
        }

        // Rebuild Huffman tree
        val tree = buildHuffmanTree(frequencies)
        val encodedData = data.copyOfRange(offset, data.size)

        // Decode bit stream
        val reader = BitReader(encodedData)
        val result = StringBuilder(originalLength)

        repeat(originalLength) {
            var node: HuffmanNode = tree
            while (node is HuffmanNode.Branch) {
                val bit = reader.readBit()
                node = if (bit == 0) node.left else node.right
            }
            result.append((node as HuffmanNode.Leaf).char)
        }

        return result.toString()
    }
}
