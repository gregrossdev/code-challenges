package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class BitWriterTest {

    @Test fun `writes full byte`() {
        val writer = BitWriter()
        writer.writeBits("10110011")
        val result = writer.flush()
        assertEquals(1, result.size)
        assertEquals(0xB3.toByte(), result[0])
    }

    @Test fun `pads incomplete byte with zeros`() {
        val writer = BitWriter()
        writer.writeBits("101")
        assertEquals(5, writer.getPaddingBits())
        val result = writer.flush()
        assertEquals(1, result.size)
        // 101 + 00000 padding = 10100000 = 0xA0
        assertEquals(0xA0.toByte(), result[0])
    }

    @Test fun `writes multiple bytes`() {
        val writer = BitWriter()
        writer.writeBits("1111111100000000")
        val result = writer.flush()
        assertEquals(2, result.size)
        assertEquals(0xFF.toByte(), result[0])
        assertEquals(0x00.toByte(), result[1])
    }

    @Test fun `empty writer produces empty output`() {
        val writer = BitWriter()
        val result = writer.flush()
        assertEquals(0, result.size)
    }

    @Test fun `padding bits correct for various lengths`() {
        for (bits in 1..7) {
            val writer = BitWriter()
            repeat(bits) { writer.writeBit(1) }
            assertEquals(8 - bits, writer.getPaddingBits())
        }
    }
}
