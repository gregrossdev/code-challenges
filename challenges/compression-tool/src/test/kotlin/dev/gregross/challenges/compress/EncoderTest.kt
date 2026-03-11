package dev.gregross.challenges.compress

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncoderTest {

    @Test fun `encoded output starts with magic number`() {
        val encoded = Encoder.encode("hello")
        val magic = ByteBuffer.wrap(encoded, 0, 4).int
        assertEquals(0x48554646, magic)
    }

    @Test fun `encoded output contains original length`() {
        val input = "hello world"
        val encoded = Encoder.encode(input)
        val originalLength = ByteBuffer.wrap(encoded, 4, 4).int
        assertEquals(input.length, originalLength)
    }

    @Test fun `encoded output is smaller than repetitive input`() {
        val input = "a".repeat(1000) + "b".repeat(100) + "c".repeat(10)
        val encoded = Encoder.encode(input)
        assertTrue(encoded.size < input.length,
            "Expected compressed (${encoded.size}) < original (${input.length})")
    }

    @Test fun `empty input throws`() {
        assertFailsWith<IllegalArgumentException> {
            Encoder.encode("")
        }
    }

    @Test fun `encodes all printable ASCII`() {
        val input = (32..126).map { it.toChar() }.joinToString("")
        val encoded = Encoder.encode(input)
        val magic = ByteBuffer.wrap(encoded, 0, 4).int
        assertEquals(0x48554646, magic)
    }
}
