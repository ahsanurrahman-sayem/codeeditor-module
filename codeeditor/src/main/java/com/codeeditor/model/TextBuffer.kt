package com.codeeditor.model

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import androidx.core.text.set
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe text buffer optimized for large file editing using a gap buffer
 * data structure for efficient insertions and deletions.
 */
public class TextBuffer private constructor(
    initialCapacity: Int = DEFAULT_CAPACITY
) {
    private val buffer = GapBuffer(initialCapacity)
    private val changeListeners = mutableListOf<TextChangeListener>()
    private val mutex = Mutex()
    private val modificationStamp = AtomicLong(0)

    /** Current line count in the buffer */
    public val lineCount: Int get() = buffer.lineCount

    /** Current character count */
    public val length: Int get() = buffer.length

    /** Whether the buffer has unsaved changes */
    public var isDirty: Boolean = false
        private set

    /** Thread-safe modification stamp for change tracking */
    public val stamp: Long get() = modificationStamp.get()

    /**
     * Inserts text at the specified position.
     *
     * @param position Character position to insert at
     * @param text Text to insert
     * @throws IndexOutOfBoundsException if position is invalid
     */
    public suspend fun insert(position: Int, text: String) {
        mutex.withLock {
            require(position in 0..length) { "Position $position out of range [0, $length]" }
            buffer.insert(position, text)
            isDirty = true
            modificationStamp.incrementAndGet()
            notifyChange(position, position + text.length, TextChangeType.INSERT)
        }
    }

    /**
     * Deletes text in the specified range.
     *
     * @param start Start position (inclusive)
     * @param end End position (exclusive)
     * @throws IndexOutOfBoundsException if range is invalid
     */
    public suspend fun delete(start: Int, end: Int) {
        mutex.withLock {
            require(start in 0..length && end in start..length) { "Invalid range [$start, $end]" }
            buffer.delete(start, end)
            isDirty = true
            modificationStamp.incrementAndGet()
            notifyChange(start, start, TextChangeType.DELETE)
        }
    }

    /**
     * Replaces text in the specified range.
     *
     * @param start Start position (inclusive)
     * @param end End position (exclusive)
     * @param text Replacement text
     */
    public suspend fun replace(start: Int, end: Int, text: String) {
        mutex.withLock {
            require(start in 0..length && end in start..length) { "Invalid range [$start, $end]" }
            buffer.delete(start, end)
            buffer.insert(start, text)
            isDirty = true
            modificationStamp.incrementAndGet()
            notifyChange(start, start + text.length, TextChangeType.REPLACE)
        }
    }

    /**
     * Gets the character at the specified position.
     */
    public fun charAt(position: Int): Char = buffer.charAt(position)

    /**
     * Gets a substring in the specified range without allocating the full string.
     */
    public fun substring(start: Int, end: Int): String = buffer.substring(start, end)

    /**
     * Gets the full text content.
     */
    public fun getText(): String = buffer.toString()

    /**
     * Gets the line number (0-based) for a character position.
     */
    public fun getLineForPosition(position: Int): Int = buffer.getLineForPosition(position)

    /**
     * Gets the start position of a line.
     */
    public fun getLineStart(line: Int): Int = buffer.getLineStart(line)

    /**
     * Gets the end position of a line (excluding newline).
     */
    public fun getLineEnd(line: Int): Int = buffer.getLineEnd(line)

    /**
     * Gets the text of a specific line.
     */
    public fun getLineText(line: Int): String = buffer.getLineText(line)

    /**
     * Marks the buffer as clean (saved).
     */
    public fun markClean() {
        isDirty = false
    }

    /**
     * Registers a text change listener.
     */
    public fun addChangeListener(listener: TextChangeListener) {
        changeListeners.add(listener)
    }

    /**
     * Unregisters a text change listener.
     */
    public fun removeChangeListener(listener: TextChangeListener) {
        changeListeners.remove(listener)
    }

    private fun notifyChange(start: Int, end: Int, type: TextChangeType) {
        changeListeners.forEach { it.onTextChanged(start, end, type) }
    }

    /**
     * Converts to Android Editable for compatibility.
     */
    public fun toEditable(): Editable = SpannableStringBuilder(getText())

    public companion object {
        private const val DEFAULT_CAPACITY = 1024

        /**
         * Creates a new empty TextBuffer.
         */
        @JvmStatic
        public fun create(): TextBuffer = TextBuffer()

        /**
         * Creates a TextBuffer from initial text.
         */
        @JvmStatic
        public fun fromText(text: String): TextBuffer {
            return TextBuffer(text.length.coerceAtLeast(DEFAULT_CAPACITY)).apply {
                buffer.insert(0, text)
            }
        }
    }
}

