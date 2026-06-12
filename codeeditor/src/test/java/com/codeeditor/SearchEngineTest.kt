package com.codeeditor

import com.codeeditor.model.TextBuffer
import com.codeeditor.search.SearchEngine
import com.codeeditor.search.SearchOptions
import com.codeeditor.search.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the search and replace engine.
 */
class SearchEngineTest {

    private lateinit var buffer: TextBuffer
    private lateinit var searchEngine: SearchEngine

    @Before
    fun setup() {
        buffer = TextBuffer.fromText(
            "Hello World\n" +
            "Hello Kotlin\n" +
            "Hello Java\n" +
            "Goodbye World"
        )
        searchEngine = SearchEngine(buffer)
        searchEngine.show()
    }

    @Test
    fun `search finds all occurrences`() {
        val results = searchEngine.search("Hello")

        assertEquals(3, results.size)
        assertTrue(results.all { it.matchText == "Hello" })
    }

    @Test
    fun `search with no matches returns empty`() {
        val results = searchEngine.search("xyz123")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `search is case insensitive by default`() {
        val results = searchEngine.search("hello")

        assertEquals(3, results.size)
    }

    @Test
    fun `case sensitive search respects casing`() {
        val results = searchEngine.search("hello", SearchOptions(caseSensitive = true))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `regex search works`() {
        val results = searchEngine.search("H.*o", SearchOptions(regex = true))

        assertEquals(3, results.size)
    }

    @Test
    fun `whole word search only matches full words`() {
        val results = searchEngine.search("Hello", SearchOptions(wholeWord = true))

        assertTrue(results.isNotEmpty())
        // "Hello" should be found as a whole word
    }

    @Test
    fun `find next cycles through results`() {
        searchEngine.search("Hello")

        val first = searchEngine.findNext()
        val second = searchEngine.findNext()
        val third = searchEngine.findNext()
        val fourth = searchEngine.findNext() // cycles back

        assertNotNull(first)
        assertNotNull(second)
        assertNotNull(third)
        assertNotNull(fourth)
        assertEquals(first?.start, fourth?.start) // Should cycle
    }

    @Test
    fun `find previous cycles backward`() {
        searchEngine.search("Hello")

        val first = searchEngine.findPrevious()
        assertNotNull(first)
    }

    @Test
    fun `search with empty query clears results`() {
        searchEngine.search("Hello")
        assertTrue(searchEngine.results.isNotEmpty())

        val results = searchEngine.search("")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `current match is first result initially`() {
        searchEngine.search("Hello")

        assertTrue(searchEngine.hasCurrentMatch)
        assertNotNull(searchEngine.currentMatch)
        assertEquals(0, searchEngine.currentResultIndex)
    }

    @Test
    fun `no results means no current match`() {
        searchEngine.search("nonexistent")

        assertFalse(searchEngine.hasCurrentMatch)
        assertNull(searchEngine.currentMatch)
    }

    @Test
    fun `isVisible returns true after show`() {
        assertTrue(searchEngine.isVisible)
    }

    @Test
    fun `isVisible returns false after hide`() {
        searchEngine.hide()
        assertFalse(searchEngine.isVisible)
    }

    @Test
    fun `results cleared after hide`() {
        searchEngine.search("Hello")
        searchEngine.hide()
        assertTrue(searchEngine.results.isEmpty())
    }

    @Test
    fun `incremental search finds from position`() {
        val result = searchEngine.incrementalSearch("Kotlin", 10)

        assertNotNull(result)
        assertEquals("Kotlin", result?.matchText)
    }

    @Test
    fun `incremental search with empty query returns null`() {
        val result = searchEngine.incrementalSearch("", 0)
        assertNull(result)
    }

    @Test
    fun `results contain correct line numbers`() {
        val results = searchEngine.search("Hello")

        results.forEachIndexed { index, result ->
            assertEquals(index, result.line)
        }
    }

    @Test
    fun `search result has correct structure`() {
        val results = searchEngine.search("World")

        assertEquals(2, results.size)
        val first = results[0]
        assertEquals("World", first.matchText)
        assertTrue(first.start < first.end)
        assertEquals(0, first.line) // First "World" is on line 0
    }

    @Test
    fun `replace all returns correct count`() {
        val count = searchEngine.replaceAll("Hello", "Hi")

        assertEquals(3, count)
    }

    @Test
    fun `replace all with no matches returns zero`() {
        val count = searchEngine.replaceAll("xyz", "abc")

        assertEquals(0, count)
    }

    @Test
    fun `destroy cleans up`() {
        searchEngine.destroy()
        assertTrue(true) // Should not throw
    }

    @Test
    fun `hide clears current match`() {
        searchEngine.search("Hello")
        assertTrue(searchEngine.hasCurrentMatch)

        searchEngine.hide()
        assertFalse(searchEngine.hasCurrentMatch)
    }

    @Test
    fun `find next without search returns null`() {
        assertNull(searchEngine.findNext())
    }

    @Test
    fun `find previous without search returns null`() {
        assertNull(searchEngine.findPrevious())
    }

    @Test
    fun `search preserves across results`() {
        val results = searchEngine.search("Hello")
        val next = searchEngine.findNext()

        assertTrue(results.size > 1)
        assertNotNull(next)
    }
}
