# Consumer ProGuard rules for CodeEditor library
# These rules are applied when this library is included in an application

-keep public class com.codeeditor.CodeEditor { public *; }
-keep public class com.codeeditor.config.EditorConfiguration { public *; }
-keep public class com.codeeditor.config.EditorConfiguration$Builder { public *; }
-keep public class com.codeeditor.highlighting.LanguageRegistry { public *; }
-keep public class com.codeeditor.highlighting.LanguageDefinition { public *; }
-keep public class com.codeeditor.theme.EditorThemes { public *; }
-keep public class com.codeeditor.theme.EditorTheme { public *; }
-keep public class com.codeeditor.vim.VimEngine { public *; }
-keep public class com.codeeditor.vim.VimMode { *; }
-keep public class com.codeeditor.vim.VimAction { *; }
-keep public class com.codeeditor.search.SearchOptions { public *; }
-keep public class com.codeeditor.search.SearchResult { public *; }
-keep public class com.codeeditor.interop.CodeEditorFactory { public *; }
-keep public class com.codeeditor.interop.VimConfigHelper { public *; }
-keep public class com.codeeditor.interop.LanguageDetectionHelper { public *; }
-keep public class com.codeeditor.interop.ThemeHelper { public *; }
