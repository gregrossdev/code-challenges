package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecoderTest {

    @Test fun `round-trips simple string`() {
        val input = "hello world"
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `round-trips single character repeated`() {
        val input = "aaaaaaaaaa"
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `round-trips all printable ASCII`() {
        val input = (32..126).map { it.toChar() }.joinToString("")
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `round-trips string with newlines`() {
        val input = "line1\nline2\nline3\n"
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `rejects invalid magic number`() {
        val badData = ByteArray(20) { 0 }
        assertFailsWith<IllegalArgumentException> {
            Decoder.decode(badData)
        }
    }
}
