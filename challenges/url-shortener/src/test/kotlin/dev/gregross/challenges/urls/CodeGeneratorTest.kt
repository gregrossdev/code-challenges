package dev.gregross.challenges.urls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CodeGeneratorTest {

    private val generator = CodeGenerator()

    @Test fun `same URL produces same code`() {
        val code1 = generator.generate("https://example.com")
        val code2 = generator.generate("https://example.com")
        assertEquals(code1, code2)
    }

    @Test fun `different URLs produce different codes`() {
        val code1 = generator.generate("https://example.com")
        val code2 = generator.generate("https://other.com")
        assertNotEquals(code1, code2)
    }

    @Test fun `code is 8 characters`() {
        val code = generator.generate("https://example.com")
        assertEquals(8, code.length)
    }

    @Test fun `code uses base62 alphabet only`() {
        val code = generator.generate("https://example.com")
        assertTrue(code.all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' })
    }

    @Test fun `collision retry produces different code`() {
        val code0 = generator.generate("https://example.com", 0)
        val code1 = generator.generate("https://example.com", 1)
        val code2 = generator.generate("https://example.com", 2)
        assertNotEquals(code0, code1)
        assertNotEquals(code1, code2)
    }

    @Test fun `deterministic output`() {
        val code = generator.generate("https://www.google.com")
        assertEquals(code, generator.generate("https://www.google.com"))
    }
}
