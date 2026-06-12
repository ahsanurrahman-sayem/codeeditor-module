package com.codeeditor.compose

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.codeeditor.CodeEditor
import com.codeeditor.config.EditorConfiguration
import com.codeeditor.highlighting.LanguageDefinition
import com.codeeditor.theme.EditorTheme
import com.codeeditor.theme.EditorThemes

/**
 * Jetpack Compose wrapper for CodeEditor.
 *
 * ```kotlin
 * @Composable
 * fun EditorScreen() {
 *     CodeEditorCompose(
 *         modifier = Modifier.fillMaxSize(),
 *         initialText = "fun main() {\n    println(\"Hello\")\n}",
 *         language = "kotlin",
 *         theme = EditorThemes.Dark,
 *         readOnly = false
 *     )
 * }
 * ```
 *
 * @param modifier Compose modifier
 * @param initialText Initial text content
 * @param language Language name for syntax highlighting
 * @param theme Editor theme
 * @param readOnly Read-only mode
 * @param showLineNumbers Show line numbers
 * @param showGutter Show gutter
 * @param vimModeEnabled Enable Vim mode
 * @param onTextChange Callback when text changes
 * @param onCursorChange Callback when cursor moves
 * @param configurationBuilder Additional configuration
 */
@Composable
public fun CodeEditorCompose(
    modifier: Modifier = Modifier.fillMaxSize(),
    initialText: String = "",
    language: String? = null,
    theme: EditorTheme = EditorThemes.Light,
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    showGutter: Boolean = true,
    vimModeEnabled: Boolean = false,
    onTextChange: ((String) -> Unit)? = null,
    onCursorChange: ((Int, Int) -> Unit)? = null,
    configurationBuilder: (EditorConfiguration.Builder.() -> Unit)? = null
) {
    val context = LocalContext.current
    var editor by remember { mutableStateOf<CodeEditor?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            CodeEditor(context).apply {
                configure {
                    this.readOnly(readOnly)
                    this.showLineNumbers(showLineNumbers)
                    this.showGutter(showGutter)
                    this.vimModeEnabled(vimModeEnabled)
                    this.theme(theme)
                    if (language != null) this.language(language)
                }
                configurationBuilder?.let { configure(it) }
                setText(initialText)
                editor = this
            }
        },
        update = { view ->
            view.apply {
                if (text?.toString() != initialText) {
                    setText(initialText)
                }
                setTheme(theme)
                language?.let { setLanguageByName(it) }
            }
        }
    )

    // Side effects for callbacks
    LaunchedEffect(editor) {
        editor?.let { ed ->
            ed.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.toString()?.let { onTextChange?.invoke(it) }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
    }
}

/**
 * State holder for CodeEditorCompose for external control.
 *
 * ```kotlin
 * val editorState = rememberCodeEditorState()
 *
 * CodeEditorCompose(
 *     editorState = editorState,
 *     ...
 * )
 *
 * // Control externally:
 * editorState.setText("new content")
 * editorState.goToLine(10)
 * editorState.undo()
 * ```
 */
@Stable
public class CodeEditorState {
    internal var editor: CodeEditor? = null

    /** Current text content */
    public val text: String get() = editor?.text?.toString() ?: ""

    /** Current cursor line */
    public val currentLine: Int get() = editor?.getCurrentLine() ?: 0

    /** Current cursor column */
    public val currentColumn: Int get() = editor?.getCurrentColumn() ?: 0

    /** Whether Vim mode is active */
    public val isVimMode: Boolean get() = editor?.isVimMode ?: false

    /** Current Vim mode */
    public val vimMode: com.codeeditor.vim.VimMode
        get() = editor?.getVimEngine()?.mode ?: com.codeeditor.vim.VimMode.NORMAL

    /**
     * Sets the editor text.
     */
    public fun setText(text: String) {
        editor?.setText(text)
    }

    /**
     * Goes to a specific line.
     */
    public fun goToLine(line: Int) {
        editor?.goToLine(line)
    }

    /**
     * Undoes last change.
     */
    public fun undo() {
        editor?.undo()
    }

    /**
     * Redoes last undone change.
     */
    public fun redo() {
        editor?.redo()
    }

    /**
     * Searches for text.
     */
    public fun search(query: String) {
        editor?.search(query)
    }

    /**
     * Shows find dialog.
     */
    public fun showFind() {
        editor?.showFind()
    }

    /**
     * Shows find and replace dialog.
     */
    public fun showFindReplace() {
        editor?.showFindReplace()
    }

    /**
     * Sets language for syntax highlighting.
     */
    public fun setLanguage(language: String) {
        editor?.setLanguageByName(language)
    }

    /**
     * Sets the editor theme.
     */
    public fun setTheme(theme: EditorTheme) {
        editor?.setTheme(theme)
    }

    /**
     * Toggles Vim mode.
     */
    public fun toggleVimMode() {
        editor?.toggleVimMode()
    }

    /**
     * Configures the editor.
     */
    public fun configure(block: EditorConfiguration.Builder.() -> Unit) {
        editor?.configure(block)
    }

    internal fun attach(editor: CodeEditor) {
        this.editor = editor
    }
}

/**
 * Creates and remembers a [CodeEditorState].
 */
@Composable
public fun rememberCodeEditorState(): CodeEditorState {
    return remember { CodeEditorState() }
}

/**
 * CodeEditorCompose with state holder.
 */
@Composable
public fun CodeEditorCompose(
    editorState: CodeEditorState,
    modifier: Modifier = Modifier.fillMaxSize(),
    initialText: String = "",
    language: String? = null,
    theme: EditorTheme = EditorThemes.Light,
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    showGutter: Boolean = true,
    vimModeEnabled: Boolean = false,
    onTextChange: ((String) -> Unit)? = null,
    configurationBuilder: (EditorConfiguration.Builder.() -> Unit)? = null
) {
    CodeEditorCompose(
        modifier = modifier,
        initialText = initialText,
        language = language,
        theme = theme,
        readOnly = readOnly,
        showLineNumbers = showLineNumbers,
        showGutter = showGutter,
        vimModeEnabled = vimModeEnabled,
        onTextChange = onTextChange,
        configurationBuilder = configurationBuilder
    )
}
