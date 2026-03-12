package dev.gregross.challenges.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class InputParserTest {

    private val parser = InputParser()

    // Token parsing

    @Test fun `simple command`() {
        assertEquals(listOf("ls"), parser.parseTokens("ls"))
    }

    @Test fun `command with args`() {
        assertEquals(listOf("ls", "-la"), parser.parseTokens("ls -la"))
    }

    @Test fun `multiple spaces between args`() {
        assertEquals(listOf("echo", "hello"), parser.parseTokens("echo   hello"))
    }

    @Test fun `leading and trailing whitespace`() {
        assertEquals(listOf("echo", "hi"), parser.parseTokens("  echo hi  "))
    }

    @Test fun `empty input`() {
        assertEquals(emptyList(), parser.parseTokens(""))
    }

    @Test fun `whitespace only`() {
        assertEquals(emptyList(), parser.parseTokens("   "))
    }

    @Test fun `single quoted string preserves spaces`() {
        assertEquals(listOf("echo", "hello world"), parser.parseTokens("echo 'hello world'"))
    }

    @Test fun `double quoted string preserves spaces`() {
        assertEquals(listOf("echo", "hello world"), parser.parseTokens("echo \"hello world\""))
    }

    @Test fun `quoted string adjacent to unquoted`() {
        assertEquals(listOf("echo", "helloworld"), parser.parseTokens("echo hello\"world\""))
    }

    @Test fun `empty quotes produce empty token`() {
        assertEquals(listOf("echo", ""), parser.parseTokens("echo ''"))
    }

    // Pipeline parsing

    @Test fun `single command pipeline`() {
        assertEquals(listOf(listOf("ls", "-la")), parser.parsePipeline("ls -la"))
    }

    @Test fun `two command pipeline`() {
        assertEquals(
            listOf(listOf("cat", "file"), listOf("wc", "-l")),
            parser.parsePipeline("cat file | wc -l"),
        )
    }

    @Test fun `three command pipeline`() {
        assertEquals(
            listOf(listOf("cat", "file"), listOf("grep", "hello"), listOf("wc", "-l")),
            parser.parsePipeline("cat file | grep hello | wc -l"),
        )
    }

    @Test fun `pipe inside quotes is not split`() {
        assertEquals(
            listOf(listOf("echo", "hello | world")),
            parser.parsePipeline("echo 'hello | world'"),
        )
    }

    @Test fun `pipe with no spaces`() {
        assertEquals(
            listOf(listOf("echo", "hi"), listOf("cat")),
            parser.parsePipeline("echo hi|cat"),
        )
    }
}
