package dev.gregross.challenges.grep

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatcherTest {

    @Test fun `literal match`() {
        assertTrue(Matcher("hello").matches("say hello world"))
    }

    @Test fun `literal no match`() {
        assertFalse(Matcher("hello").matches("goodbye world"))
    }

    @Test fun `empty pattern matches everything`() {
        assertTrue(Matcher("").matches("anything"))
        assertTrue(Matcher("").matches(""))
    }

    @Test fun `digit class`() {
        assertTrue(Matcher("\\d").matches("abc123"))
        assertFalse(Matcher("\\d").matches("abcdef"))
    }

    @Test fun `word class`() {
        assertTrue(Matcher("\\w").matches("hello"))
        assertFalse(Matcher("\\w").matches("   "))
    }

    @Test fun `start anchor`() {
        assertTrue(Matcher("^hello").matches("hello world"))
        assertFalse(Matcher("^hello").matches("say hello"))
    }

    @Test fun `end anchor`() {
        assertTrue(Matcher("world$").matches("hello world"))
        assertFalse(Matcher("world$").matches("world hello"))
    }

    @Test fun `character class`() {
        assertTrue(Matcher("[abc]").matches("xbz"))
        assertFalse(Matcher("[abc]").matches("xyz"))
    }

    @Test fun `negated character class`() {
        assertTrue(Matcher("[^abc]").matches("axyz"))
        assertFalse(Matcher("[^abc]").matches("abc"))
    }

    @Test fun `case insensitive`() {
        assertTrue(Matcher("hello", ignoreCase = true).matches("HELLO WORLD"))
        assertFalse(Matcher("hello", ignoreCase = false).matches("HELLO WORLD"))
    }

    @Test fun `invert match`() {
        assertFalse(Matcher("hello", invert = true).matches("hello world"))
        assertTrue(Matcher("hello", invert = true).matches("goodbye world"))
    }

    @Test fun `invert with case insensitive`() {
        assertFalse(Matcher("hello", ignoreCase = true, invert = true).matches("HELLO"))
        assertTrue(Matcher("hello", ignoreCase = true, invert = true).matches("GOODBYE"))
    }

    @Test fun `single character`() {
        assertTrue(Matcher("x").matches("text"))
        assertFalse(Matcher("x").matches("hello"))
    }
}
