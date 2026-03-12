package dev.gregross.challenges.sort

import kotlin.test.Test
import kotlin.test.assertEquals

class RadixSorterTest {

    private val sorter = RadixSorter()

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

    @Test fun `varying length strings`() {
        val input = listOf("fig", "apple", "kiwi", "a", "banana")
        assertEquals(listOf("a", "apple", "banana", "fig", "kiwi"), sorter.sort(input))
    }

    @Test fun `case sensitive sorting`() {
        val input = listOf("banana", "Apple", "cherry")
        assertEquals(listOf("Apple", "banana", "cherry"), sorter.sort(input))
    }
}
