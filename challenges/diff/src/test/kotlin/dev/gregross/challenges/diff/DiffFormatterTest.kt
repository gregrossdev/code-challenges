package dev.gregross.challenges.diff

import kotlin.test.Test
import kotlin.test.assertEquals

class DiffFormatterTest {

    private val formatter = DiffFormatter()
    private val generator = DiffGenerator()

    private fun diff(original: List<String>, modified: List<String>): String {
        return formatter.format(generator.generate(original, modified))
    }

    @Test fun `identical files produce empty output`() {
        assertEquals("", diff(listOf("a", "b", "c"), listOf("a", "b", "c")))
    }

    @Test fun `empty files produce empty output`() {
        assertEquals("", diff(emptyList(), emptyList()))
    }

    @Test fun `all lines removed`() {
        val output = diff(listOf("alpha", "beta"), emptyList())
        assertEquals("< alpha\n< beta\n", output)
    }

    @Test fun `all lines added`() {
        val output = diff(emptyList(), listOf("alpha", "beta"))
        assertEquals("> alpha\n> beta\n", output)
    }

    @Test fun `single modification has separator`() {
        val output = diff(listOf("a", "old", "c"), listOf("a", "new", "c"))
        assertEquals("< old\n---\n> new\n", output)
    }

    @Test fun `pure removal no separator`() {
        val output = diff(listOf("a", "b", "c"), listOf("a", "c"))
        assertEquals("< b\n", output)
    }

    @Test fun `pure addition no separator`() {
        val output = diff(listOf("a", "c"), listOf("a", "b", "c"))
        assertEquals("> b\n", output)
    }

    @Test fun `multiple change groups separated by blank line`() {
        val output = diff(
            listOf("a", "old1", "c", "old2", "e"),
            listOf("a", "new1", "c", "new2", "e"),
        )
        assertEquals("< old1\n---\n> new1\n\n< old2\n---\n> new2\n", output)
    }

    @Test fun `mixed adds and removes across groups`() {
        val output = diff(
            listOf("a", "b", "c", "d"),
            listOf("a", "c", "x", "d"),
        )
        assertEquals("< b\n\n> x\n", output)
    }
}
