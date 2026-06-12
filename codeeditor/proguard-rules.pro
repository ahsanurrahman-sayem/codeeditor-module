# ProGuard rules for CodeEditor library

# Keep public API classes
-keep public class com.codeeditor.CodeEditor {
    public *;
}

-keep public class com.codeeditor.model.* {
    public *;
}

-keep public class com.codeeditor.highlighting.* {
    public *;
}

-keep public class com.codeeditor.vim.* {
    public *;
}

-keep public class com.codeeditor.search.* {
    public *;
}

-keep public class com.codeeditor.theme.* {
    public *;
}

-keep public class com.codeeditor.config.* {
    public *;
}

-keep public class com.codeeditor.util.* {
    public *;
}

-keep public class com.codeeditor.compose.* {
    public *;
}

-keep public class com.codeeditor.interop.* {
    public *;
}

# Keep enums
-keep public enum com.codeeditor.highlighting.TokenType { *; }
-keep public enum com.codeeditor.vim.VimMode { *; }
-keep public enum com.codeeditor.vim.VimAction { *; }
-keep public enum com.codeeditor.model.TextChangeType { *; }

# Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
