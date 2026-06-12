# CodeEditor Integration Guide

## Overview

The CodeEditor library provides a production-ready code editing component for Android with syntax highlighting, Vim keybindings, search/replace, theming, and extensive customization.

## Setup

### Gradle Dependency

```kotlin
dependencies {
    implementation(project(":codeeditor"))
    // Or after publishing:
    // implementation("com.codeeditor:codeeditor:1.0.0")
}
```

### Minimum Requirements

- Android SDK 24+
- Material 3
- Kotlin 1.9+

## Basic Usage

### XML Layout

```xml
<com.codeeditor.CodeEditor
    android:id="@+id/editor"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:ce_language="kotlin"
    app:ce_theme="dark"
    app:ce_showLineNumbers="true"
    app:ce_vimModeEnabled="false"
    app:ce_tabWidth="4"
    app:ce_useSoftTabs="true" />
```

### Kotlin - Programmatic

```kotlin
val editor = CodeEditor(context)

// Configure
editor.configure {
    language("kotlin")
    theme(EditorThemes.Dark)
    showLineNumbers(true)
    showGutter(true)
    vimModeEnabled(false)
    tabWidth(4)
    useSoftTabs(true)
    autoIndent(true)
    highlightCurrentLine(true)
    highlightMatchingBrackets(true)
}

// Set content
editor.setText("""
    fun main() {
        println("Hello, World!")
    }
""".trimIndent())

// Navigate
editor.goToLine(0)

// Search
editor.showFind()
editor.search("println")
editor.findNext()

// Undo/Redo
editor.undo()
editor.redo()
```

### Java - Programmatic

```java
CodeEditor editor = CodeEditorFactory.create(context);

CodeEditorFactory.configure(editor, config ->
    config.language("java")
          .theme(EditorThemes.Light)
          .showLineNumbers(true)
          .tabWidth(4)
);

editor.setText("public class Main { }");
```

## Jetpack Compose

```kotlin
@Composable
fun EditorScreen() {
    val editorState = rememberCodeEditorState()
    
    CodeEditorCompose(
        editorState = editorState,
        modifier = Modifier.fillMaxSize(),
        initialText = "fun main() {\n    println()\n}",
        language = "kotlin",
        theme = EditorThemes.Dark,
        showLineNumbers = true,
        vimModeEnabled = false,
        onTextChange = { text ->
            // Handle text changes
        }
    )
    
    // Control externally
    Button(onClick = { editorState.undo() }) {
        Text("Undo")
    }
}
```

## Features

### Syntax Highlighting

```kotlin
// Auto-detect from filename
editor.detectLanguage("MainActivity.kt")

// Set explicitly
editor.setLanguageByName("kotlin")

// Custom language
val lang = LanguageDefinition(
    name = "custom",
    fileExtensions = listOf("custom"),
    keywords = setOf("if", "else", "while"),
    singleLineComment = "//",
    multiLineCommentStart = "/*",
    multiLineCommentEnd = "*/",
    stringDelimiters = setOf('"')
)
editor.setLanguage(lang)
```

### Vim Keybindings

```kotlin
// Enable/disable
editor.enableVimMode()
editor.disableVimMode()
editor.toggleVimMode()

// Advanced configuration
val vim = editor.getVimEngine()

// Custom mappings
vim.mapKey("jj", VimAction.EXIT_INSERT_MODE)
vim.mapKey("<Space>w", VimAction.SAVE)

// Registers
vim.setRegister('a', "stored text")
val content = vim.getRegister('a')

// Mode monitoring
vim.onModeChanged = { mode ->
    when (mode) {
        VimMode.NORMAL -> showStatus("NORMAL")
        VimMode.INSERT -> showStatus("INSERT")
        VimMode.VISUAL -> showStatus("VISUAL")
        VimMode.COMMAND -> showStatus("COMMAND")
    }
}
```

### Search and Replace

```kotlin
// Find
editor.showFind()
val results = editor.search("pattern")

// Find/Replace
editor.showFindReplace()
val count = editor.replaceAll("old", "new")

// Advanced search
import com.codeeditor.search.SearchOptions
val results = editor.search("pattern", SearchOptions(
    regex = true,
    caseSensitive = true,
    wholeWord = true
))
```

