package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationTest {

    @Test fun `round-trips small test file`() {
        val input = this::class.java.getResourceAsStream("/test.txt")!!
            .bufferedReader().readText()
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `compressed repetitive text is smaller than original`() {
        // Small files may not compress well due to header overhead,
        // so use a repetitive string that compresses well
        val input = "the quick brown fox ".repeat(100)
        val encoded = Encoder.encode(input)
        assertTrue(encoded.size < input.toByteArray().size,
            "Compressed (${encoded.size}) should be smaller than original (${input.toByteArray().size})")
    }

    @Test fun `round-trips les miserables`() {
        val input = this::class.java.getResourceAsStream("/lesmiserables.txt")!!
            .bufferedReader().readText()
        val encoded = Encoder.encode(input)
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test fun `les miserables compressed is smaller`() {
        val input = this::class.java.getResourceAsStream("/lesmiserables.txt")!!
            .bufferedReader().readText()
        val originalSize = input.toByteArray().size
        val encoded = Encoder.encode(input)
        assertTrue(encoded.size < originalSize,
            "Compressed (${encoded.size}) should be smaller than original ($originalSize)")
        val ratio = 100 - (encoded.size * 100 / originalSize)
        println("Les Miserables compression: $originalSize → ${encoded.size} bytes ($ratio% reduction)")
    }

    @Test fun `full pipeline with frequency verification`() {
        val input = this::class.java.getResourceAsStream("/lesmiserables.txt")!!
            .bufferedReader().readText()

        // Step 1: Frequency verification
        val freq = countFrequencies(input)
        assertEquals(333, freq['X'])

        // Step 2-3: Tree and codes
        val tree = buildHuffmanTree(freq)
        val codes = generateCodeTable(tree)
        assertTrue(codes.isNotEmpty())

        // Steps 4-5: Encode
        val encoded = Encoder.encode(input)

        // Steps 6-7: Decode
        val decoded = Decoder.decode(encoded)
        assertEquals(input, decoded)
    }
}
