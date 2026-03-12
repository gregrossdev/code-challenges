package dev.gregross.challenges.grep

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSearcherTest {

    private val searcher = FileSearcher()

    private fun testResource(name: String): String {
        return File(javaClass.classLoader.getResource(name)!!.toURI()).absolutePath
    }

    private fun testDir(name: String): String {
        return File(javaClass.classLoader.getResource(name)!!.toURI()).absolutePath
    }

    @Test fun `single file returns that file`() {
        val files = searcher.findFiles(listOf(testResource("test.txt")), recursive = false)
        assertEquals(1, files.size)
        assertTrue(files[0].name == "test.txt")
    }

    @Test fun `directory non-recursive returns empty`() {
        val files = searcher.findFiles(listOf(testDir("testdir")), recursive = false)
        assertEquals(0, files.size)
    }

    @Test fun `directory recursive lists all files`() {
        val files = searcher.findFiles(listOf(testDir("testdir")), recursive = true)
        assertEquals(3, files.size)
        val names = files.map { it.name }
        assertTrue("a.txt" in names)
        assertTrue("b.txt" in names)
        assertTrue("c.txt" in names)
    }

    @Test fun `recursive finds nested files`() {
        val files = searcher.findFiles(listOf(testDir("testdir")), recursive = true)
        assertTrue(files.any { it.path.contains("sub") && it.name == "c.txt" })
    }

    @Test fun `results are sorted by path`() {
        val files = searcher.findFiles(listOf(testDir("testdir")), recursive = true)
        assertEquals(files, files.sortedBy { it.path })
    }

    @Test fun `nonexistent path is skipped`() {
        val files = searcher.findFiles(listOf("/nonexistent/path"), recursive = false)
        assertEquals(0, files.size)
    }

    @Test fun `multiple paths`() {
        val files = searcher.findFiles(
            listOf(testResource("test.txt"), testDir("testdir")),
            recursive = true,
        )
        assertEquals(4, files.size)
    }
}
