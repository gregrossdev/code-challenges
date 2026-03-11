package dev.gregross.challenges.json

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class IntegrationTest {

    private fun parseFile(path: String): JsonValue {
        val input = this::class.java.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Test file not found: $path")
        val text = input.bufferedReader().readText()
        val tokens = Lexer(text).tokenize()
        return Parser(tokens).parse()
    }

    private fun assertValid(path: String) {
        val result = parseFile(path)
        assertNotNull(result)
    }

    private fun assertInvalid(path: String) {
        assertFailsWith<JsonParseException> { parseFile(path) }
    }

    // Step 1 — empty object and empty input
    @Test fun `step1 valid - empty object`() = assertValid("/step1/valid.json")
    @Test fun `step1 invalid - empty input`() = assertInvalid("/step1/invalid.json")

    // Step 2 — string key-value pairs
    @Test fun `step2 valid - single key-value`() = assertValid("/step2/valid.json")
    @Test fun `step2 valid - multiple key-values`() = assertValid("/step2/valid2.json")
    @Test fun `step2 invalid - trailing comma`() = assertInvalid("/step2/invalid.json")
    @Test fun `step2 invalid - unquoted key`() = assertInvalid("/step2/invalid2.json")

    // Step 3 — multiple data types
    @Test fun `step3 valid - all value types`() {
        val result = parseFile("/step3/valid.json")
        assertIs<JsonValue.JsonObject>(result)
    }
    @Test fun `step3 invalid - capitalized False`() = assertInvalid("/step3/invalid.json")

    // Step 4 — nested structures
    @Test fun `step4 valid - nested objects and arrays`() {
        val result = parseFile("/step4/valid.json")
        assertIs<JsonValue.JsonObject>(result)
    }
    @Test fun `step4 valid - deeply nested`() {
        val result = parseFile("/step4/valid2.json")
        assertIs<JsonValue.JsonObject>(result)
    }
    @Test fun `step4 invalid - trailing comma in nested object`() = assertInvalid("/step4/invalid.json")

    // Pretty printer round-trip
    @Test fun `pretty print round-trips valid json`() {
        val input = """{"key": "value", "num": 42, "arr": [1, 2, 3], "nested": {"a": true}}"""
        val tokens = Lexer(input).tokenize()
        val value = Parser(tokens).parse()
        val printed = prettyPrint(value)

        // Re-parse the pretty-printed output
        val tokens2 = Lexer(printed).tokenize()
        val value2 = Parser(tokens2).parse()
        assertIs<JsonValue.JsonObject>(value2)
    }
}