### Theming

```kotlin
// Built-in themes
editor.setTheme(EditorThemes.Light)
editor.setTheme(EditorThemes.Dark)
editor.setTheme(EditorThemes.Dracula)
editor.setTheme("dracula")

// Custom theme
val customTheme = EditorTheme(
    name = "mytheme",
    background = Color(0xFF1A1A2E).toArgb(),
    foreground = Color(0xFFEEEEEE).toArgb(),
    // ... all colors
    syntaxColors = SyntaxColors(
        keyword = Color(0xFFFF79C6).toArgb(),
        string = Color(0xFFF1FA8C).toArgb(),
        // ... all token colors
    )
)

EditorThemes.registerTheme("mytheme", customTheme)
editor.setTheme("mytheme")
```

### File Operations

```kotlin
val fileHandler = FileHandler(context)

// Load from SAF URI
lifecycleScope.launch {
    val content = fileHandler.loadFromUri(uri)
    editor.setText(content)
}

// Save to URI
lifecycleScope.launch {
    fileHandler.saveToUri(uri, editor.getTextString())
}

// Large file loading with progress
lifecycleScope.launch {
    fileHandler.loadLargeFile(uri) { read, total ->
        progressBar.progress = (read * 100 / total).toInt()
    }
}

// Encoding detection
val charset = fileHandler.detectEncoding(uri)
editor.setEncoding(charset)
```

## Configuration Options

| Option | XML Attribute | Default | Description |
|--------|--------------|---------|-------------|
| Read Only | `ce_readOnly` | false | Disables editing |
| Line Wrapping | `ce_lineWrapping` | false | Wraps long lines |
| Tab Width | `ce_tabWidth` | 4 | Spaces per tab |
| Soft Tabs | `ce_useSoftTabs` | true | Use spaces for tabs |
| Show Tabs | `ce_showTabCharacters` | false | Visual tab indicators |
| Auto Indent | `ce_autoIndent` | true | Automatic indentation |
| Smart Indent | `ce_smartIndent` | true | Context-aware indent |
| Vim Mode | `ce_vimModeEnabled` | false | Enable Vim bindings |
| Line Numbers | `ce_showLineNumbers` | true | Show line numbers |
| Word Count | `ce_showWordCount` | false | Show word count |
| Char Count | `ce_showCharacterCount` | false | Show character count |
| Current Line | `ce_highlightCurrentLine` | true | Highlight current line |
| Trailing Spaces | `ce_highlightTrailingSpaces` | true | Show trailing spaces |
| Brackets | `ce_highlightMatchingBrackets` | true | Match bracket pairs |
| Minimap | `ce_showMinimap` | false | Show code minimap |
| Gutter | `ce_showGutter` | true | Show gutter panel |
| Regex Search | `ce_regexSearch` | false | Regex in search |
| Case Sensitive | `ce_caseSensitive` | false | Case-sensitive search |
| Whole Word | `ce_wholeWord` | false | Whole word search |
| Highlight Matches | `ce_highlightAllMatches` | true | Highlight all matches |
| Syntax Highlight | `ce_syntaxHighlightingEnabled` | true | Enable highlighting |
| Language | `ce_language` | "" | Language identifier |
| Theme | `ce_theme` | light | Editor theme |

## Architecture

```
CodeEditor (AppCompatEditText)
  ├── TextBuffer (Gap Buffer)
  ├── MultiCursorManager
  ├── LineMetricsManager
  ├── HighlightingEngine
  │     ├── RegexHighlighter
  │     └── TokenHighlighter
  ├── VimEngine
  │     ├── VimCommandParser
  │     ├── VimRegisters
  │     └── VimMappings
  ├── SearchEngine
  ├── ThemeManager
  ├── FileHandler
  └── EncodingManager
```

## Thread Safety

- TextBuffer: Thread-safe with coroutine mutex
- HighlightingEngine: Background processing with debouncing
- All UI operations must be on Main thread
- File operations use IO dispatcher

## Performance

- Gap buffer for efficient edits
- Incremental syntax highlighting
- Visible-region-first processing
- Background parsing with coroutines
- Cached line metrics
- Debounced text change handling
