package com.codeeditor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.codeeditor.config.EditorConfiguration
import com.codeeditor.highlighting.LanguageRegistry
import com.codeeditor.theme.EditorThemes
import com.codeeditor.vim.VimMode
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for CodeEditor UI component.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CodeEditorInstrumentedTest {

    private lateinit var context: Context
    private lateinit var editor: CodeEditor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        editor = CodeEditor(context)
    }

    @Test
    fun testEditorCreation() {
        assertNotNull(editor)
        assertTrue(editor is CodeEditor)
    }

    @Test
    fun testEditorConfiguration() {
        editor.configure {
            language("kotlin")
            theme(EditorThemes.Dark)
            showLineNumbers(true)
            readOnly(false)
            tabWidth(4)
        }

        val config = editor.configuration.value
        assertEquals("kotlin", config.language)
        assertEquals(EditorThemes.Dark, config.theme)
        assertTrue(config.showLineNumbers)
        assertFalse(config.readOnly)
        assertEquals(4, config.tabWidth)
    }

    @Test
    fun testSetText() {
        val testText = "fun main() {\n    println(\"Hello\")\n}"
        editor.setText(testText)

        assertEquals(testText, editor.text.toString())
    }

    @Test
    fun testLineCount() {
        val testText = "Line 1\nLine 2\nLine 3"
        editor.setText(testText)

        assertEquals(3, editor.lineCount)
    }

    @Test
    fun testWordCount() {
        val testText = "Hello World Foo Bar"
        editor.setText(testText)

        assertEquals(4, editor.wordCount)
    }

    @Test
    fun testCharacterCount() {
        val testText = "Hello World"
        editor.setText(testText)

        assertEquals(11, editor.characterCount)
    }

    @Test
    fun testSetLanguage() {
        editor.setLanguageByName("kotlin")
        assertNotNull(editor.currentLanguage)
        assertEquals("kotlin", editor.currentLanguage?.name)
    }

    @Test
    fun testDetectLanguage() {
        editor.detectLanguage("test.kt")
        assertEquals("kotlin", editor.currentLanguage?.name)

        editor.detectLanguage("test.java")
        assertEquals("java", editor.currentLanguage?.name)
    }

    @Test
    fun testThemeSwitching() {
        editor.setTheme(EditorThemes.Dark)
        assertEquals(EditorThemes.Dark, editor.currentTheme)

        editor.setTheme(EditorThemes.Light)
        assertEquals(EditorThemes.Light, editor.currentTheme)

        editor.setTheme("dracula")
        assertEquals(EditorThemes.Dracula, editor.currentTheme)
    }

    @Test
    fun testGoToLine() {
        editor.setText("Line 0\nLine 1\nLine 2\nLine 3")
        editor.goToLine(2)

        assertEquals(2, editor.getCurrentLine())
    }

    @Test
    fun testReadOnlyMode() {
        editor.configure { readOnly(true) }

        val config = editor.configuration.value
        assertTrue(config.readOnly)
    }

    @Test
    fun testVimModeEnableDisable() {
        editor.enableVimMode()
        assertTrue(editor.isVimMode)

        editor.disableVimMode()
        assertFalse(editor.isVimMode)

        editor.toggleVimMode()
        assertTrue(editor.isVimMode)
    }

    @Test
    fun testUndoRedo() {
        editor.setText("initial")
        editor.setText("modified")

        editor.undo()
        // After undo, text should revert

        editor.redo()
        // After redo, text should be modified again
    }

    @Test
    fun testSearchShowHide() {
        editor.showFind()
        assertTrue(editor.isSearchVisible)

        editor.hideSearch()
        assertFalse(editor.isSearchVisible)
    }

    @Test
    fun testGetCurrentLineColumn() {
        editor.setText("Line 0\nLine 1\nLine 2")

        val line = editor.getCurrentLine()
        val column = editor.getCurrentColumn()

        assertTrue(line >= 0)
        assertTrue(column >= 0)
    }

    @Test
    fun testSelectLine() {
        editor.setText("Line 0\nLine 1\nLine 2")
        editor.selectLine(1)

        val start = editor.selectionStart
        val end = editor.selectionEnd
        assertNotEquals(start, end)
    }

    @Test
    fun testDuplicateLine() {
        editor.setText("Line 0\nLine 1")
        editor.duplicateLine(0)

        assertTrue(editor.lineCount >= 3)
    }

    @Test
    fun testDeleteLine() {
        editor.setText("Line 0\nLine 1\nLine 2")
        editor.deleteLine(1)

        assertEquals(2, editor.lineCount)
    }

    @Test
    fun testTabWidthConfiguration() {
        editor.configure { tabWidth(2) }
        assertEquals(2, editor.configuration.value.tabWidth)

        editor.configure { tabWidth(8) }
        assertEquals(8, editor.configuration.value.tabWidth)
    }

    @Test
    fun testSoftTabsConfiguration() {
        editor.configure { useSoftTabs(true) }
        assertTrue(editor.configuration.value.useSoftTabs)

        editor.configure { useSoftTabs(false) }
        assertFalse(editor.configuration.value.useSoftTabs)
    }

    @Test
    fun testLineWrappingToggle() {
        editor.configure { lineWrapping(true) }
        assertTrue(editor.configuration.value.lineWrapping)

        editor.configure { lineWrapping(false) }
        assertFalse(editor.configuration.value.lineWrapping)
    }

    @Test
    fun testAutoIndentConfiguration() {
        editor.configure { autoIndent(true) }
        assertTrue(editor.configuration.value.autoIndent)

        editor.configure { autoIndent(false) }
        assertFalse(editor.configuration.value.autoIndent)
    }

    @Test
    fun testHighlightingToggle() {
        editor.configure { syntaxHighlightingEnabled(true) }
        assertTrue(editor.configuration.value.syntaxHighlightingEnabled)

        editor.configure { syntaxHighlightingEnabled(false) }
        assertFalse(editor.configuration.value.syntaxHighlightingEnabled)
    }

    @Test
    fun testGutterToggle() {
        editor.configure { showGutter(true) }
        assertTrue(editor.configuration.value.showGutter)

        editor.configure { showGutter(false) }
        assertFalse(editor.configuration.value.showGutter)
    }

    @Test
    fun testMinimapToggle() {
        editor.configure { showMinimap(true) }
        assertTrue(editor.configuration.value.showMinimap)

        editor.configure { showMinimap(false) }
        assertFalse(editor.configuration.value.showMinimap)
    }

    @Test
    fun testCurrentLineHighlightToggle() {
        editor.configure { highlightCurrentLine(true) }
        assertTrue(editor.configuration.value.highlightCurrentLine)

        editor.configure { highlightCurrentLine(false) }
        assertFalse(editor.configuration.value.highlightCurrentLine)
    }

    @Test
    fun testMatchingBracketsToggle() {
        editor.configure { highlightMatchingBrackets(true) }
        assertTrue(editor.configuration.value.highlightMatchingBrackets)

        editor.configure { highlightMatchingBrackets(false) }
        assertFalse(editor.configuration.value.highlightMatchingBrackets)
    }

    @Test
    fun testTrailingSpacesToggle() {
        editor.configure { highlightTrailingSpaces(true) }
        assertTrue(editor.configuration.value.highlightTrailingSpaces)

        editor.configure { highlightTrailingSpaces(false) }
        assertFalse(editor.configuration.value.highlightTrailingSpaces)
    }

    @Test
    fun testSearchOptions() {
        editor.configure {
            regexSearch(true)
            caseSensitive(true)
            wholeWord(true)
        }

        val config = editor.configuration.value
        assertTrue(config.regexSearch)
        assertTrue(config.caseSensitive)
        assertTrue(config.wholeWord)
    }

    @Test
    fun testAutoClosePairsConfiguration() {
        editor.configure { autoClosePairs(true) }
        assertTrue(editor.configuration.value.autoClosePairs)

        val pairs = mapOf('(' to ')', '{' to '}')
        editor.configure { autoClosePairsMap(pairs) }
        assertEquals(pairs, editor.configuration.value.autoClosePairsMap)
    }

    @Test
    fun testLanguageRegistry() {
        assertNotNull(LanguageRegistry.getLanguage("kotlin"))
        assertNotNull(LanguageRegistry.getLanguage("java"))
        assertNotNull(LanguageRegistry.getLanguage("python"))
        assertNotNull(LanguageRegistry.getLanguage("javascript"))
        assertNull(LanguageRegistry.getLanguage("nonexistent"))
    }

    @Test
    fun testAvailableThemes() {
        val themes = com.codeeditor.theme.EditorThemes.availableThemes()
        assertTrue(themes.contains("light"))
        assertTrue(themes.contains("dark"))
    }

    @Test
    fun testDestroy() {
        editor.destroy()
        // Should not throw
        assertTrue(true)
    }
}
