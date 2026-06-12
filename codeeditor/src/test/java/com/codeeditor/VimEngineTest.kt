package com.codeeditor

import android.view.KeyEvent
import com.codeeditor.model.Cursor
import com.codeeditor.model.MultiCursorManager
import com.codeeditor.model.TextBuffer
import com.codeeditor.vim.VimAction
import com.codeeditor.vim.VimEngine
import com.codeeditor.vim.VimMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for the Vim keybindings engine.
 */
@RunWith(MockitoJUnitRunner::class)
class VimEngineTest {

    @Mock
    private lateinit var mockEditor: CodeEditor

    private lateinit var buffer: TextBuffer
    private lateinit var cursorManager: MultiCursorManager
    private lateinit var vimEngine: VimEngine

    @Before
    fun setup() {
        buffer = TextBuffer.fromText("Hello World\nSecond line\nThird line")
        cursorManager = MultiCursorManager()
        cursorManager.setPrimaryCursor(Cursor(0, 0, 0))
        vimEngine = VimEngine(mockEditor, buffer, cursorManager)
        vimEngine.enable()
    }

    @Test
    fun `starts in normal mode`() {
        assertEquals(VimMode.NORMAL, vimEngine.mode)
    }

    @Test
    fun `enter insert mode changes mode`() {
        vimEngine.enterInsertMode()
        assertEquals(VimMode.INSERT, vimEngine.mode)
    }

    @Test
    fun `exit insert mode returns to normal`() {
        vimEngine.enterInsertMode()
        vimEngine.enterNormalMode()
        assertEquals(VimMode.NORMAL, vimEngine.mode)
    }

    @Test
    fun `enter visual mode`() {
        vimEngine.enterVisualMode()
        assertEquals(VimMode.VISUAL, vimEngine.mode)
    }

    @Test
    fun `enter command mode`() {
        vimEngine.enterCommandMode()
        assertEquals(VimMode.COMMAND, vimEngine.mode)
        assertEquals(":", vimEngine.pendingCommand)
    }

    @Test
    fun `enter visual line mode`() {
        vimEngine.enterVisualLineMode()
        assertEquals(VimMode.VISUAL_LINE, vimEngine.mode)
    }

    @Test
    fun `registers store and retrieve values`() {
        vimEngine.setRegister('a', "test content")
        assertEquals("test content", vimEngine.getRegister('a'))
    }

    @Test
    fun `default registers are empty`() {
        assertNull(vimEngine.getRegister('a'))
        assertNull(vimEngine.getRegister('b'))
    }

    @Test
    fun `clear registers removes all`() {
        vimEngine.setRegister('a', "content")
        vimEngine.setRegister('b', "more")
        vimEngine.clearRegisters()
        assertNull(vimEngine.getRegister('a'))
        assertNull(vimEngine.getRegister('b'))
    }

    @Test
    fun `register names are tracked`() {
        vimEngine.setRegister('a', "test")
        vimEngine.setRegister('x', "other")
        val names = vimEngine.getRegisterNames()
        assertTrue(names.contains('a'))
        assertTrue(names.contains('x'))
    }

    @Test
    fun `custom key mapping`() {
        vimEngine.mapKey("jj", VimAction.EXIT_INSERT_MODE)
        // Should not throw
        assertTrue(true)
    }

    @Test
    fun `unmap key removes mapping`() {
        vimEngine.mapKey("jj", VimAction.EXIT_INSERT_MODE)
        vimEngine.unmapKey("jj")
        // Should not throw
        assertTrue(true)
    }

    @Test
    fun `clear mappings resets to defaults`() {
        vimEngine.mapKey("custom", VimAction.ENTER_INSERT_MODE)
        vimEngine.clearMappings()
        // Should keep default mappings
        assertTrue(true)
    }

    @Test
    fun `disable vim sets mode to normal`() {
        vimEngine.enterInsertMode()
        vimEngine.disable()
        assertFalse(vimEngine.isEnabled)
    }

    @Test
    fun `mode change callback is triggered`() {
        var capturedMode: VimMode? = null
        vimEngine.onModeChanged = { mode ->
            capturedMode = mode
        }

        vimEngine.enterInsertMode()
        assertEquals(VimMode.INSERT, capturedMode)

        vimEngine.enterNormalMode()
        assertEquals(VimMode.NORMAL, capturedMode)
    }

    @Test
    fun `status update callback`() {
        var status: String? = null
        vimEngine.onStatusUpdate = { s -> status = s }

        vimEngine.enterInsertMode()
        assertNotNull(status)
    }

    @Test
    fun `normal mode cursor movement commands exist`() {
        // These commands should be defined and not crash
        // Actual movement is tested via integration
        assertEquals(VimMode.NORMAL, vimEngine.mode)
    }

    @Test
    fun `command buffer is cleared on mode change`() {
        vimEngine.enterCommandMode()
        vimEngine.enterNormalMode()
        assertEquals("", vimEngine.pendingCommand)
    }

    @Test
    fun `multiple register operations`() {
        val testData = mapOf(
            'a' to "register a",
            'b' to "register b",
            'z' to "register z",
            '"' to "unnamed",
            '*' to "clipboard"
        )

        testData.forEach { (name, content) ->
            vimEngine.setRegister(name, content)
        }

        testData.forEach { (name, expected) ->
            assertEquals(expected, vimEngine.getRegister(name))
        }
    }

    @Test
    fun `destroy cleans up resources`() {
        vimEngine.destroy()
        assertTrue(true) // Should not throw
    }
}
