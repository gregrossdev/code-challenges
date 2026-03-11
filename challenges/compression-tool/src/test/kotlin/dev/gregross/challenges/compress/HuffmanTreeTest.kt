package dev.gregross.challenges.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class HuffmanTreeTest {

    @Test fun `builds tree from two characters`() {
        val tree = buildHuffmanTree(mapOf('a' to 3, 'b' to 1))
        assertIs<HuffmanNode.Branch>(tree)
        assertEquals(4, tree.frequency)
    }

    @Test fun `builds tree from three characters`() {
        val tree = buildHuffmanTree(mapOf('a' to 5, 'b' to 2, 'c' to 1))
        assertIs<HuffmanNode.Branch>(tree)
        assertEquals(8, tree.frequency)
    }

    @Test fun `root frequency equals total of all characters`() {
        val frequencies = mapOf('a' to 10, 'b' to 5, 'c' to 3, 'd' to 1)
        val tree = buildHuffmanTree(frequencies)
        assertEquals(19, tree.frequency)
    }

    @Test fun `single character produces a tree`() {
        val tree = buildHuffmanTree(mapOf('x' to 5))
        assertIs<HuffmanNode.Branch>(tree)
    }

    @Test fun `empty frequency map throws`() {
        assertFailsWith<IllegalArgumentException> {
            buildHuffmanTree(emptyMap())
        }
    }

    @Test fun `leaf count matches unique characters`() {
        val frequencies = mapOf('a' to 10, 'b' to 5, 'c' to 3, 'd' to 1, 'e' to 7)
        val tree = buildHuffmanTree(frequencies)
        assertEquals(5, countLeaves(tree))
    }

    @Test fun `higher frequency chars have shorter or equal codes`() {
        // With frequencies a=100, b=1, 'a' should get a shorter code
        val frequencies = mapOf('a' to 100, 'b' to 1, 'c' to 1, 'd' to 1)
        val tree = buildHuffmanTree(frequencies)
        val depths = mutableMapOf<Char, Int>()
        collectDepths(tree, 0, depths)
        assert(depths['a']!! <= depths['b']!!) {
            "Expected 'a' (freq=100) depth ${depths['a']} <= 'b' (freq=1) depth ${depths['b']}"
        }
    }

    private fun countLeaves(node: HuffmanNode): Int = when (node) {
        is HuffmanNode.Leaf -> 1
        is HuffmanNode.Branch -> countLeaves(node.left) + countLeaves(node.right)
    }

    private fun collectDepths(node: HuffmanNode, depth: Int, result: MutableMap<Char, Int>) {
        when (node) {
            is HuffmanNode.Leaf -> result[node.char] = depth
            is HuffmanNode.Branch -> {
                collectDepths(node.left, depth + 1, result)
                collectDepths(node.right, depth + 1, result)
            }
        }
    }
}
