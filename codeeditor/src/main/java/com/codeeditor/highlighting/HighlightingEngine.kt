package com.codeeditor.highlighting

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.codeeditor.model.TextBuffer
import com.codeeditor.theme.EditorTheme
import com.codeeditor.theme.ThemeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Incremental syntax highlighting engine with background parsing
 * and visible-region-first processing.
 *
 * @property textBuffer The text buffer to highlight
 * @property themeManager Theme manager for color resolution
 */
public class HighlightingEngine(
    private val textBuffer: TextBuffer,
    private val themeManager: ThemeManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val highlightCache = ConcurrentHashMap<Int, List<HighlightToken>>()
    private val visibleLines = AtomicInteger(0) to AtomicInteger(0)
    private val isRunning = AtomicBoolean(false)
    private val processingMutex = Mutex()
    private var currentLanguage: LanguageDefinition? = null
    private val languageMutex = Mutex()
    private var pendingJob: Job? = null
    private var incrementalVersion = AtomicInteger(0)

    /** Callback for when highlights are ready */
    public var onHighlightsUpdated: ((Int, Int, List<HighlightToken>) -> Unit)? = null

    /**
     * Sets the language for highlighting.
     */
    public suspend fun setLanguage(language: LanguageDefinition?) {
        languageMutex.withLock {
            currentLanguage = language
            highlightCache.clear()
            scheduleRehighlight()
        }
    }

    /**
     * Updates the visible region for priority highlighting.
     *
     * @param firstVisibleLine First visible line (0-based)
     * @param lastVisibleLine Last visible line (0-based)
     */
    public fun setVisibleRegion(firstVisibleLine: Int, lastVisibleLine: Int) {
        visibleLines.first.set(firstVisibleLine)
        visibleLines.second.set(lastVisibleLine)
    }

    /**
     * Schedules incremental re-highlighting after text changes.
     */
    public fun scheduleRehighlight() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            incrementalVersion.incrementAndGet()
            processHighlights()
        }
    }

    /**
     * Processes highlights for the current text and language.
     */
    public suspend fun processHighlights() {
        if (!processingMutex.tryLock()) return
        try {
            isRunning.set(true)
            val language = languageMutex.withLock { currentLanguage }
            if (language == null) return

            val version = incrementalVersion.get()
            val firstVisible = visibleLines.first.get()
            val lastVisible = visibleLines.second.get()

            // Priority 1: Visible region
            highlightRange(firstVisible, lastVisible, language, version)

            // Priority 2: Rest of document in chunks
            val lineCount = textBuffer.lineCount
            if (version == incrementalVersion.get()) {
                // Lines before visible region
                if (firstVisible > 0) {
                    highlightRange(0, firstVisible - 1, language, version)
                }
                // Lines after visible region
                if (lastVisible < lineCount - 1) {
                    highlightRange(lastVisible + 1, lineCount - 1, language, version)
                }
            }
        } finally {
            isRunning.set(false)
            processingMutex.unlock()
        }
    }

    /**
     * Generates Spannable with applied highlighting for the given text range.
     */
    public fun getHighlightedSpannable(start: Int, end: Int): Spannable {
        val text = textBuffer.substring(start, end)
        val spannable = SpannableStringBuilder(text)
        val theme = themeManager.theme

        // Collect all tokens in this range
        val tokens = mutableListOf<HighlightToken>()
        val startLine = textBuffer.getLineForPosition(start)
        val endLine = textBuffer.getLineForPosition(end)

        for (line in startLine..endLine) {
            val cached = highlightCache[line]
            if (cached != null) {
                tokens.addAll(cached.filter { it.start in start until end || it.end in start..end })
            }
        }

        // Apply spans
        for (token in tokens.sortedBy { it.start }) {
            val tokenStart = (token.start - start).coerceIn(0, text.length)
            val tokenEnd = (token.end - start).coerceIn(0, text.length)
            if (tokenStart < tokenEnd) {
                val color = resolveTokenColor(token.type, theme)
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    tokenStart,
                    tokenEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return spannable
    }

    /**
     * Clears the highlight cache.
     */
    public fun clearCache() {
        highlightCache.clear()
    }

    /**
     * Shuts down the highlighting engine.
     */
    public fun shutdown() {
        scope.cancel()
        highlightCache.clear()
    }

    private suspend fun highlightRange(
        startLine: Int,
        endLine: Int,
        language: LanguageDefinition,
        version: Int
    ) {
        for (line in startLine..endLine.coerceAtMost(textBuffer.lineCount - 1)) {
            if (version != incrementalVersion.get()) return
            yield()

            if (highlightCache.containsKey(line)) continue

            val tokens = tokenizeLine(line, language)
            highlightCache[line] = tokens

            // Notify for visible lines
            if (line in visibleLines.first.get()..visibleLines.second.get()) {
                val lineStart = textBuffer.getLineStart(line)
                val lineEnd = textBuffer.getLineEnd(line)
                onHighlightsUpdated?.invoke(lineStart, lineEnd, tokens)
            }
        }
    }

    private fun tokenizeLine(line: Int, language: LanguageDefinition): List<HighlightToken> {
        val lineStart = textBuffer.getLineStart(line)
        val lineEnd = textBuffer.getLineEnd(line)
        if (lineStart >= lineEnd) return emptyList()

        val text = textBuffer.substring(lineStart, lineEnd)
        val tokens = mutableListOf<HighlightToken>()
        val processed = BooleanArray(text.length) { false }

        // Apply regex rules first
        val sortedRules = language.rules.sortedByDescending { it.priority }
        for (rule in sortedRules) {
            for (match in rule.pattern.findAll(text)) {
                val start = match.range.first
                val end = match.range.last + 1
                if ((start until end).any { processed[it] }) continue

                tokens.add(HighlightToken(lineStart + start, lineStart + end, rule.tokenType, match.value))
                for (i in start until end) processed[i] = true
            }
        }

        // Tokenize remaining text
        var pos = 0
        while (pos < text.length) {
            if (processed[pos]) { pos++; continue }

            val c = text[pos]
            val absolutePos = lineStart + pos

            // Skip whitespace
            if (c.isWhitespace()) { pos++; continue }

            // Check for single-line comment
            language.singleLineComment?.let { marker ->
                if (text.startsWith(marker, pos)) {
                    val commentEnd = text.length
                    tokens.add(HighlightToken(absolutePos, lineStart + commentEnd, TokenType.COMMENT,
                        text.substring(pos)))
                    for (i in pos until commentEnd) processed[i] = true
                    return tokens
                }
            }

            // Check for string literals
            if (c in language.stringDelimiters) {
                val stringEnd = findStringEnd(text, pos, c, language.escapeChar)
                tokens.add(HighlightToken(absolutePos, lineStart + stringEnd + 1, TokenType.STRING,
                    text.substring(pos, stringEnd + 1)))
                for (i in pos..stringEnd) processed[i] = true
                pos = stringEnd + 1
                continue
            }

            // Check for numbers
            if (c.isDigit() || (c == '.' && pos + 1 < text.length && text[pos + 1].isDigit())) {
                val numEnd = findNumberEnd(text, pos)
                val numText = text.substring(pos, numEnd)
                tokens.add(HighlightToken(absolutePos, lineStart + numEnd, TokenType.NUMBER, numText))
                for (i in pos until numEnd) processed[i] = true
                pos = numEnd
                continue
            }

            // Check for identifiers/keywords
            if (c.isJavaIdentifierStart() || c == '_') {
                val identEnd = findIdentifierEnd(text, pos)
                val ident = text.substring(pos, identEnd)
                val type = when {
                    language.keywords.contains(ident) -> TokenType.KEYWORD
                    ident[0].isUpperCase() -> TokenType.TYPE
                    identEnd < text.length && text[identEnd] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(HighlightToken(absolutePos, lineStart + identEnd, type, ident))
                for (i in pos until identEnd) processed[i] = true
                pos = identEnd
                continue
            }

            // Operators and punctuation
            val tokenType = when (c) {
                in "(){}[]" -> TokenType.BRACKET
                in "+-*/%=<>!&|^~?:" -> TokenType.OPERATOR
                in ",;.:" -> TokenType.PUNCTUATION
                else -> TokenType.UNKNOWN
            }
            tokens.add(HighlightToken(absolutePos, absolutePos + 1, tokenType, c.toString()))
            processed[pos] = true
            pos++
        }

        return tokens.sortedBy { it.start }
    }

    private fun findStringEnd(text: String, start: Int, delimiter: Char, escape: Char): Int {
        var pos = start + 1
        while (pos < text.length) {
            if (text[pos] == escape) { pos += 2; continue }
            if (text[pos] == delimiter) return pos
            pos++
        }
        return text.length - 1
    }

    private fun findNumberEnd(text: String, start: Int): Int {
        var pos = start
        var dotSeen = false
        while (pos < text.length) {
            val c = text[pos]
            if (c.isDigit()) { pos++; continue }
            if (c == '.' && !dotSeen && pos + 1 < text.length && text[pos + 1].isDigit()) {
                dotSeen = true; pos++; continue
            }
            if (c in "fFdDlL") return pos + 1
            break
        }
        return pos
    }

    private fun findIdentifierEnd(text: String, start: Int): Int {
        var pos = start + 1
        while (pos < text.length && (text[pos].isJavaIdentifierPart() || text[pos] == '_')) pos++
        return pos
    }

    private fun resolveTokenColor(type: TokenType, theme: EditorTheme): Int {
        return when (type) {
            TokenType.KEYWORD -> theme.syntaxColors.keyword
            TokenType.STRING -> theme.syntaxColors.string
            TokenType.NUMBER -> theme.syntaxColors.number
            TokenType.COMMENT -> theme.syntaxColors.comment
            TokenType.OPERATOR -> theme.syntaxColors.operator
            TokenType.IDENTIFIER -> theme.syntaxColors.identifier
            TokenType.TYPE -> theme.syntaxColors.type
            TokenType.FUNCTION -> theme.syntaxColors.function
            TokenType.PREPROCESSOR -> theme.syntaxColors.preprocessor
            TokenType.BRACKET -> theme.syntaxColors.bracket
            TokenType.PUNCTUATION -> theme.syntaxColors.punctuation
            TokenType.ESCAPE -> theme.syntaxColors.escape
            else -> theme.foreground
        }
    }

    public companion object {
        private const val DEBOUNCE_MS = 150L
    }
}

/**
 * Interface for custom syntax highlighters.
 */
public interface SyntaxHighlighter {
    /**
     * Highlights the given text range and returns tokens.
     *
     * @param text The text to highlight
     * @param startOffset Absolute start offset in the document
     * @param language Language definition
     * @return List of highlight tokens
     */
    public fun highlight(text: String, startOffset: Int, language: LanguageDefinition): List<HighlightToken>
}

/**
 * Regex-based syntax highlighter implementation.
 */
public class RegexHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, startOffset: Int, language: LanguageDefinition): List<HighlightToken> {
        val tokens = mutableListOf<HighlightToken>()
        val sortedRules = language.rules.sortedByDescending { it.priority }

        for (rule in sortedRules) {
            for (match in rule.pattern.findAll(text)) {
                tokens.add(HighlightToken(
                    startOffset + match.range.first,
                    startOffset + match.range.last + 1,
                    rule.tokenType,
                    match.value
                ))
            }
        }

        return tokens.sortedBy { it.start }
    }
}

