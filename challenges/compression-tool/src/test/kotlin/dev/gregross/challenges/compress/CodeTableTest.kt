package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeTableTest {

    @Test fun `generates codes for two characters`() {
        val tree = buildHuffmanTree(mapOf('a' to 3, 'b' to 1))
        val codes = generateCodeTable(tree)
        assertEquals(2, codes.size)
        assertTrue(codes.containsKey('a'))
        assertTrue(codes.containsKey('b'))
    }

    @Test fun `codes are prefix-free`() {
        val frequencies = mapOf('a' to 10, 'b' to 5, 'c' to 3, 'd' to 1, 'e' to 7)
        val tree = buildHuffmanTree(frequencies)
        val codes = generateCodeTable(tree)
        val codeValues = codes.values.toList()
        for (i in codeValues.indices) {
            for (j in codeValues.indices) {
                if (i != j) {
                    assertTrue(!codeValues[j].startsWith(codeValues[i]),
                        "Code '${codeValues[i]}' is a prefix of '${codeValues[j]}'")
                }
            }
        }
    }

    @Test fun `codes only contain 0 and 1`() {
        val tree = buildHuffmanTree(mapOf('a' to 5, 'b' to 3, 'c' to 1))
        val codes = generateCodeTable(tree)
        for ((_, code) in codes) {
            assertTrue(code.all { it == '0' || it == '1' },
                "Code '$code' contains non-binary characters")
        }
    }

    @Test fun `higher frequency gets shorter or equal code`() {
        val tree = buildHuffmanTree(mapOf('a' to 100, 'b' to 1, 'c' to 1, 'd' to 1))
        val codes = generateCodeTable(tree)
        assertTrue(codes['a']!!.length <= codes['b']!!.length,
            "Expected 'a' code (${codes['a']}) to be shorter than 'b' code (${codes['b']})")
    }

    @Test fun `single character gets a code`() {
        val tree = buildHuffmanTree(mapOf('x' to 5))
        val codes = generateCodeTable(tree)
        assertEquals(1, codes.size)
        assertTrue(codes['x']!!.length == 1, "Single char should get 1-bit code")
    }
}
