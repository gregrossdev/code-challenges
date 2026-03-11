package dev.gregross.challenges.compress

import java.io.ByteArrayOutputStream

class BitWriter {
    private val output = ByteArrayOutputStream()
    private var currentByte = 0
    private var bitPosition = 0

    fun writeBit(bit: Int) {
        currentByte = (currentByte shl 1) or (bit and 1)
        bitPosition++
        if (bitPosition == 8) {
            output.write(currentByte)
            currentByte = 0
            bitPosition = 0
        }
    }

    fun writeBits(bits: String) {
        for (c in bits) {
            writeBit(if (c == '1') 1 else 0)
        }
    }

    fun flush(): ByteArray {
        val paddingBits = if (bitPosition > 0) 8 - bitPosition else 0
        repeat(paddingBits) { writeBit(0) }
        return output.toByteArray()
    }

    fun getPaddingBits(): Int {
        return if (bitPosition > 0) 8 - bitPosition else 0
    }
}
