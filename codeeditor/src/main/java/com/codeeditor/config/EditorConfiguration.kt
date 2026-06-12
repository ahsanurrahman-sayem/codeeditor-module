package com.codeeditor.config

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import com.codeeditor.R
import com.codeeditor.theme.EditorTheme
import com.codeeditor.theme.EditorThemes
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Immutable configuration for the CodeEditor.
 *
 * Use [Builder] or [CodeEditorConfigurationDSL] to create instances.
 */
public data class EditorConfiguration(
    /** Whether the editor is read-only */
    val readOnly: Boolean = false,

    /** Whether line wrapping is enabled */
    val lineWrapping: Boolean = false,

    /** Number of spaces per tab */
    val tabWidth: Int = 4,

    /** Use soft tabs (spaces) instead of hard tabs */
    val useSoftTabs: Boolean = true,

    /** Visually show tab characters */
    val showTabCharacters: Boolean = false,

    /** Enable automatic indentation */
    val autoIndent: Boolean = true,

    /** Enable smart indentation context detection */
    val smartIndent: Boolean = true,

    /** Whether Vim mode is enabled */
    val vimModeEnabled: Boolean = false,

    /** Show line numbers in gutter */
    val showLineNumbers: Boolean = true,

    /** Show word count */
    val showWordCount: Boolean = false,

    /** Show character count */
    val showCharacterCount: Boolean = false,

    /** Highlight the current line */
    val highlightCurrentLine: Boolean = true,

    /** Highlight trailing whitespace */
    val highlightTrailingSpaces: Boolean = true,

    /** Highlight matching bracket pairs */
    val highlightMatchingBrackets: Boolean = true,

    /** Show minimap */
    val showMinimap: Boolean = false,

    /** Show gutter with line numbers and indicators */
    val showGutter: Boolean = true,

    /** Use regex for search */
    val regexSearch: Boolean = false,

    /** Case-sensitive search */
    val caseSensitive: Boolean = false,

    /** Whole word search only */
    val wholeWord: Boolean = false,

    /** Highlight all search matches */
    val highlightAllMatches: Boolean = true,

    /** Enable syntax highlighting */
    val syntaxHighlightingEnabled: Boolean = true,

    /** Language identifier for syntax highlighting */
    val language: String = "",

    /** Editor theme */
    val theme: EditorTheme = EditorThemes.Light,

    /** Text size in SP */
    @Dimension(unit = Dimension.SP) val textSizeSp: Float = 14f,

    /** Line number text size in SP */
    @Dimension(unit = Dimension.SP) val lineNumberTextSizeSp: Float = 12f,

    /** Gutter padding in pixels */
    @Px val gutterPaddingPx: Int = 16,

    /** Color for current line highlight */
    @ColorInt val currentLineColor: Int = theme.currentLineBackground,

    /** Color for selection highlight */
    @ColorInt val selectionColor: Int = theme.selectionBackground,

    /** Gutter background color */
    @ColorInt val gutterBackgroundColor: Int = theme.gutterBackground,

    /** Gutter divider line color */
    @ColorInt val gutterDividerColor: Int = theme.gutterDivider,

    /** Color for trailing space indicators */
    @ColorInt val trailingSpaceColor: Int = theme.trailingSpace,

    /** Color for matching bracket highlights */
    @ColorInt val matchingBracketColor: Int = theme.matchingBracket,

    /** Minimap width in pixels */
    @Px val minimapWidthPx: Int = 100,

    /** Default file encoding */
    val defaultEncoding: Charset = StandardCharsets.UTF_8,

    /** Enable bracket auto-closing pairs */
    val autoClosePairs: Boolean = true,

    /** Pairs of characters that auto-close */
    val autoClosePairsMap: Map<Char, Char> = mapOf(
        '(' to ')',
        '{' to '}',
        '[' to ']',
        '"' to '"',
        '\'' to '\''
    )
) {
    /**
     * Builder for creating [EditorConfiguration] instances.
     */
    public class Builder {
        private var readOnly: Boolean = false
        private var lineWrapping: Boolean = false
        private var tabWidth: Int = 4
        private var useSoftTabs: Boolean = true
        private var showTabCharacters: Boolean = false
        private var autoIndent: Boolean = true
        private var smartIndent: Boolean = true
        private var vimModeEnabled: Boolean = false
        private var showLineNumbers: Boolean = true
        private var showWordCount: Boolean = false
        private var showCharacterCount: Boolean = false
        private var highlightCurrentLine: Boolean = true
        private var highlightTrailingSpaces: Boolean = true
        private var highlightMatchingBrackets: Boolean = true
        private var showMinimap: Boolean = false
        private var showGutter: Boolean = true
        private var regexSearch: Boolean = false
        private var caseSensitive: Boolean = false
        private var wholeWord: Boolean = false
        private var highlightAllMatches: Boolean = true
        private var syntaxHighlightingEnabled: Boolean = true
        private var language: String = ""
        private var theme: EditorTheme = EditorThemes.Light
        private var textSizeSp: Float = 14f
        private var lineNumberTextSizeSp: Float = 12f
        private var gutterPaddingPx: Int = 16
        private var currentLineColor: Int? = null
        private var selectionColor: Int? = null
        private var gutterBackgroundColor: Int? = null
        private var gutterDividerColor: Int? = null
        private var trailingSpaceColor: Int? = null
        private var matchingBracketColor: Int? = null
        private var minimapWidthPx: Int = 100
        private var defaultEncoding: Charset = StandardCharsets.UTF_8
        private var autoClosePairs: Boolean = true
        private var autoClosePairsMap: Map<Char, Char> = mapOf(
            '(' to ')',
            '{' to '}',
            '[' to ']',
            '"' to '"',
            '\'' to '\''
        )

        public fun readOnly(value: Boolean): Builder = apply { readOnly = value }
        public fun lineWrapping(value: Boolean): Builder = apply { lineWrapping = value }
        public fun tabWidth(value: Int): Builder = apply { tabWidth = value }
        public fun useSoftTabs(value: Boolean): Builder = apply { useSoftTabs = value }
        public fun showTabCharacters(value: Boolean): Builder = apply { showTabCharacters = value }
        public fun autoIndent(value: Boolean): Builder = apply { autoIndent = value }
        public fun smartIndent(value: Boolean): Builder = apply { smartIndent = value }
        public fun vimModeEnabled(value: Boolean): Builder = apply { vimModeEnabled = value }
        public fun showLineNumbers(value: Boolean): Builder = apply { showLineNumbers = value }
        public fun showWordCount(value: Boolean): Builder = apply { showWordCount = value }
        public fun showCharacterCount(value: Boolean): Builder = apply { showCharacterCount = value }
        public fun highlightCurrentLine(value: Boolean): Builder = apply { highlightCurrentLine = value }
        public fun highlightTrailingSpaces(value: Boolean): Builder = apply { highlightTrailingSpaces = value }
        public fun highlightMatchingBrackets(value: Boolean): Builder = apply { highlightMatchingBrackets = value }
        public fun showMinimap(value: Boolean): Builder = apply { showMinimap = value }
        public fun showGutter(value: Boolean): Builder = apply { showGutter = value }
        public fun regexSearch(value: Boolean): Builder = apply { regexSearch = value }
        public fun caseSensitive(value: Boolean): Builder = apply { caseSensitive = value }
        public fun wholeWord(value: Boolean): Builder = apply { wholeWord = value }
        public fun highlightAllMatches(value: Boolean): Builder = apply { highlightAllMatches = value }
        public fun syntaxHighlightingEnabled(value: Boolean): Builder = apply { syntaxHighlightingEnabled = value }
        public fun language(value: String): Builder = apply { language = value }
        public fun theme(value: EditorTheme): Builder = apply { theme = value }
        public fun textSizeSp(value: Float): Builder = apply { textSizeSp = value }
        public fun lineNumberTextSizeSp(value: Float): Builder = apply { lineNumberTextSizeSp = value }
        public fun gutterPaddingPx(value: Int): Builder = apply { gutterPaddingPx = value }
        public fun currentLineColor(@ColorInt value: Int): Builder = apply { currentLineColor = value }
        public fun selectionColor(@ColorInt value: Int): Builder = apply { selectionColor = value }
        public fun gutterBackgroundColor(@ColorInt value: Int): Builder = apply { gutterBackgroundColor = value }
        public fun gutterDividerColor(@ColorInt value: Int): Builder = apply { gutterDividerColor = value }
        public fun trailingSpaceColor(@ColorInt value: Int): Builder = apply { trailingSpaceColor = value }
        public fun matchingBracketColor(@ColorInt value: Int): Builder = apply { matchingBracketColor = value }
        public fun minimapWidthPx(value: Int): Builder = apply { minimapWidthPx = value }
        public fun defaultEncoding(value: Charset): Builder = apply { defaultEncoding = value }
        public fun autoClosePairs(value: Boolean): Builder = apply { autoClosePairs = value }
        public fun autoClosePairsMap(value: Map<Char, Char>): Builder = apply { autoClosePairsMap = value }

        public fun build(): EditorConfiguration = EditorConfiguration(
            readOnly = readOnly,
            lineWrapping = lineWrapping,
            tabWidth = tabWidth,
            useSoftTabs = useSoftTabs,
            showTabCharacters = showTabCharacters,
            autoIndent = autoIndent,
            smartIndent = smartIndent,
            vimModeEnabled = vimModeEnabled,
            showLineNumbers = showLineNumbers,
            showWordCount = showWordCount,
            showCharacterCount = showCharacterCount,
            highlightCurrentLine = highlightCurrentLine,
            highlightTrailingSpaces = highlightTrailingSpaces,
            highlightMatchingBrackets = highlightMatchingBrackets,
            showMinimap = showMinimap,
            showGutter = showGutter,
            regexSearch = regexSearch,
            caseSensitive = caseSensitive,
            wholeWord = wholeWord,
            highlightAllMatches = highlightAllMatches,
            syntaxHighlightingEnabled = syntaxHighlightingEnabled,
            language = language,
            theme = theme,
            textSizeSp = textSizeSp,
            lineNumberTextSizeSp = lineNumberTextSizeSp,
            gutterPaddingPx = gutterPaddingPx,
            currentLineColor = currentLineColor ?: theme.currentLineBackground,
            selectionColor = selectionColor ?: theme.selectionBackground,
            gutterBackgroundColor = gutterBackgroundColor ?: theme.gutterBackground,
            gutterDividerColor = gutterDividerColor ?: theme.gutterDivider,
            trailingSpaceColor = trailingSpaceColor ?: theme.trailingSpace,
            matchingBracketColor = matchingBracketColor ?: theme.matchingBracket,
            minimapWidthPx = minimapWidthPx,
            defaultEncoding = defaultEncoding,
            autoClosePairs = autoClosePairs,
            autoClosePairsMap = autoClosePairsMap
        )
    }

    public companion object {
        /**
         * Creates configuration from XML attributes.
         */
        @JvmStatic
        public fun fromAttributes(context: Context, attrs: AttributeSet?): EditorConfiguration {
            if (attrs == null) return EditorConfiguration()

            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CodeEditor)
            return try {
                val themeValue = typedArray.getInt(R.styleable.CodeEditor_ce_theme, 0)
                Builder()
                    .readOnly(typedArray.getBoolean(R.styleable.CodeEditor_ce_readOnly, false))
                    .lineWrapping(typedArray.getBoolean(R.styleable.CodeEditor_ce_lineWrapping, false))
                    .tabWidth(typedArray.getInt(R.styleable.CodeEditor_ce_tabWidth, 4))
                    .useSoftTabs(typedArray.getBoolean(R.styleable.CodeEditor_ce_useSoftTabs, true))
                    .showTabCharacters(typedArray.getBoolean(R.styleable.CodeEditor_ce_showTabCharacters, false))
                    .autoIndent(typedArray.getBoolean(R.styleable.CodeEditor_ce_autoIndent, true))
                    .smartIndent(typedArray.getBoolean(R.styleable.CodeEditor_ce_smartIndent, true))
                    .vimModeEnabled(typedArray.getBoolean(R.styleable.CodeEditor_ce_vimModeEnabled, false))
                    .showLineNumbers(typedArray.getBoolean(R.styleable.CodeEditor_ce_showLineNumbers, true))
                    .showWordCount(typedArray.getBoolean(R.styleable.CodeEditor_ce_showWordCount, false))
                    .showCharacterCount(typedArray.getBoolean(R.styleable.CodeEditor_ce_showCharacterCount, false))
                    .highlightCurrentLine(typedArray.getBoolean(R.styleable.CodeEditor_ce_highlightCurrentLine, true))
                    .highlightTrailingSpaces(typedArray.getBoolean(R.styleable.CodeEditor_ce_highlightTrailingSpaces, true))
                    .highlightMatchingBrackets(typedArray.getBoolean(R.styleable.CodeEditor_ce_highlightMatchingBrackets, true))
                    .showMinimap(typedArray.getBoolean(R.styleable.CodeEditor_ce_showMinimap, false))
                    .showGutter(typedArray.getBoolean(R.styleable.CodeEditor_ce_showGutter, true))
                    .regexSearch(typedArray.getBoolean(R.styleable.CodeEditor_ce_regexSearch, false))
                    .caseSensitive(typedArray.getBoolean(R.styleable.CodeEditor_ce_caseSensitive, false))
                    .wholeWord(typedArray.getBoolean(R.styleable.CodeEditor_ce_wholeWord, false))
                    .highlightAllMatches(typedArray.getBoolean(R.styleable.CodeEditor_ce_highlightAllMatches, true))
                    .syntaxHighlightingEnabled(typedArray.getBoolean(R.styleable.CodeEditor_ce_syntaxHighlightingEnabled, true))
                    .language(typedArray.getString(R.styleable.CodeEditor_ce_language) ?: "")
                    .theme(if (themeValue == 1) EditorThemes.Dark else EditorThemes.Light)
                    .textSizeSp(typedArray.getDimension(R.styleable.CodeEditor_ce_textSize, 14f))
                    .lineNumberTextSizeSp(typedArray.getDimension(R.styleable.CodeEditor_ce_lineNumberTextSize, 12f))
                    .gutterPaddingPx(typedArray.getDimensionPixelSize(R.styleable.CodeEditor_ce_lineNumberPadding, 16))
                    .currentLineColor(typedArray.getColor(R.styleable.CodeEditor_ce_currentLineColor, 0))
                    .selectionColor(typedArray.getColor(R.styleable.CodeEditor_ce_selectionColor, 0))
                    .gutterBackgroundColor(typedArray.getColor(R.styleable.CodeEditor_ce_gutterBackgroundColor, 0))
                    .gutterDividerColor(typedArray.getColor(R.styleable.CodeEditor_ce_gutterDividerColor, 0))
                    .trailingSpaceColor(typedArray.getColor(R.styleable.CodeEditor_ce_trailingSpaceColor, 0))
                    .matchingBracketColor(typedArray.getColor(R.styleable.CodeEditor_ce_matchingBracketColor, 0))
                    .minimapWidthPx(typedArray.getDimensionPixelSize(R.styleable.CodeEditor_ce_minimapWidth, 100))
                    .build()
            } finally {
                typedArray.recycle()
            }
        }
    }
}

/**
 * DSL-style configuration builder.
 */
public inline fun codeEditorConfiguration(block: EditorConfiguration.Builder.() -> Unit): EditorConfiguration {
    return EditorConfiguration.Builder().apply(block).build()
}
