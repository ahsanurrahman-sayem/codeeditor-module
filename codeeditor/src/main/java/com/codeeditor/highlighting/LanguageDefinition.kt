package com.codeeditor.highlighting

/**
 * Defines a language syntax for highlighting.
 *
 * @property name Language name (e.g., "kotlin", "java", "python")
 * @property fileExtensions Associated file extensions
 * @property rules List of highlighting rules in priority order
 * @property keywords Language-specific keywords
 * @property singleLineComment Single-line comment marker (e.g., "//")
 * @property multiLineCommentStart Multi-line comment start (e.g., "/*")
 * @property multiLineCommentEnd Multi-line comment end (e.g., "*/")
 * @property stringDelimiters Characters that delimit strings
 * @property escapeChar Escape character (typically '\\')
 */
public data class LanguageDefinition(
    val name: String,
    val fileExtensions: List<String>,
    val rules: List<HighlightRule> = emptyList(),
    val keywords: Set<String> = emptySet(),
    val singleLineComment: String? = null,
    val multiLineCommentStart: String? = null,
    val multiLineCommentEnd: String? = null,
    val stringDelimiters: Set<Char> = setOf('"', '\''),
    val escapeChar: Char = '\\'
)

/**
 * A single highlighting rule.
 *
 * @property pattern Regex pattern to match
 * @property tokenType Token type for color lookup
 * @property priority Higher priority rules are applied first
 */
public data class HighlightRule(
    val pattern: Regex,
    val tokenType: TokenType,
    val priority: Int = 0
)

/**
 * Token types for syntax highlighting.
 */
public enum class TokenType {
    KEYWORD,
    STRING,
    NUMBER,
    COMMENT,
    OPERATOR,
    IDENTIFIER,
    TYPE,
    FUNCTION,
    PREPROCESSOR,
    BRACKET,
    PUNCTUATION,
    ESCAPE,
    WHITESPACE,
    UNKNOWN
}

/**
 * A highlighted token in the text.
 *
 * @property start Start position (inclusive)
 * @property end End position (exclusive)
 * @property type Token type
 * @property text The matched text
 */
public data class HighlightToken(
    val start: Int,
    val end: Int,
    val type: TokenType,
    val text: String
)

/**
 * Registry of language definitions.
 */
public object LanguageRegistry {
    private val languages = mutableMapOf<String, LanguageDefinition>()
    private val extensionMap = mutableMapOf<String, LanguageDefinition>()

    init {
        registerBuiltInLanguages()
    }

    /**
     * Registers a language definition.
     */
    @JvmStatic
    public fun register(language: LanguageDefinition) {
        languages[language.name.lowercase()] = language
        language.fileExtensions.forEach { ext ->
            extensionMap[ext.lowercase()] = language
        }
    }

    /**
     * Gets a language by name.
     */
    @JvmStatic
    public fun getLanguage(name: String): LanguageDefinition? {
        return languages[name.lowercase()]
    }

    /**
     * Detects language from file extension.
     */
    @JvmStatic
    public fun detectFromExtension(filename: String): LanguageDefinition? {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return extensionMap[ext]
    }

    /**
     * Lists all registered language names.
     */
    @JvmStatic
    public fun availableLanguages(): Set<String> = languages.keys.toSet()

    private fun registerBuiltInLanguages() {
        register(kotlinLanguage())
        register(javaLanguage())
        register(pythonLanguage())
        register(javascriptLanguage())
        register(xmlLanguage())
        register(jsonLanguage())
        register(vimLanguage())
    }
}

private fun kotlinLanguage() = LanguageDefinition(
    name = "kotlin",
    fileExtensions = listOf("kt", "kts"),
    keywords = setOf(
        "package", "import", "fun", "val", "var", "class", "object", "interface",
        "sealed", "data", "enum", "abstract", "open", "final", "override", "lateinit",
        "init", "constructor", "by", "companion", "in", "out", "where", "when", "if",
        "else", "for", "while", "do", "return", "break", "continue", "throw", "try",
        "catch", "finally", "true", "false", "null", "is", "as", "typeof", "suspend",
        "inline", "noinline", "crossinline", "reified", "operator", "infix", "tailrec",
        "external", "annotation", "const", "vararg", "typealias", "expect", "actual",
        "this", "super", "it"
    ),
    singleLineComment = "//",
    multiLineCommentStart = "/*",
    multiLineCommentEnd = "*/",
    stringDelimiters = setOf('"', '\''),
    rules = listOf(
        HighlightRule("\\b\\d+(\\.\\d+)?(L|F|D)?\\b".toRegex(), TokenType.NUMBER, 10),
        HighlightRule("\\b[A-Z][a-zA-Z0-9_]*\\b".toRegex(), TokenType.TYPE, 5)
    )
)

