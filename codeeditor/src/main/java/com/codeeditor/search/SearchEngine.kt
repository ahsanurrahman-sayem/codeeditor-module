package com.codeeditor.search

import com.codeeditor.model.TextBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Search and replace engine with regex support, case sensitivity,
 * whole word matching, and incremental search capabilities.
 *
 * @param textBuffer Text buffer to search within
 */
public class SearchEngine(private val textBuffer: TextBuffer) {
    private val _results = mutableListOf<SearchResult>()
    private val currentIndex = AtomicInteger(-1)
    private val _isVisible = AtomicBoolean(false)
    private val searchScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val searchMutex = Mutex()

    /** Whether the search panel is visible */
    public val isVisible: Boolean get() = _isVisible.get()

    /** Current search results */
    public val results: List<SearchResult> get() = _results.toList()

    /** Current result index */
    public val currentResultIndex: Int get() = currentIndex.get()

    /** Whether a current match exists */
    public val hasCurrentMatch: Boolean get() = currentIndex.get() in _results.indices

    /** Current match or null */
    public val currentMatch: SearchResult? get() = _results.getOrNull(currentIndex.get())

    /** Callback when results change */
    public var onResultsChanged: ((List<SearchResult>) -> Unit)? = null

    /** Callback when current match changes */
    public var onCurrentMatchChanged: ((SearchResult?) -> Unit)? = null

    /**
     * Shows the search panel.
     */
    public fun show() {
        _isVisible.set(true)
    }

    /**
     * Shows the find and replace panel.
     */
    public fun showReplace() {
        _isVisible.set(true)
    }

    /**
     * Hides the search panel.
     */
    public fun hide() {
        _isVisible.set(false)
        clearResults()
    }

    /**
     * Performs a search with the given query and options.
     *
     * @param query Search query string
     * @param options Search options
     * @return List of search results
     */
    public fun search(query: String, options: SearchOptions = SearchOptions()): List<SearchResult> {
        if (query.isEmpty()) {
            clearResults()
            return emptyList()
        }

        val text = textBuffer.getText()
        val regex = buildSearchRegex(query, options)
        val newResults = mutableListOf<SearchResult>()

        for (match in regex.findAll(text)) {
            newResults.add(SearchResult(
                start = match.range.first,
                end = match.range.last + 1,
                matchText = match.value,
                line = textBuffer.getLineForPosition(match.range.first)
            ))
        }

        searchScope.launch {
            searchMutex.withLock {
                _results.clear()
                _results.addAll(newResults)
                currentIndex.set(if (newResults.isNotEmpty()) 0 else -1)

                withContext(Dispatchers.Main) {
                    onResultsChanged?.invoke(newResults)
                    onCurrentMatchChanged?.invoke(currentMatch)
                }
            }
        }

        return newResults
    }

    /**
     * Performs incremental search starting from cursor position.
     *
     * @param query Search query
     * @param fromPosition Start position for search
     * @param options Search options
     * @return Next result from position, or null
     */
    public fun incrementalSearch(
        query: String,
        fromPosition: Int,
        options: SearchOptions = SearchOptions()
    ): SearchResult? {
        if (query.isEmpty()) return null

        val text = textBuffer.getText()
        val regex = buildSearchRegex(query, options)

        // Search from position first
        val forwardMatches = regex.findAll(text, fromPosition)
        val result = forwardMatches.firstOrNull()
            ?: regex.findAll(text).firstOrNull() // Wrap around

        return result?.let {
            SearchResult(
                start = it.range.first,
                end = it.range.last + 1,
                matchText = it.value,
                line = textBuffer.getLineForPosition(it.range.first)
            )
        }
    }

    /**
     * Navigates to the next search result.
     */
    public fun findNext(): SearchResult? {
        if (_results.isEmpty()) return null
        val next = (currentIndex.incrementAndGet()) % _results.size
        currentIndex.set(next)
        val result = _results[next]
        onCurrentMatchChanged?.invoke(result)
        return result
    }

    /**
     * Navigates to the previous search result.
     */
    public fun findPrevious(): SearchResult? {
        if (_results.isEmpty()) return null
        val prev = if (currentIndex.get() <= 0) _results.size - 1 else currentIndex.decrementAndGet()
        currentIndex.set(prev)
        val result = _results[prev]
        onCurrentMatchChanged?.invoke(result)
        return result
    }

    /**
     * Replaces the current match with the replacement text.
     *
     * @param replacement Replacement text
     * @return true if a replacement was made
     */
    public fun replaceCurrent(replacement: String): Boolean {
        val match = currentMatch ?: return false
        return runBlocking {
            searchMutex.withLock {
                textBuffer.replace(match.start, match.end, replacement)
                _results.removeAt(currentIndex.get())
                if (currentIndex.get() >= _results.size) {
                    currentIndex.set(0)
                }
                onResultsChanged?.invoke(_results.toList())
                onCurrentMatchChanged?.invoke(currentMatch)
                true
            }
        }
    }

    /**
     * Replaces all matches.
     *
     * @param query Search query
     * @param replacement Replacement text
     * @param options Search options
     * @return Number of replacements made
     */
    public fun replaceAll(query: String, replacement: String, options: SearchOptions = SearchOptions()): Int {
        if (query.isEmpty()) return 0

        val text = textBuffer.getText()
        val regex = buildSearchRegex(query, options)
        var count = 0
        val sb = StringBuilder()
        var lastEnd = 0

        for (match in regex.findAll(text)) {
            sb.append(text.substring(lastEnd, match.range.first))
            sb.append(replacement)
            lastEnd = match.range.last + 1
            count++
        }
        sb.append(text.substring(lastEnd))

        if (count > 0) {
            runBlocking {
                searchMutex.withLock {
                    textBuffer.delete(0, textBuffer.length)
                    textBuffer.insert(0, sb.toString())
                    clearResults()
                }
            }
        }

        return count
    }

    /**
     * Clears all search results.
     */
    public fun clearResults() {
        _results.clear()
        currentIndex.set(-1)
        onResultsChanged?.invoke(emptyList())
        onCurrentMatchChanged?.invoke(null)
    }

    /**
     * Shuts down the search engine.
     */
    public fun destroy() {
        searchScope.cancel()
        clearResults()
    }

    private fun buildSearchRegex(query: String, options: SearchOptions): Regex {
        var pattern = if (options.regex) query else Regex.escape(query)

        if (options.wholeWord) {
            pattern = "\\b$pattern\\b"
        }

        val flags = mutableSetOf<RegexOption>()
        if (!options.caseSensitive) {
            flags.add(RegexOption.IGNORE_CASE)
        }

        return Regex(pattern, flags)
    }
}

/**
 * A single search result.
 *
 * @property start Start position (inclusive)
 * @property end End position (exclusive)
 * @property matchText The matched text
 * @property line Line number (0-based)
 */
public data class SearchResult(
    val start: Int,
    val end: Int,
    val matchText: String,
    val line: Int
)

/**
 * Search options for controlling search behavior.
 *
 * @property regex Use regex pattern matching
 * @property caseSensitive Case-sensitive search
 * @property wholeWord Match whole words only
 * @property forward Search forward (vs backward)
 */
public data class SearchOptions(
    val regex: Boolean = false,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val forward: Boolean = true
)
