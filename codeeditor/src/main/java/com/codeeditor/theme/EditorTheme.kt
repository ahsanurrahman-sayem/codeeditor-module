package com.codeeditor.theme

import androidx.annotation.ColorInt

/**
 * Color theme for the code editor.
 *
 * @property name Theme name identifier
 * @property background Main editor background color
 * @property foreground Main text color
 * @property currentLineBackground Color for highlighting the current line
 * @property selectionBackground Color for text selection
 * @property gutterBackground Gutter background color
 * @property gutterForeground Gutter text color
 * @property gutterDivider Divider line color between gutter and editor
 * @property lineNumberColor Line number text color
 * @property cursorColor Cursor indicator color
 * @property trailingSpace Color for trailing space indicators
 * @property matchingBracket Color for matching bracket highlights
 * @property syntaxColors Map of token types to colors
 */
public data class EditorTheme(
    val name: String,
    @ColorInt val background: Int,
    @ColorInt val foreground: Int,
    @ColorInt val currentLineBackground: Int,
    @ColorInt val selectionBackground: Int,
    @ColorInt val gutterBackground: Int,
    @ColorInt val gutterForeground: Int,
    @ColorInt val gutterDivider: Int,
    @ColorInt val lineNumberColor: Int,
    @ColorInt val activeLineNumberColor: Int,
    @ColorInt val cursorColor: Int,
    @ColorInt val trailingSpace: Int,
    @ColorInt val matchingBracket: Int,
    @ColorInt val bracketMatchBackground: Int,
    @ColorInt val minimapBackground: Int,
    @ColorInt val minimapViewport: Int,
    val syntaxColors: SyntaxColors
)

/**
 * Syntax highlighting colors for different token types.
 */
public data class SyntaxColors(
    @ColorInt val keyword: Int,
    @ColorInt val string: Int,
    @ColorInt val number: Int,
    @ColorInt val comment: Int,
    @ColorInt val operator: Int,
    @ColorInt val identifier: Int,
    @ColorInt val type: Int,
    @ColorInt val function: Int,
    @ColorInt val preprocessor: Int,
    @ColorInt val bracket: Int,
    @ColorInt val punctuation: Int,
    @ColorInt val escape: Int
)

/**
 * Pre-built editor themes.
 */
public object EditorThemes {
    /** Light theme */
    public val Light: EditorTheme = EditorTheme(
        name = "light",
        background = 0xFFFFFFFF.toInt(),
        foreground = 0xFF2E2E2E.toInt(),
        currentLineBackground = 0xFFF5F5F5.toInt(),
        selectionBackground = 0xFFADD6FF.toInt(),
        gutterBackground = 0xFFF5F5F5.toInt(),
        gutterForeground = 0xFF8E8E8E.toInt(),
        gutterDivider = 0xFFE0E0E0.toInt(),
        lineNumberColor = 0xFF8E8E8E.toInt(),
        activeLineNumberColor = 0xFF2E2E2E.toInt(),
        cursorColor = 0xFF000000.toInt(),
        trailingSpace = 0xFFE0E0E0.toInt(),
        matchingBracket = 0xFFFF0000.toInt(),
        bracketMatchBackground = 0xFFE0E8F0.toInt(),
        minimapBackground = 0xFFF0F0F0.toInt(),
        minimapViewport = 0x40000000.toInt(),
        syntaxColors = SyntaxColors(
            keyword = 0xFF0000FF.toInt(),
            string = 0xFF008000.toInt(),
            number = 0xFFFF0000.toInt(),
            comment = 0xFF808080.toInt(),
            operator = 0xFF2E2E2E.toInt(),
            identifier = 0xFF2E2E2E.toInt(),
            type = 0xFF267988.toInt(),
            function = 0xFF795E26.toInt(),
            preprocessor = 0xFF808000.toInt(),
            bracket = 0xFF2E2E2E.toInt(),
            punctuation = 0xFF2E2E2E.toInt(),
            escape = 0xFF0000FF.toInt()
        )
    )

