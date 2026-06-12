package com.codeeditor.interop

import android.content.Context
import android.util.AttributeSet
import com.codeeditor.CodeEditor
import com.codeeditor.config.EditorConfiguration
import com.codeeditor.highlighting.LanguageDefinition
import com.codeeditor.highlighting.LanguageRegistry
import com.codeeditor.theme.EditorTheme
import com.codeeditor.theme.EditorThemes
import com.codeeditor.vim.VimAction
import com.codeeditor.vim.VimEngine

/**
 * Java-friendly factory and configuration methods for CodeEditor.
 *
 * ```java
 * CodeEditor editor = CodeEditorFactory.create(context);
 * CodeEditorFactory.configure(editor, config ->
 *     config.language("java")
 *           .theme(EditorThemes.Light)
 *           .showLineNumbers(true)
 * );
 * ```
 */
public object CodeEditorFactory {
    /**
     * Creates a new CodeEditor instance.
     */
    @JvmStatic
    public fun create(context: Context): CodeEditor {
        return CodeEditor(context)
    }

    /**
     * Creates a CodeEditor with XML attributes.
     */
    @JvmStatic
    public fun create(context: Context, attrs: AttributeSet?): CodeEditor {
        return CodeEditor(context, attrs)
    }

    /**
     * Configures a CodeEditor using a Java-friendly builder callback.
     */
    @JvmStatic
    public fun configure(editor: CodeEditor, block: EditorConfigurationBuilder) {
        val builder = EditorConfiguration.Builder()
        block.configure(builder)
        editor.configure { 
            readOnly(builder.build().readOnly)
            lineWrapping(builder.build().lineWrapping)
            tabWidth(builder.build().tabWidth)
            useSoftTabs(builder.build().useSoftTabs)
            showTabCharacters(builder.build().showTabCharacters)
            autoIndent(builder.build().autoIndent)
            smartIndent(builder.build().smartIndent)
            vimModeEnabled(builder.build().vimModeEnabled)
            showLineNumbers(builder.build().showLineNumbers)
            showWordCount(builder.build().showWordCount)
            showCharacterCount(builder.build().showCharacterCount)
            highlightCurrentLine(builder.build().highlightCurrentLine)
            highlightTrailingSpaces(builder.build().highlightTrailingSpaces)
            highlightMatchingBrackets(builder.build().highlightMatchingBrackets)
            showMinimap(builder.build().showMinimap)
            showGutter(builder.build().showGutter)
            regexSearch(builder.build().regexSearch)
            caseSensitive(builder.build().caseSensitive)
            wholeWord(builder.build().wholeWord)
            highlightAllMatches(builder.build().highlightAllMatches)
            syntaxHighlightingEnabled(builder.build().syntaxHighlightingEnabled)
            language(builder.build().language)
            theme(builder.build().theme)
        }
    }

    /**
     * Sets language by name (Java-friendly).
     */
    @JvmStatic
    public fun setLanguage(editor: CodeEditor, languageName: String) {
        editor.setLanguageByName(languageName)
    }

    /**
     * Sets theme by name (Java-friendly).
     */
    @JvmStatic
    public fun setTheme(editor: CodeEditor, themeName: String) {
        editor.setTheme(themeName)
    }

    /**
     * Enables Vim mode.
     */
    @JvmStatic
    public fun enableVim(editor: CodeEditor) {
        editor.enableVimMode()
    }

    /**
     * Disables Vim mode.
     */
    @JvmStatic
    public fun disableVim(editor: CodeEditor) {
        editor.disableVimMode()
    }
}

/**
 * Java functional interface for editor configuration.
 */
public fun interface EditorConfigurationBuilder {
    public fun configure(builder: EditorConfiguration.Builder)
}

/**
 * Java-friendly Vim configuration helper.
 */
public object VimConfigHelper {
    /**
     * Maps a key sequence to an action.
     */
    @JvmStatic
    public fun mapKey(vimEngine: VimEngine, keys: String, action: VimAction) {
        vimEngine.mapKey(keys, action)
    }

    /**
     * Sets a named register.
     */
    @JvmStatic
    public fun setRegister(vimEngine: VimEngine, name: Char, content: String) {
        vimEngine.setRegister(name, content)
    }

    /**
     * Gets a named register value.
     */
    @JvmStatic
    public fun getRegister(vimEngine: VimEngine, name: Char): String? {
        return vimEngine.getRegister(name)
    }
}

/**
 * Java-friendly language detection helper.
 */
public object LanguageDetectionHelper {
    /**
     * Detects language from filename.
     */
    @JvmStatic
    public fun detectFromFilename(filename: String): String? {
        return LanguageRegistry.detectFromExtension(filename)?.name
    }

    /**
     * Checks if a language is supported.
     */
    @JvmStatic
    public fun isLanguageSupported(name: String): Boolean {
        return LanguageRegistry.getLanguage(name) != null
    }

    /**
     * Gets all supported language names.
     */
    @JvmStatic
    public fun getSupportedLanguages(): List<String> {
        return LanguageRegistry.availableLanguages().toList()
    }
}

/**
 * Java-friendly theme helper.
 */
public object ThemeHelper {
    /**
     * Gets available theme names.
     */
    @JvmStatic
    public fun getAvailableThemes(): List<String> {
        return EditorThemes.availableThemes().toList()
    }

    /**
     * Gets a theme by name.
     */
    @JvmStatic
    public fun getTheme(name: String): EditorTheme {
        return EditorThemes.getTheme(name)
    }
}
