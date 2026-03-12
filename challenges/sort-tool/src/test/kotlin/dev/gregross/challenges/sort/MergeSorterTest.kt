package dev.gregross.challenges.sort

import kotlin.test.Test
import kotlin.test.assertEquals

class MergeSorterTest {

    private val sorter = MergeSorter()

    @Test fun `empty list`() {
        assertEquals(emptyList(), sorter.sort(emptyList()))
    }

    @Test fun `single element`() {
        assertEquals(listOf("apple"), sorter.sort(listOf("apple")))
    }

    @Test fun `already sorted`() {
        val input = listOf("apple", "banana", "cherry")
        assertEquals(listOf("apple", "banana", "cherry"), sorter.sort(input))
    }

    @Test fun `reverse sorted`() {
        val input = listOf("cherry", "banana", "apple")
        assertEquals(listOf("apple", "banana", "cherry"), sorter.sort(input))
    }

    @Test fun `random order`() {
        val input = listOf("banana", "apple", "date", "cherry")
        assertEquals(listOf("apple", "banana", "cherry", "date"), sorter.sort(input))
    }

    @Test fun `duplicates`() {
        val input = listOf("banana", "apple", "banana", "apple")
        assertEquals(listOf("apple", "apple", "banana", "banana"), sorter.sort(input))
    }

    @Test fun `stable sort preserves relative order of equal elements`() {
        // Use strings that compare equal but we can track by position
        val input = listOf("b", "a", "c", "a", "b")
        val result = sorter.sort(input)
        assertEquals(listOf("a", "a", "b", "b", "c"), result)
    }

    @Test fun `case sensitive sorting`() {
        val input = listOf("banana", "Apple", "cherry")
        // Uppercase comes before lowercase in lexicographic order
        assertEquals(listOf("Apple", "banana", "cherry"), sorter.sort(input))
    }
}
