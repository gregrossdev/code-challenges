package dev.gregross.challenges.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffGeneratorTest {

    private val generator = DiffGenerator()

    @Test fun `identical files produce all common entries`() {
        val lines = listOf("alpha", "beta", "gamma")
        val entries = generator.generate(lines, lines)
        assertEquals(3, entries.size)
        assertTrue(entries.all { it is DiffEntry.Common })
    }

    @Test fun `empty files produce no entries`() {
        assertEquals(emptyList(), generator.generate(emptyList(), emptyList()))
    }

    @Test fun `all lines removed`() {
        val original = listOf("alpha", "beta")
        val entries = generator.generate(original, emptyList())
        assertEquals(2, entries.size)
        assertTrue(entries.all { it is DiffEntry.Removed })
        assertEquals("alpha", entries[0].line)
        assertEquals("beta", entries[1].line)
    }

    @Test fun `all lines added`() {
        val modified = listOf("alpha", "beta")
        val entries = generator.generate(emptyList(), modified)
        assertEquals(2, entries.size)
        assertTrue(entries.all { it is DiffEntry.Added })
    }

    @Test fun `single line modified`() {
        val original = listOf("alpha", "beta", "gamma")
        val modified = listOf("alpha", "BETA", "gamma")
        val entries = generator.generate(original, modified)

        val removed = entries.filterIsInstance<DiffEntry.Removed>()
        val added = entries.filterIsInstance<DiffEntry.Added>()
        assertEquals(1, removed.size)
        assertEquals("beta", removed[0].line)
        assertEquals(1, added.size)
        assertEquals("BETA", added[0].line)
    }

    @Test fun `line added in middle`() {
        val original = listOf("first", "third")
        val modified = listOf("first", "second", "third")
        val entries = generator.generate(original, modified)

        assertEquals(3, entries.size)
        assertEquals(DiffEntry.Common("first"), entries[0])
        assertEquals(DiffEntry.Added("second"), entries[1])
        assertEquals(DiffEntry.Common("third"), entries[2])
    }

    @Test fun `line removed from middle`() {
        val original = listOf("first", "second", "third")
        val modified = listOf("first", "third")
        val entries = generator.generate(original, modified)

        assertEquals(3, entries.size)
        assertEquals(DiffEntry.Common("first"), entries[0])
        assertEquals(DiffEntry.Removed("second"), entries[1])
        assertEquals(DiffEntry.Common("third"), entries[2])
    }

    @Test fun `multiple change groups`() {
        val original = listOf("a", "b", "c", "d", "e")
        val modified = listOf("a", "B", "c", "D", "e")
        val entries = generator.generate(original, modified)

        val removed = entries.filterIsInstance<DiffEntry.Removed>()
        val added = entries.filterIsInstance<DiffEntry.Added>()
        assertEquals(2, removed.size)
        assertEquals(2, added.size)
        assertEquals(listOf("b", "d"), removed.map { it.line })
        assertEquals(listOf("B", "D"), added.map { it.line })
    }
}
