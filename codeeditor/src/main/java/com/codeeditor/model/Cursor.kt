package com.codeeditor.model

import androidx.annotation.IntRange

/**
 * Represents a cursor position in the text editor.
 *
 * @property line Zero-based line number
 * @property column Zero-based column number
 * @property absolutePosition Absolute character position in the text
 */
public data class Cursor(
    @IntRange(from = 0) val line: Int = 0,
    @IntRange(from = 0) val column: Int = 0,
    @IntRange(from = 0) val absolutePosition: Int = 0
) {
    /**
     * Creates a new Cursor moved by the specified delta.
     */
    public fun movedBy(lineDelta: Int = 0, columnDelta: Int = 0, positionDelta: Int = 0): Cursor {
        return Cursor(
            (line + lineDelta).coerceAtLeast(0),
            (column + columnDelta).coerceAtLeast(0),
            (absolutePosition + positionDelta).coerceAtLeast(0)
        )
    }

    override fun toString(): String = "Cursor(line=$line, col=$column, pos=$absolutePosition)"
}

/**
 * Represents a text selection range.
 *
 * @property start Selection start position (inclusive)
 * @property end Selection end position (exclusive)
 * @property isDirected Whether selection was made forward (end >= start)
 */
public data class Selection(
    @IntRange(from = 0) val start: Int = 0,
    @IntRange(from = 0) val end: Int = 0,
    val isDirected: Boolean = true
) {
    init {
        require(start >= 0) { "Selection start must be >= 0" }
        require(end >= 0) { "Selection end must be >= 0" }
    }

    /** Whether this selection has any length */
    public val isEmpty: Boolean get() = start == end

    /** The normalized start (smaller value) */
    public val normalizedStart: Int get() = if (isDirected) start else end

    /** The normalized end (larger value) */
    public val normalizedEnd: Int get() = if (isDirected) end else start

    /** Selection length */
    public val length: Int get() = kotlin.math.abs(end - start)

    /**
     * Returns the selected text from the given buffer.
     */
    public fun getSelectedText(buffer: TextBuffer): String {
        return buffer.substring(normalizedStart, normalizedEnd)
    }

    /**
     * Checks if the given position is within this selection.
     */
    public fun contains(position: Int): Boolean {
        return position in normalizedStart until normalizedEnd
    }

    /**
     * Returns a normalized selection (start <= end).
     */
    public fun normalized(): Selection = if (isDirected) this else Selection(end, start, true)

    override fun toString(): String = "Selection[$start, $end]${if (isEmpty) " (empty)" else ""}"
}

/**
 * Manages multi-cursor state for the editor.
 */
public class MultiCursorManager {
    private val cursors = mutableListOf<Cursor>()
    private val selections = mutableListOf<Selection>()
    private var primaryIndex = 0

    /** The primary (active) cursor */
    public val primaryCursor: Cursor get() = cursors.getOrElse(primaryIndex) { Cursor() }

    /** The primary selection */
    public val primarySelection: Selection get() = selections.getOrElse(primaryIndex) { Selection() }

    /** Number of active cursors */
    public val cursorCount: Int get() = cursors.size

    /** All cursors in this manager */
    public val allCursors: List<Cursor> get() = cursors.toList()

    /** All selections */
    public val allSelections: List<Selection> get() = selections.toList()

    /**
     * Initializes with a single cursor.
     */
    public fun setPrimaryCursor(cursor: Cursor) {
        cursors.clear()
        selections.clear()
        cursors.add(cursor)
        selections.add(Selection(cursor.absolutePosition, cursor.absolutePosition))
        primaryIndex = 0
    }

    /**
     * Adds an additional cursor (multi-cursor).
     */
    public fun addCursor(cursor: Cursor) {
        cursors.add(cursor)
        selections.add(Selection(cursor.absolutePosition, cursor.absolutePosition))
    }

    /**
     * Removes all cursors except the primary.
     */
    public fun collapseToPrimary() {
        val primary = primaryCursor
        cursors.clear()
        selections.clear()
        setPrimaryCursor(primary)
    }

    /**
     * Updates the selection for a specific cursor.
     */
    public fun updateSelection(cursorIndex: Int, selection: Selection) {
        if (cursorIndex in selections.indices) {
            selections[cursorIndex] = selection
        }
    }

    /**
     * Clears all cursors and selections.
     */
    public fun clear() {
        cursors.clear()
        selections.clear()
        primaryIndex = 0
    }
}
