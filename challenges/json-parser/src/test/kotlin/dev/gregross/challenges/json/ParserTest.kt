package dev.gregross.challenges.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ParserTest {

    private fun parse(input: String): JsonValue {
        val tokens = Lexer(input).tokenize()
        return Parser(tokens).parse()
    }

    @Test
    fun `parses empty object`() {
        val result = parse("{}")
        assertIs<JsonValue.JsonObject>(result)
        assertEquals(0, result.members.size)
    }

    @Test
    fun `parses string key-value`() {
        val result = parse("""{"key": "value"}""")
        assertIs<JsonValue.JsonObject>(result)
        assertEquals(JsonValue.JsonString("value"), result.members["key"])
    }

    @Test
    fun `parses multiple key-values`() {
        val result = parse("""{"key": "value", "key2": "value2"}""")
        assertIs<JsonValue.JsonObject>(result)
        assertEquals(2, result.members.size)
    }

    @Test
    fun `parses all value types`() {
        val result = parse("""{"a": true, "b": false, "c": null, "d": "str", "e": 101}""")
        assertIs<JsonValue.JsonObject>(result)
        assertEquals(JsonValue.JsonBool(true), result.members["a"])
        assertEquals(JsonValue.JsonBool(false), result.members["b"])
        assertEquals(JsonValue.JsonNull, result.members["c"])
        assertEquals(JsonValue.JsonString("str"), result.members["d"])
        assertEquals(JsonValue.JsonNumber(101.0), result.members["e"])
    }

    @Test
    fun `parses nested object`() {
        val result = parse("""{"outer": {"inner": "value"}}""")
        assertIs<JsonValue.JsonObject>(result)
        val inner = result.members["outer"]
        assertIs<JsonValue.JsonObject>(inner)
        assertEquals(JsonValue.JsonString("value"), inner.members["inner"])
    }

    @Test
    fun `parses empty array`() {
        val result = parse("[]")
        assertIs<JsonValue.JsonArray>(result)
        assertEquals(0, result.elements.size)
    }

    @Test
    fun `parses array with values`() {
        val result = parse("""[1, "two", true, null]""")
        assertIs<JsonValue.JsonArray>(result)
        assertEquals(4, result.elements.size)
        assertEquals(JsonValue.JsonNumber(1.0), result.elements[0])
        assertEquals(JsonValue.JsonString("two"), result.elements[1])
        assertEquals(JsonValue.JsonBool(true), result.elements[2])
        assertEquals(JsonValue.JsonNull, result.elements[3])
    }

    @Test
    fun `parses nested arrays and objects`() {
        val result = parse("""{"key": "value", "arr": [1, {"nested": true}], "obj": {}}""")
        assertIs<JsonValue.JsonObject>(result)
        val arr = result.members["arr"]
        assertIs<JsonValue.JsonArray>(arr)
        assertEquals(2, arr.elements.size)
        val nested = arr.elements[1]
        assertIs<JsonValue.JsonObject>(nested)
        assertEquals(JsonValue.JsonBool(true), nested.members["nested"])
    }

    @Test
    fun `rejects empty input`() {
        assertFailsWith<JsonParseException> { parse("") }
    }

    @Test
    fun `rejects trailing comma in object`() {
        assertFailsWith<JsonParseException> { parse("""{"key": "value",}""") }
    }

    @Test
    fun `rejects trailing comma in array`() {
        assertFailsWith<JsonParseException> { parse("[1, 2,]") }
    }

    @Test
    fun `rejects missing colon`() {
        assertFailsWith<JsonParseException> { parse("""{"key" "value"}""") }
    }

    @Test
    fun `rejects unquoted key`() {
        assertFailsWith<JsonParseException> { parse("""{key: "value"}""") }
    }

    @Test
    fun `rejects unclosed object`() {
        assertFailsWith<JsonParseException> { parse("""{"key": "value"""") }
    }

    @Test
    fun `rejects unclosed array`() {
        assertFailsWith<JsonParseException> { parse("[1, 2") }
    }

    @Test
    fun `rejects extra tokens after value`() {
        assertFailsWith<JsonParseException> { parse("{}{}") }
    }

    @Test
    fun `parses deeply nested structure`() {
        val result = parse("""{"a": {"b": {"c": [[[1]]]}}}""")
        assertIs<JsonValue.JsonObject>(result)
    }

    @Test
    fun `parses number as value`() {
        val result = parse("""{"n": -0.5e+2}""")
        assertIs<JsonValue.JsonObject>(result)
        assertEquals(JsonValue.JsonNumber(-50.0), result.members["n"])
    }
}
