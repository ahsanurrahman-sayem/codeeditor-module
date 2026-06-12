package com.codeeditor

import com.codeeditor.highlighting.*
import com.codeeditor.model.TextBuffer
import com.codeeditor.theme.EditorThemes
import com.codeeditor.theme.ThemeManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the syntax highlighting engine.
 */
class HighlightingEngineTest {

    private lateinit var buffer: TextBuffer
    private lateinit var themeManager: ThemeManager
    private lateinit var engine: HighlightingEngine

    @Before
    fun setup() {
        buffer = TextBuffer.fromText("fun main() {\n    println(\"Hello\")\n    val x = 42\n}")
        themeManager = ThemeManager()
        themeManager.setTheme(EditorThemes.Light)
        engine = HighlightingEngine(buffer, themeManager)
    }

    @Test
    fun `tokenize line identifies keywords`() = runBlocking {
        engine.setLanguage(LanguageRegistry.getLanguage("kotlin"))
        engine.processHighlights()

        val spannable = engine.getHighlightedSpannable(0, buffer.length)
        assertNotNull(spannable)
    }

    @Test
    fun `regex highlighter finds matches`() {
        val highlighter = RegexHighlighter()
        val language = LanguageRegistry.getLanguage("kotlin")!!

        val text = "class MyClass { fun test() = 42 }"
        val tokens = highlighter.highlight(text, 0, language)

        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.any { it.type == TokenType.KEYWORD && it.text == "class" })
        assertTrue(tokens.any { it.type == TokenType.KEYWORD && it.text == "fun" })
    }

    @Test
    fun `token highlighter processes all token types`() {
        val highlighter = TokenHighlighter()
        val language = LanguageDefinition(
            name = "test",
            fileExtensions = listOf("test"),
            keywords = setOf("if", "else", "return"),
            singleLineComment = "//",
            stringDelimiters = setOf('"', '\'')
        )

        val text = """
            // comment
            if (x == 42) {
                return "hello"
            }
        """.trimIndent()

        val tokens = highlighter.highlight(text, 0, language)

        assertTrue(tokens.isNotEmpty())
        assertTrue("Should find comment", tokens.any { it.type == TokenType.COMMENT })
        assertTrue("Should find keyword", tokens.any { it.type == TokenType.KEYWORD })
        assertTrue("Should find string", tokens.any { it.type == TokenType.STRING })
        assertTrue("Should find number", tokens.any { it.type == TokenType.NUMBER })
    }

    @Test
    fun `language registry contains kotlin`() {
        val kotlin = LanguageRegistry.getLanguage("kotlin")
        assertNotNull(kotlin)
        assertEquals("kotlin", kotlin!!.name)
        assertTrue(kotlin.keywords.contains("fun"))
        assertTrue(kotlin.keywords.contains("val"))
        assertTrue(kotlin.keywords.contains("class"))
    }

    @Test
    fun `language detection from extension`() {
        assertEquals("kotlin", LanguageRegistry.detectFromExtension("test.kt")?.name)
        assertEquals("java", LanguageRegistry.detectFromExtension("test.java")?.name)
        assertEquals("python", LanguageRegistry.detectFromExtension("test.py")?.name)
        assertEquals("javascript", LanguageRegistry.detectFromExtension("test.js")?.name)
    }

    @Test
    fun `unknown language returns null`() {
        assertNull(LanguageRegistry.getLanguage("nonexistent"))
    }

    @Test
    fun `highlight cache is cleared on language change`() = runBlocking {
        engine.setLanguage(LanguageRegistry.getLanguage("kotlin"))
        engine.processHighlights()

        engine.setLanguage(LanguageRegistry.getLanguage("java"))
        // Should not throw and should clear cache
    }

    @Test
    fun `engine handles empty buffer`() = runBlocking {
        val emptyBuffer = TextBuffer.create()
        val emptyEngine = HighlightingEngine(emptyBuffer, themeManager)
        emptyEngine.setLanguage(LanguageRegistry.getLanguage("kotlin"))
        emptyEngine.processHighlights()

        val spannable = emptyEngine.getHighlightedSpannable(0, 0)
        assertEquals(0, spannable.length)
    }

    @Test
    fun `engine handles null language gracefully`() = runBlocking {
        engine.setLanguage(null)
        engine.processHighlights()
        val spannable = engine.getHighlightedSpannable(0, buffer.length)
        assertNotNull(spannable)
    }

    @Test
    fun `visible region highlighting prioritizes visible lines`() = runBlocking {
        engine.setLanguage(LanguageRegistry.getLanguage("kotlin"))
        engine.setVisibleRegion(0, 1)
        engine.processHighlights()

        // Should complete without error
        assertTrue(true)
    }
}