private fun javaLanguage() = LanguageDefinition(
    name = "java",
    fileExtensions = listOf("java"),
    keywords = setOf(
        "package", "import", "public", "private", "protected", "static", "final",
        "abstract", "class", "interface", "extends", "implements", "void", "return",
        "if", "else", "for", "while", "do", "switch", "case", "default", "break",
        "continue", "new", "this", "super", "try", "catch", "finally", "throw",
        "throws", "instanceof", "true", "false", "null", "enum", "assert",
        "synchronized", "volatile", "transient", "native", "strictfp", "const", "goto"
    ),
    singleLineComment = "//",
    multiLineCommentStart = "/*",
    multiLineCommentEnd = "*/",
    stringDelimiters = setOf('"', '\'')
)

private fun pythonLanguage() = LanguageDefinition(
    name = "python",
    fileExtensions = listOf("py", "pyw"),
    keywords = setOf(
        "and", "as", "assert", "break", "class", "continue", "def", "del", "elif",
        "else", "except", "False", "finally", "for", "from", "global", "if", "import",
        "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise",
        "return", "True", "try", "while", "with", "yield", "async", "await"
    ),
    singleLineComment = "#",
    multiLineCommentStart = "\"\"\"",
    multiLineCommentEnd = "\"\"\"",
    stringDelimiters = setOf('"', '\'')
)

private fun javascriptLanguage() = LanguageDefinition(
    name = "javascript",
    fileExtensions = listOf("js", "jsx", "mjs", "cjs", "ts", "tsx"),
    keywords = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function",
        "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch",
        "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
        "await", "async", "of", "from", "as", "true", "false", "null", "undefined",
        "interface", "type", "enum", "module", "declare", "abstract", "implements",
        "public", "private", "protected", "readonly", "static", "namespace"
    ),
    singleLineComment = "//",
    multiLineCommentStart = "/*",
    multiLineCommentEnd = "*/",
    stringDelimiters = setOf('"', '\'', '`')
)

private fun xmlLanguage() = LanguageDefinition(
    name = "xml",
    fileExtensions = listOf("xml", "html", "htm", "xhtml", "svg"),
    rules = listOf(
        HighlightRule("&lt;\\?[\\s\\S]*?\\?&gt;".toRegex(), TokenType.PREPROCESSOR, 10),
        HighlightRule("&lt;!--[\\s\\S]*?--&gt;".toRegex(), TokenType.COMMENT, 10),
        HighlightRule("&lt;\\/?[\\w:-]+".toRegex(), TokenType.KEYWORD, 8),
        HighlightRule("\\w+=\"[^\"]*\"".toRegex(), TokenType.STRING, 5),
        HighlightRule("\\w+='[^']*'".toRegex(), TokenType.STRING, 5)
    ),
    stringDelimiters = setOf('"', '\'')
)

private fun jsonLanguage() = LanguageDefinition(
    name = "json",
    fileExtensions = listOf("json", "jsonc"),
    keywords = setOf("true", "false", "null"),
    stringDelimiters = setOf('"'),
    rules = listOf(
        HighlightRule("\"[^\"]*\"\\s*:".toRegex(), TokenType.KEYWORD, 10),
        HighlightRule("\\b\\d+(\\.\\d+)?\\b".toRegex(), TokenType.NUMBER, 8)
    )
)

private fun vimLanguage() = LanguageDefinition(
    name = "vim",
    fileExtensions = listOf("vim", "vimrc"),
    keywords = setOf(
        "if", "else", "elseif", "endif", "for", "endfor", "while", "endwhile",
        "function", "endfunction", "return", "let", "unlet", "set", "map", "nmap",
        "vmap", "imap", "cmap", "nnoremap", "vnoremap", "inoremap", "cnoremap",
        "autocmd", "augroup", "au", "execute", "echo", "echom", "call", "try",
        "catch", "finally", "endtry", "throw", "source", "syntax", "highlight",
        "colorscheme", "filetype", "on", "off", "enable", "disable", "plugin",
        "indent", "runtime"
    ),
    singleLineComment = "\"",
    stringDelimiters = setOf('"', '\'')
)
