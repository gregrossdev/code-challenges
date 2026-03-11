package dev.gregross.challenges.compress

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object Encoder {

    private const val MAGIC = 0x48554646 // "HUFF"

    fun encode(input: String): ByteArray {
        require(input.isNotEmpty()) { "Cannot compress empty input" }

        val frequencies = countFrequencies(input)
        val tree = buildHuffmanTree(frequencies)
        val codeTable = generateCodeTable(tree)

        val bitWriter = BitWriter()
        for (char in input) {
            bitWriter.writeBits(codeTable[char]!!)
        }
        val paddingBits = bitWriter.getPaddingBits()
        val encodedData = bitWriter.flush()

        return buildOutput(frequencies, encodedData, input.length, paddingBits)
    }

    private fun buildOutput(
        frequencies: Map<Char, Int>,
        encodedData: ByteArray,
        originalLength: Int,
        paddingBits: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // Magic number (4 bytes)
        out.write(ByteBuffer.allocate(4).putInt(MAGIC).array())

        // Original text length (4 bytes) — for decoder to know when to stop
        out.write(ByteBuffer.allocate(4).putInt(originalLength).array())

        // Padding bits in last byte (1 byte)
        out.write(paddingBits)

        // Frequency table: count (4 bytes) + [char (2 bytes) + freq (4 bytes)]*
        out.write(ByteBuffer.allocate(4).putInt(frequencies.size).array())
        for ((char, freq) in frequencies) {
            out.write(ByteBuffer.allocate(2).putChar(char).array())
            out.write(ByteBuffer.allocate(4).putInt(freq).array())
        }

        // Encoded data
        out.write(encodedData)

        return out.toByteArray()
    }
}