    /** Dark theme */
    public val Dark: EditorTheme = EditorTheme(
        name = "dark",
        background = 0xFF1E1E1E.toInt(),
        foreground = 0xFFD4D4D4.toInt(),
        currentLineBackground = 0xFF2A2D2E.toInt(),
        selectionBackground = 0xFF264F78.toInt(),
        gutterBackground = 0xFF1E1E1E.toInt(),
        gutterForeground = 0xFF858585.toInt(),
        gutterDivider = 0xFF333333.toInt(),
        lineNumberColor = 0xFF858585.toInt(),
        activeLineNumberColor = 0xFFD4D4D4.toInt(),
        cursorColor = 0xFFFFFFFF.toInt(),
        trailingSpace = 0xFF404040.toInt(),
        matchingBracket = 0xFFFF0000.toInt(),
        bracketMatchBackground = 0xFF3A3D3E.toInt(),
        minimapBackground = 0xFF1E1E1E.toInt(),
        minimapViewport = 0x40FFFFFF.toInt(),
        syntaxColors = SyntaxColors(
            keyword = 0xFF569CD6.toInt(),
            string = 0xFFCE9178.toInt(),
            number = 0xFFB5CEA8.toInt(),
            comment = 0xFF6A9955.toInt(),
            operator = 0xFFD4D4D4.toInt(),
            identifier = 0xFFD4D4D4.toInt(),
            type = 0xFF4EC9B0.toInt(),
            function = 0xFFDCDCAA.toInt(),
            preprocessor = 0xFF9B9B9B.toInt(),
            bracket = 0xFFD4D4D4.toInt(),
            punctuation = 0xFFD4D4D4.toInt(),
            escape = 0xFFD7BA7D.toInt()
        )
    )

    /** Dracula theme */
    public val Dracula: EditorTheme = EditorTheme(
        name = "dracula",
        background = 0xFF282A36.toInt(),
        foreground = 0xFFF8F8F2.toInt(),
        currentLineBackground = 0xFF44475A.toInt(),
        selectionBackground = 0xFF44475A.toInt(),
        gutterBackground = 0xFF282A36.toInt(),
        gutterForeground = 0xFF6272A4.toInt(),
        gutterDivider = 0xFF44475A.toInt(),
        lineNumberColor = 0xFF6272A4.toInt(),
        activeLineNumberColor = 0xFFF8F8F2.toInt(),
        cursorColor = 0xFFF8F8F2.toInt(),
        trailingSpace = 0xFF44475A.toInt(),
        matchingBracket = 0xFFFF79C6.toInt(),
        bracketMatchBackground = 0xFF44475A.toInt(),
        minimapBackground = 0xFF282A36.toInt(),
        minimapViewport = 0x40F8F8F2.toInt(),
        syntaxColors = SyntaxColors(
            keyword = 0xFFFF79C6.toInt(),
            string = 0xFFF1FA8C.toInt(),
            number = 0xFFBD93F9.toInt(),
            comment = 0xFF6272A4.toInt(),
            operator = 0xFFFF79C6.toInt(),
            identifier = 0xFFF8F8F2.toInt(),
            type = 0xFF8BE9FD.toInt(),
            function = 0xFF50FA7B.toInt(),
            preprocessor = 0xFFFF79C6.toInt(),
            bracket = 0xFFF8F8F2.toInt(),
            punctuation = 0xFFF8F8F2.toInt(),
            escape = 0xFFFF79C6.toInt()
        )
    )

    private val themes = mutableMapOf(
        "light" to Light,
        "dark" to Dark,
        "dracula" to Dracula
    )

    /**
     * Registers a custom theme.
     */
    @JvmStatic
    public fun registerTheme(name: String, theme: EditorTheme) {
        themes[name] = theme
    }

    /**
     * Gets a theme by name.
     */
    @JvmStatic
    public fun getTheme(name: String): EditorTheme = themes[name] ?: Light

    /**
     * Lists all available theme names.
     */
    @JvmStatic
    public fun availableThemes(): Set<String> = themes.keys.toSet()
}

/**
 * Manager for runtime theme switching.
 */
public class ThemeManager {
    private var currentTheme: EditorTheme = EditorThemes.Light
    private val listeners = mutableListOf<(EditorTheme) -> Unit>()

    /** The currently active theme */
    public val theme: EditorTheme get() = currentTheme

    /** Syntax colors from current theme */
    public val syntaxColors: SyntaxColors get() = currentTheme.syntaxColors

    /**
     * Sets the active theme and notifies listeners.
     */
    public fun setTheme(theme: EditorTheme) {
        currentTheme = theme
        listeners.forEach { it(theme) }
    }

    /**
     * Sets theme by name.
     */
    public fun setTheme(name: String) {
        setTheme(EditorThemes.getTheme(name))
    }

    /**
     * Registers a theme change listener.
     */
    public fun addListener(listener: (EditorTheme) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Removes a theme change listener.
     */
    public fun removeListener(listener: (EditorTheme) -> Unit) {
        listeners.remove(listener)
    }
}