/**
 * Listener interface for text buffer changes.
 */
public interface TextChangeListener {
    /**
     * Called when text changes occur.
     *
     * @param start Start position of change
     * @param end End position of change
     * @param type Type of change
     */
    public fun onTextChanged(start: Int, end: Int, type: TextChangeType)
}

/**
 * Types of text changes.
 */
public enum class TextChangeType {
    INSERT,
    DELETE,
    REPLACE
}

/**
 * Internal gap buffer implementation for efficient text editing.
 */
private class GapBuffer(initialCapacity: Int) {
    private var chars = CharArray(initialCapacity)
    private var gapStart = 0
    private var gapEnd = chars.size
    private var contentLength = 0
    private val lineOffsets = mutableListOf(0)

    val length: Int get() = contentLength
    val lineCount: Int get() = lineOffsets.size

    fun charAt(position: Int): Char {
        require(position in 0 until contentLength)
        return if (position < gapStart) chars[position] else chars[position + gapSize]
    }

    fun substring(start: Int, end: Int): String {
        require(start in 0..end && end <= contentLength)
        return buildString(end - start) {
            for (i in start until end) append(charAt(i))
        }
    }

    override fun toString(): String = substring(0, contentLength)

    fun getLineForPosition(position: Int): Int {
        require(position in 0..contentLength)
        var left = 0
        var right = lineOffsets.size - 1
        while (left <= right) {
            val mid = (left + right) ushr 1
            when {
                lineOffsets[mid] > position -> right = mid - 1
                mid == lineOffsets.lastIndex || lineOffsets[mid + 1] > position -> return mid
                else -> left = mid + 1
            }
        }
        return 0
    }

    fun getLineStart(line: Int): Int {
        require(line in 0 until lineCount)
        return lineOffsets[line]
    }

    fun getLineEnd(line: Int): Int {
        require(line in 0 until lineCount)
        return if (line == lineOffsets.lastIndex) contentLength else lineOffsets[line + 1] - 1
    }

    fun getLineText(line: Int): String {
        val start = getLineStart(line)
        val end = getLineEnd(line)
        return substring(start, end)
    }

    fun insert(position: Int, text: String) {
        ensureGapCapacity(text.length)
        moveGapTo(position)
        text.toCharArray(chars, gapStart, 0, text.length)
        gapStart += text.length
        contentLength += text.length
        updateLineOffsets(position, text)
    }

    fun delete(start: Int, end: Int) {
        moveGapTo(start)
        val deleteCount = end - start
        gapEnd += deleteCount
        contentLength -= deleteCount
        rebuildLineOffsets()
    }

    private val gapSize: Int get() = gapEnd - gapStart

    private fun ensureGapCapacity(required: Int) {
        if (gapSize >= required) return
        val newCapacity = (contentLength + required).coerceAtLeast(chars.size * 2)
        val newChars = CharArray(newCapacity)
        chars.copyInto(newChars, 0, 0, gapStart)
        val newGapEnd = newCapacity - (chars.size - gapEnd)
        chars.copyInto(newChars, newGapEnd, gapEnd, chars.size)
        chars = newChars
        gapEnd = newGapEnd
    }

    private fun moveGapTo(position: Int) {
        when {
            position < gapStart -> {
                val count = gapStart - position
                chars.copyInto(chars, gapEnd - count, position, gapStart)
                gapStart -= count
                gapEnd -= count
            }
            position > gapStart -> {
                val count = position - gapStart
                chars.copyInto(chars, gapStart, gapEnd, gapEnd + count)
                gapStart += count
                gapEnd += count
            }
        }
    }

    private fun updateLineOffsets(insertPos: Int, text: String) {
        val line = getLineForPosition(insertPos)
        val offset = insertPos - lineOffsets[line]
        var currentPos = insertPos
        for (c in text) {
            if (c == '\n') {
                lineOffsets.add(line + 1, currentPos + 1)
            }
            currentPos++
        }
        val shift = text.length
        for (i in line + 1 until lineOffsets.size) {
            lineOffsets[i] += shift
        }
    }

    private fun rebuildLineOffsets() {
        lineOffsets.clear()
        lineOffsets.add(0)
        for (i in 0 until contentLength) {
            if (charAt(i) == '\n') lineOffsets.add(i + 1)
        }
    }
}
