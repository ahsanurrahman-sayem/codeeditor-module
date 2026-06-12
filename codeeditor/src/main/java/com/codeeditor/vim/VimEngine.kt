package com.codeeditor.vim

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.BaseInputConnection
import com.codeeditor.CodeEditor
import com.codeeditor.model.Cursor
import com.codeeditor.model.MultiCursorManager
import com.codeeditor.model.TextBuffer
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Vim keybindings engine supporting Normal, Insert, Visual, and Command modes.
 *
 * Provides configurable key mappings, registers, motions, operators,
 * and a command-line parser.
 *
 * ## Configuration
 * ```kotlin
 * val vim = editor.getVimEngine()
 * vim.mapKey("jj", VimAction.ENTER_INSERT_MODE)
 * vim.setRegister('a', "stored text")
 * ```
 *
 * @see VimMode for available modes
 * @see VimAction for configurable actions
 */
public class VimEngine(
    private val editor: CodeEditor,
    private val textBuffer: TextBuffer,
    private val cursorManager: MultiCursorManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val isEnabled = AtomicBoolean(false)
    private val _mode = AtomicReference(VimMode.NORMAL)
    private val commandBuffer = StringBuilder()
    private val registers = ConcurrentHashMap<Char, String>()
    private val namedMappings = ConcurrentHashMap<String, VimAction>()
    private val operatorPending = AtomicBoolean(false)
    private val visualStartPos = AtomicReference(0)

    /** Current Vim mode */
    public val mode: VimMode get() = _mode.get()

    /** Current command being built */
    public val pendingCommand: String get() = commandBuffer.toString()

    /** Callback for mode changes */
    public var onModeChanged: ((VimMode) -> Unit)? = null

    /** Callback for status line updates */
    public var onStatusUpdate: ((String) -> Unit)? = null

    init {
        initializeDefaultMappings()
    }

    // region Mode Management

    /**
     * Enables Vim keybindings.
     */
    public fun enable() {
        isEnabled.set(true)
        enterNormalMode()
    }

    /**
     * Disables Vim keybindings.
     */
    public fun disable() {
        isEnabled.set(false)
        _mode.set(VimMode.NORMAL)
        editor.isCursorVisible = true
    }

    /**
     * Enters Normal mode.
     */
    public fun enterNormalMode() {
        val prevMode = _mode.getAndSet(VimMode.NORMAL)
        if (prevMode == VimMode.INSERT) {
            // Move cursor left when exiting insert (Vim behavior)
            val pos = editor.selectionStart
            if (pos > 0) {
                editor.setSelection(pos - 1)
            }
        }
        editor.isCursorVisible = true
        commandBuffer.clear()
        operatorPending.set(false)
        onModeChanged?.invoke(VimMode.NORMAL)
        updateStatus()
    }

    /**
     * Enters Insert mode.
     */
    public fun enterInsertMode() {
        _mode.set(VimMode.INSERT)
        editor.isCursorVisible = true
        commandBuffer.clear()
        operatorPending.set(false)
        onModeChanged?.invoke(VimMode.INSERT)
        onStatusUpdate?.invoke("-- INSERT --")
    }

    /**
     * Enters Visual mode.
     */
    public fun enterVisualMode() {
        _mode.set(VimMode.VISUAL)
        visualStartPos.set(editor.selectionStart)
        onModeChanged?.invoke(VimMode.VISUAL)
        onStatusUpdate?.invoke("-- VISUAL --")
    }

    /**
     * Enters Visual Line mode.
     */
    public fun enterVisualLineMode() {
        _mode.set(VimMode.VISUAL_LINE)
        visualStartPos.set(editor.selectionStart)
        onModeChanged?.invoke(VimMode.VISUAL_LINE)
        onStatusUpdate?.invoke("-- VISUAL LINE --")
    }

    /**
     * Enters Command mode.
     */
    public fun enterCommandMode() {
        _mode.set(VimMode.COMMAND)
        commandBuffer.clear()
        commandBuffer.append(':')
        onModeChanged?.invoke(VimMode.COMMAND)
        updateStatus()
    }

    // endregion

    // region Input Handling

    /**
     * Handles touch events for Vim (e.g., tap to move cursor in normal mode).
     */
    public fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled.get()) return false
        if (event.action == MotionEvent.ACTION_DOWN && mode == VimMode.NORMAL) {
            // Tap moves cursor but doesn't enter insert mode
            return false // Let default handler process it
        }
        return false
    }

    /**
     * Handles key down events.
     *
     * @return true if the key was handled
     */
    public fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isEnabled.get() || event == null) return false

        return when (_mode.get()) {
            VimMode.NORMAL -> handleNormalModeKey(keyCode, event)
            VimMode.INSERT -> handleInsertModeKey(keyCode, event)
            VimMode.VISUAL, VimMode.VISUAL_LINE -> handleVisualModeKey(keyCode, event)
            VimMode.COMMAND -> handleCommandModeKey(keyCode, event)
        }
    }

    /**
     * Handles key up events.
     */
    public fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isEnabled.get() || event == null) return false
        return false
    }

    // endregion

    // region Normal Mode

    private fun handleNormalModeKey(keyCode: Int, event: KeyEvent): Boolean {
        val char = event.unicodeChar.toChar()
        val ctrl = event.isCtrlPressed

        // Check for mapped sequences
        commandBuffer.append(char)
        val mappedAction = checkMappings(commandBuffer.toString())
        if (mappedAction != null) {
            executeAction(mappedAction)
            commandBuffer.clear()
            return true
        }

        // Single-character commands
        if (!event.isPrintingKey) {
            return handleSpecialKey(keyCode, event)
        }

        // Check for pending operator
        if (operatorPending.get()) {
            executeOperatorMotion(commandBuffer.toString())
            commandBuffer.clear()
            operatorPending.set(false)
            return true
        }

        // Direct command execution
        when {
            ctrl -> return handleCtrlCommand(char)
            else -> return handleNormalCommand(char, event)
        }
    }

    private fun handleNormalCommand(char: Char, event: KeyEvent): Boolean {
        when (char) {
            'i' -> enterInsertMode()
            'a' -> { editor.setSelection(editor.selectionStart + 1); enterInsertMode() }
            'o' -> {
                val line = editor.getCurrentLine()
                val lineEnd = editor.layout?.getLineEnd(line) ?: editor.selectionStart
                editor.text?.insert(lineEnd, "\n")
                editor.setSelection(lineEnd + 1)
                enterInsertMode()
            }
            'O' -> {
                val line = editor.getCurrentLine()
                val lineStart = editor.layout?.getLineStart(line) ?: editor.selectionStart
                editor.text?.insert(lineStart, "\n")
                editor.setSelection(lineStart)
                enterInsertMode()
            }
            'h', KeyEvent.KEYCODE_DPAD_LEFT.toChar() -> moveCursor(-1, 0)
            'j', KeyEvent.KEYCODE_DPAD_DOWN.toChar() -> moveCursor(0, 1)
            'k', KeyEvent.KEYCODE_DPAD_UP.toChar() -> moveCursor(0, -1)
            'l', KeyEvent.KEYCODE_DPAD_RIGHT.toChar() -> moveCursor(1, 0)
            'w' -> moveWordForward()
            'b' -> moveWordBackward()
            'e' -> moveWordEnd()
            '0' -> moveToLineStart()
            '^' -> moveToFirstNonBlank()
            '$' -> moveToLineEnd()
            'G' -> moveToLine(editor.lineCount - 1)
            'g' -> {
                if (commandBuffer.length > 1 && commandBuffer[commandBuffer.length - 2] == 'g') {
                    moveToLine(0)
                    commandBuffer.clear()
                }
                return true
            }
            'x' -> deleteCharacter()
            'X' -> deleteCharacterBefore()
            'd' -> { operatorPending.set(true); commandBuffer.clear(); commandBuffer.append('d'); return true }
            'D' -> deleteToEndOfLine()
            'c' -> { operatorPending.set(true); commandBuffer.clear(); commandBuffer.append('c'); return true }
            'C' -> { deleteToEndOfLine(); enterInsertMode() }
            'y' -> { operatorPending.set(true); commandBuffer.clear(); commandBuffer.append('y'); return true }
            'Y' -> yankLine()
            'p' -> pasteAfter()
            'P' -> pasteBefore()
            'u' -> editor.undo()
            'r' -> editor.redo()
            'v' -> enterVisualMode()
            'V' -> enterVisualLineMode()
            ':' -> enterCommandMode()
            '/' -> { enterCommandMode(); commandBuffer.clear(); commandBuffer.append('/'); return true }
            'n' -> editor.findNext()
            'N' -> editor.findPrevious()
            '>' -> indentLine()
            '<' -> outdentLine()
            'J' -> joinLines()
            '~' -> toggleCase()
            '%' -> jumpToMatchingBracket()
            else -> return false
        }
        commandBuffer.clear()
        return true
    }

    private fun handleCtrlCommand(char: Char): Boolean {
        when (char) {
            'f' -> scrollPageDown()
            'b' -> scrollPageUp()
            'd' -> scrollHalfPageDown()
            'u' -> scrollHalfPageUp()
            'e' -> scrollLineDown()
            'y' -> scrollLineUp()
            'r' -> editor.redo()
            'w' -> { editor.text?.insert(editor.selectionStart, " "); moveCursor(1, 0) }
            else -> return false
        }
        return true
    }

    private fun handleSpecialKey(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveCursor(-1, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveCursor(1, 0)
            KeyEvent.KEYCODE_DPAD_UP -> moveCursor(0, -1)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveCursor(0, 1)
            KeyEvent.KEYCODE_MOVE_HOME -> moveToLineStart()
            KeyEvent.KEYCODE_MOVE_END -> moveToLineEnd()
            KeyEvent.KEYCODE_PAGE_UP -> scrollPageUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> scrollPageDown()
            KeyEvent.KEYCODE_ESCAPE -> {
                commandBuffer.clear()
                operatorPending.set(false)
            }
            else -> return false
        }
        return true
    }

    // endregion

    // region Insert Mode

    private fun handleInsertModeKey(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                enterNormalMode()
                return true
            }
            KeyEvent.KEYCODE_TAB -> {
                // Let editor handle tab
                return false
            }
            KeyEvent.KEYCODE_ENTER -> {
                return false // Let default handler insert newline
            }
            else -> return false // Let default handler process character
        }
    }

    // endregion

    // region Visual Mode

    private fun handleVisualModeKey(keyCode: Int, event: KeyEvent): Boolean {
        val char = event.unicodeChar.toChar()

        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                enterNormalMode()
                return true
            }
        }

        when (char) {
            'h', 'j', 'k', 'l' -> {
                handleNormalCommand(char, event)
                updateVisualSelection()
                return true
            }
            'w', 'b', 'e' -> {
                handleNormalCommand(char, event)
                updateVisualSelection()
                return true
            }
            'd', 'x' -> {
                deleteSelection()
                enterNormalMode()
                return true
            }
            'y' -> {
                yankSelection()
                enterNormalMode()
                return true
            }
            'c' -> {
                deleteSelection()
                enterInsertMode()
                return true
            }
            '>' -> {
                indentSelection()
                enterNormalMode()
                return true
            }
            '<' -> {
                outdentSelection()
                enterNormalMode()
                return true
            }
            '~' -> {
                toggleCaseSelection()
                enterNormalMode()
                return true
            }
        }

        return false
    }

    private fun updateVisualSelection() {
        val start = visualStartPos.get()
        val end = editor.selectionStart
        editor.setSelection(start.coerceAtMost(end), start.coerceAtLeast(end))
    }

    // endregion

    // region Command Mode

    private fun handleCommandModeKey(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                enterNormalMode()
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                executeCommand(commandBuffer.toString())
                return true
            }
            KeyEvent.KEYCODE_DEL -> {
                if (commandBuffer.length > 1) {
                    commandBuffer.deleteAt(commandBuffer.length - 1)
                    updateStatus()
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (commandBuffer.length > 1) {
                    commandBuffer.deleteAt(commandBuffer.length - 1)
                    updateStatus()
                    return true
                }
            }
        }

        if (event.isPrintingKey) {
            commandBuffer.append(event.unicodeChar.toChar())
            updateStatus()
            return true
        }

        return true
    }

    private fun executeCommand(command: String) {
        val cmd = command.trimStart(':')
        when {
            cmd == "w" || cmd.startsWith("w ") -> onStatusUpdate?.invoke(":w (save)")
            cmd == "q" -> onStatusUpdate?.invoke(":q (quit)")
            cmd == "wq" || cmd == "x" -> onStatusUpdate?.invoke(":wq (save and quit)")
            cmd == "q!" -> onStatusUpdate?.invoke(":q! (force quit)")
            cmd.toIntOrNull() != null -> {
                val line = cmd.toInt() - 1
                editor.goToLine(line.coerceAtLeast(0))
            }
            cmd.startsWith("set ") -> handleSetCommand(cmd.substring(4))
            cmd.startsWith("s/") -> handleSubstituteCommand(cmd)
            else -> onStatusUpdate?.invoke("E492: Not an editor command: $cmd")
        }
        enterNormalMode()
    }

    private fun handleSetCommand(setting: String) {
        when (setting.trim()) {
            "nu", "number" -> editor.configure { showLineNumbers(true) }
            "nonu", "nonumber" -> editor.configure { showLineNumbers(false) }
            "wrap" -> editor.configure { lineWrapping(true) }
            "nowrap" -> editor.configure { lineWrapping(false) }
            "ic", "ignorecase" -> editor.configure { caseSensitive(false) }
            "noic", "noignorecase" -> editor.configure { caseSensitive(true) }
            else -> onStatusUpdate?.invoke("Unknown option: $setting")
        }
    }

    private fun handleSubstituteCommand(cmd: String) {
        val parts = cmd.substring(2).split('/')
        if (parts.size >= 2) {
            val pattern = parts[0]
            val replacement = parts[1]
            val flags = if (parts.size > 2) parts[2] else ""
            editor.replaceAll(pattern, replacement, com.codeeditor.search.SearchOptions(
                regex = true,
                caseSensitive = !editor.configuration.value.caseSensitive
            ))
        }
    }

    // endregion

    // region Motions

    private fun moveCursor(deltaX: Int, deltaY: Int) {
        val layout = editor.layout ?: return
        val currentLine = layout.getLineForOffset(editor.selectionStart)
        val currentColumn = editor.selectionStart - layout.getLineStart(currentLine)

        when {
            deltaY != 0 -> {
                val newLine = (currentLine + deltaY).coerceIn(0, editor.lineCount - 1)
                val newLineStart = layout.getLineStart(newLine)
                val newLineLength = layout.getLineEnd(newLine) - newLineStart
                val newColumn = currentColumn.coerceAtMost(newLineLength)
                editor.setSelection(newLineStart + newColumn)
            }
            deltaX != 0 -> {
                val newPos = (editor.selectionStart + deltaX).coerceIn(0, editor.length())
                editor.setSelection(newPos)
            }
        }
    }

    private fun moveWordForward() {
        val text = editor.text?.toString() ?: return
        var pos = editor.selectionStart + 1
        while (pos < text.length && !text[pos].isLetterOrDigit()) pos++
        while (pos < text.length && (text[pos].isLetterOrDigit() || text[pos] == '_')) pos++
        editor.setSelection(pos.coerceAtMost(text.length))
    }

    private fun moveWordBackward() {
        val text = editor.text?.toString() ?: return
        var pos = editor.selectionStart - 1
        if (pos < 0) return
        while (pos > 0 && !text[pos].isLetterOrDigit()) pos--
        while (pos > 0 && (text[pos - 1].isLetterOrDigit() || text[pos - 1] == '_')) pos--
        editor.setSelection(pos.coerceAtLeast(0))
    }

    private fun moveWordEnd() {
        val text = editor.text?.toString() ?: return
        var pos = editor.selectionStart + 1
        while (pos < text.length && !text[pos].isLetterOrDigit()) pos++
        while (pos < text.length && (text[pos].isLetterOrDigit() || text[pos] == '_')) pos++
        editor.setSelection((pos - 1).coerceAtLeast(0))
    }

    private fun moveToLineStart() {
        val line = editor.getCurrentLine()
        val start = editor.layout?.getLineStart(line) ?: 0
        editor.setSelection(start)
    }

    private fun moveToFirstNonBlank() {
        val line = editor.getCurrentLine()
        val text = editor.text?.toString() ?: return
        val lineStart = editor.layout?.getLineStart(line) ?: 0
        val lineEnd = editor.layout?.getLineEnd(line) ?: return
        var pos = lineStart
        while (pos < lineEnd && text[pos].isWhitespace() && text[pos] != '\n') pos++
        editor.setSelection(pos)
    }

    private fun moveToLineEnd() {
        val line = editor.getCurrentLine()
        val end = editor.layout?.getLineEnd(line)?.let { it - 1 } ?: editor.selectionStart
        editor.setSelection(end.coerceAtLeast(0))
    }

    private fun moveToLine(line: Int) {
        val target = line.coerceIn(0, (editor.lineCount - 1).coerceAtLeast(0))
        editor.goToLine(target)
    }

    // endregion

    // region Operators

    private fun executeOperatorMotion(motion: String) {
        when (motion) {
            "dd" -> deleteLine()
            "dw" -> { moveWordForward(); deleteSelection() }
            "db" -> { moveWordBackward(); deleteSelection() }
            "d$" -> deleteToEndOfLine()
            "d0" -> { val start = editor.selectionStart; moveToLineStart(); deleteRange(editor.selectionStart, start) }
            "cc" -> { deleteLine(); enterInsertMode() }
            "cw" -> { val start = editor.selectionStart; moveWordForward(); deleteRange(start, editor.selectionStart); enterInsertMode() }
            "yy" -> yankLine()
            "yw" -> { val start = editor.selectionStart; moveWordForward(); yankRange(start, editor.selectionStart) }
            else -> {
                // Generic: operator + motion
                if (motion.startsWith("d")) {
                    val saved = editor.selectionStart
                    // Execute the motion character
                    val motionChar = motion.lastOrNull() ?: return
                    executeMotion(motionChar)
                    deleteRange(saved, editor.selectionStart)
                }
            }
        }
    }

    private fun executeMotion(char: Char) {
        when (char) {
            'w' -> moveWordForward()
            'b' -> moveWordBackward()
            'e' -> moveWordEnd()
            '$' -> moveToLineEnd()
            '0' -> moveToLineStart()
            'h' -> moveCursor(-1, 0)
            'j' -> moveCursor(0, 1)
            'k' -> moveCursor(0, -1)
            'l' -> moveCursor(1, 0)
        }
    }

    // endregion

    // region Text Operations

    private fun deleteCharacter() {
        val pos = editor.selectionStart
        editor.text?.delete(pos, pos + 1)
    }

    private fun deleteCharacterBefore() {
        val pos = editor.selectionStart
        if (pos > 0) editor.text?.delete(pos - 1, pos)
    }

    private fun deleteLine() {
        editor.deleteLine()
    }

    private fun deleteToEndOfLine() {
        val pos = editor.selectionStart
        val line = editor.getCurrentLine()
        val end = editor.layout?.getLineEnd(line) ?: return
        editor.text?.delete(pos, end)
    }

    private fun deleteRange(start: Int, end: Int) {
        val s = start.coerceAtMost(end)
        val e = start.coerceAtLeast(end)
        editor.text?.delete(s, e)
        editor.setSelection(s)
    }

    private fun deleteSelection() {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start != end) {
            editor.text?.delete(start, end)
            editor.setSelection(start)
        }
    }

    private fun yankLine() {
        val line = editor.getCurrentLine()
        val start = editor.layout?.getLineStart(line) ?: return
        val end = editor.layout?.getLineEnd(line) ?: return
        val text = editor.text?.substring(start, end) ?: return
        setRegister('"', text)
        setRegister('0', text)
    }

    private fun yankRange(start: Int, end: Int) {
        val s = start.coerceAtMost(end)
        val e = start.coerceAtLeast(end)
        val text = editor.text?.substring(s, e) ?: return
        setRegister('"', text)
        setRegister('0', text)
    }

    private fun yankSelection() {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start != end) {
            val text = editor.text?.substring(start, end) ?: return
            setRegister('"', text)
        }
    }

    private fun pasteAfter() {
        val text = getRegister('"') ?: return
        val pos = editor.selectionStart + 1
        editor.text?.insert(pos.coerceAtMost(editor.length()), text)
        editor.setSelection(pos)
    }

    private fun pasteBefore() {
        val text = getRegister('"') ?: return
        val pos = editor.selectionStart
        editor.text?.insert(pos, text)
    }

    private fun toggleCase() {
        val pos = editor.selectionStart
        val text = editor.text ?: return
        if (pos < text.length) {
            val char = text[pos]
            val newChar = if (char.isUpperCase()) char.lowercaseChar() else char.uppercaseChar()
            text.replace(pos, pos + 1, newChar.toString())
            editor.setSelection(pos + 1)
        }
    }

    private fun joinLines() {
        val line = editor.getCurrentLine()
        val text = editor.text?.toString() ?: return
        val lines = text.split('\n')
        if (line < 0 || line >= lines.size - 1) return
        val joined = lines[line].trimEnd() + " " + lines[line + 1].trimStart()
        val start = editor.layout?.getLineStart(line) ?: return
        val nextEnd = editor.layout?.getLineEnd(line + 1) ?: return
        editor.text?.replace(start, nextEnd, joined)
    }

    private fun indentLine() {
        editor.indentSelection()
    }

    private fun outdentLine() {
        editor.outdentLine()
    }

    private fun indentSelection() {
        editor.indentSelection()
    }

    private fun outdentSelection() {
        editor.outdentLine()
    }

    private fun toggleCaseSelection() {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start == end) return
        val text = editor.text ?: return
        val selected = text.substring(start, end)
        val toggled = selected.map { if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }.joinToString("")
        text.replace(start, end, toggled)
    }

    private fun jumpToMatchingBracket() {
        val pos = editor.selectionStart
        val text = editor.text?.toString() ?: return
        if (pos >= text.length) return

        val pairs = mapOf('(' to ')', '{' to '}', '[' to ']', ')' to '(', '}' to '{', ']' to '[')
        val char = text[pos]
        val target = pairs[char] ?: return

        val matchPos = if (char in "({[") {
            findForwardMatch(text, pos, char, target)
        } else {
            findBackwardMatch(text, pos, char, target)
        }

        if (matchPos != -1) {
            editor.setSelection(matchPos)
        }
    }

    private fun findForwardMatch(text: String, start: Int, open: Char, close: Char): Int {
        var depth = 1
        var pos = start + 1
        while (pos < text.length) {
            when (text[pos]) {
                open -> depth++
                close -> { depth--; if (depth == 0) return pos }
            }
            pos++
        }
        return -1
    }

    private fun findBackwardMatch(text: String, start: Int, close: Char, open: Char): Int {
        var depth = 1
        var pos = start - 1
        while (pos >= 0) {
            when (text[pos]) {
                close -> depth++
                open -> { depth--; if (depth == 0) return pos }
            }
            pos--
        }
        return -1
    }

    // endregion

    // region Scrolling

    private fun scrollPageDown() {
        editor.scrollBy(0, editor.height)
    }

    private fun scrollPageUp() {
        editor.scrollBy(0, -editor.height)
    }

    private fun scrollHalfPageDown() {
        editor.scrollBy(0, editor.height / 2)
    }

    private fun scrollHalfPageUp() {
        editor.scrollBy(0, -editor.height / 2)
    }

    private fun scrollLineDown() {
        editor.scrollBy(0, editor.lineHeight)
    }

    private fun scrollLineUp() {
        editor.scrollBy(0, -editor.lineHeight)
    }

    // endregion

    // region Mappings

    private fun initializeDefaultMappings() {
        // Example: map "jj" to exit insert mode
        mapKey("jj", VimAction.EXIT_INSERT_MODE)
        mapKey("jk", VimAction.EXIT_INSERT_MODE)
    }

    /**
     * Maps a key sequence to an action.
     *
     * @param keys Key sequence (e.g., "jj", "<Space>w")
     * @param action Action to execute
     */
    public fun mapKey(keys: String, action: VimAction) {
        namedMappings[keys] = action
    }

    /**
     * Unmaps a key sequence.
     */
    public fun unmapKey(keys: String) {
        namedMappings.remove(keys)
    }

    /**
     * Clears all custom mappings.
     */
    public fun clearMappings() {
        namedMappings.clear()
        initializeDefaultMappings()
    }

    private fun checkMappings(input: String): VimAction? {
        return namedMappings[input]
    }

    private fun executeAction(action: VimAction) {
        when (action) {
            VimAction.ENTER_INSERT_MODE -> enterInsertMode()
            VimAction.EXIT_INSERT_MODE -> enterNormalMode()
            VimAction.ENTER_VISUAL_MODE -> enterVisualMode()
            VimAction.ENTER_COMMAND_MODE -> enterCommandMode()
            VimAction.MOVE_LEFT -> moveCursor(-1, 0)
            VimAction.MOVE_RIGHT -> moveCursor(1, 0)
            VimAction.MOVE_UP -> moveCursor(0, -1)
            VimAction.MOVE_DOWN -> moveCursor(0, 1)
            VimAction.UNDO -> editor.undo()
            VimAction.REDO -> editor.redo()
            VimAction.SAVE -> { /* Trigger save callback */ }
            VimAction.QUIT -> { /* Trigger quit callback */ }
        }
    }

    // endregion

    // region Registers

    /**
     * Sets the content of a named register.
     *
     * @param name Register name ('a'-'z', '"', '*', '+')
     * @param content Content to store
     */
    public fun setRegister(name: Char, content: String) {
        registers[name] = content
    }

    /**
     * Gets the content of a named register.
     *
     * @param name Register name
     * @return Register content or null if empty
     */
    public fun getRegister(name: Char): String? {
        return registers[name]
    }

    /**
     * Clears all registers.
     */
    public fun clearRegisters() {
        registers.clear()
    }

    /**
     * Lists all non-empty register names.
     */
    public fun getRegisterNames(): Set<Char> = registers.keys.toSet()

    // endregion

    // region Status

    private fun updateStatus() {
        when (_mode.get()) {
            VimMode.NORMAL -> onStatusUpdate?.invoke("")
            VimMode.INSERT -> onStatusUpdate?.invoke("-- INSERT --")
            VimMode.VISUAL -> onStatusUpdate?.invoke("-- VISUAL --")
            VimMode.VISUAL_LINE -> onStatusUpdate?.invoke("-- VISUAL LINE --")
            VimMode.COMMAND -> onStatusUpdate?.invoke(commandBuffer.toString())
        }
    }

    // endregion

    // region Lifecycle

    /**
     * Cleans up Vim engine resources.
     */
    public fun destroy() {
        scope.cancel()
        namedMappings.clear()
        registers.clear()
    }

    // endregion
}

/**
 * Vim editing modes.
 */
public enum class VimMode {
    NORMAL,
    INSERT,
    VISUAL,
    VISUAL_LINE,
    COMMAND
}

/**
 * Configurable Vim actions for key mappings.
 */
public enum class VimAction {
    ENTER_INSERT_MODE,
    EXIT_INSERT_MODE,
    ENTER_VISUAL_MODE,
    ENTER_COMMAND_MODE,
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    UNDO,
    REDO,
    SAVE,
    QUIT
}
