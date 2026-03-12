package dev.gregross.challenges.urls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlStoreTest {

    @Test fun `create and retrieve by code`() {
        val store = UrlStore()
        assertTrue(store.create("abc123", "https://example.com"))
        assertEquals("https://example.com", store.getUrl("abc123"))
    }

    @Test fun `retrieve code by url`() {
        val store = UrlStore()
        store.create("abc123", "https://example.com")
        assertEquals("abc123", store.getCode("https://example.com"))
    }

    @Test fun `create returns false on duplicate code`() {
        val store = UrlStore()
        assertTrue(store.create("abc123", "https://example.com"))
        assertFalse(store.create("abc123", "https://other.com"))
    }

    @Test fun `get unknown code returns null`() {
        val store = UrlStore()
        assertNull(store.getUrl("unknown"))
    }

    @Test fun `get unknown url returns null`() {
        val store = UrlStore()
        assertNull(store.getCode("https://unknown.com"))
    }

    @Test fun `delete removes from both maps`() {
        val store = UrlStore()
        store.create("abc123", "https://example.com")
        assertTrue(store.delete("abc123"))
        assertNull(store.getUrl("abc123"))
        assertNull(store.getCode("https://example.com"))
    }

    @Test fun `delete unknown returns false`() {
        val store = UrlStore()
        assertFalse(store.delete("unknown"))
    }

    @Test fun `size tracks entries`() {
        val store = UrlStore()
        assertEquals(0, store.size())
        store.create("a", "https://a.com")
        store.create("b", "https://b.com")
        assertEquals(2, store.size())
        store.delete("a")
        assertEquals(1, store.size())
    }
}