/**
 * Token-based syntax highlighter that parses into tokens first.
 */
public class TokenHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, startOffset: Int, language: LanguageDefinition): List<HighlightToken> {
        val tokens = mutableListOf<HighlightToken>()
        val processed = BooleanArray(text.length) { false }

        // Apply high-priority regex rules first
        val sortedRules = language.rules.sortedByDescending { it.priority }
        for (rule in sortedRules.filter { it.priority >= 5 }) {
            for (match in rule.pattern.findAll(text)) {
                val start = match.range.first
                val end = match.range.last + 1
                if ((start until end).any { processed[it] }) continue
                tokens.add(HighlightToken(startOffset + start, startOffset + end, rule.tokenType, match.value))
                for (i in start until end) processed[i] = true
            }
        }

        // Tokenize remaining
        var pos = 0
        while (pos < text.length) {
            if (processed[pos]) { pos++; continue }
            val c = text[pos]
            val absPos = startOffset + pos

            when {
                c.isWhitespace() -> pos++
                language.stringDelimiters.contains(c) -> {
                    val end = findStringEnd(text, pos, c, language.escapeChar)
                    tokens.add(HighlightToken(absPos, startOffset + end + 1, TokenType.STRING,
                        text.substring(pos, end + 1)))
                    pos = end + 1
                }
                c.isDigit() -> {
                    var end = pos
                    while (end < text.length && (text[end].isDigit() || text[end] == '.')) end++
                    tokens.add(HighlightToken(absPos, startOffset + end, TokenType.NUMBER,
                        text.substring(pos, end)))
                    pos = end
                }
                c.isJavaIdentifierStart() || c == '_' -> {
                    var end = pos + 1
                    while (end < text.length && (text[end].isJavaIdentifierPart() || text[end] == '_')) end++
                    val word = text.substring(pos, end)
                    val type = if (language.keywords.contains(word)) TokenType.KEYWORD else TokenType.IDENTIFIER
                    tokens.add(HighlightToken(absPos, startOffset + end, type, word))
                    pos = end
                }
                else -> {
                    val type = when (c) {
                        in "(){}[]" -> TokenType.BRACKET
                        in "+-*/%=<>!&|^~?:" -> TokenType.OPERATOR
                        else -> TokenType.PUNCTUATION
                    }
                    tokens.add(HighlightToken(absPos, absPos + 1, type, c.toString()))
                    pos++
                }
            }
        }

        return tokens.sortedBy { it.start }
    }

    private fun findStringEnd(text: String, start: Int, delimiter: Char, escape: Char): Int {
        var pos = start + 1
        while (pos < text.length) {
            if (text[pos] == escape) { pos += 2; continue }
            if (text[pos] == delimiter) return pos
            pos++
        }
        return text.length - 1
    }
}
