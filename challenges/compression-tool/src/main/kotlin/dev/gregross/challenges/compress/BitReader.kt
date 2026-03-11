package dev.gregross.challenges.compress

class BitReader(private val data: ByteArray) {
    private var byteIndex = 0
    private var bitIndex = 0

    fun readBit(): Int {
        if (byteIndex >= data.size) throw IllegalStateException("No more bits to read")
        val bit = (data[byteIndex].toInt() shr (7 - bitIndex)) and 1
        bitIndex++
        if (bitIndex == 8) {
            bitIndex = 0
            byteIndex++
        }
        return bit
    }

    fun hasMore(): Boolean = byteIndex < data.size
}
