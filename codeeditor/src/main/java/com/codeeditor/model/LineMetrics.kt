package com.codeeditor.model

import android.graphics.Rect
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

/**
 * Metrics for a single line in the editor.
 *
 * @property lineNumber Zero-based line number
 * @property startOffset Character offset where this line starts
 * @property endOffset Character offset where this line ends (exclusive)
 * @property visibleWidth Width of the visible portion in pixels
 * @property height Line height in pixels
 * @property baseline Baseline position from top of line
 */
public data class LineMetrics(
    @IntRange(from = 0) val lineNumber: Int,
    @IntRange(from = 0) val startOffset: Int,
    @IntRange(from = 0) val endOffset: Int,
    @FloatRange(from = 0.0) val visibleWidth: Float,
    @FloatRange(from = 0.0) val height: Float,
    @FloatRange(from = 0.0) val baseline: Float
) {
    /** Character count on this line */
    public val length: Int get() = endOffset - startOffset

    /** Rect for this line's bounds */
    public fun getBounds(y: Float): Rect = Rect(
        0, y.toInt(),
        visibleWidth.toInt(), (y + height).toInt()
    )
}

/**
 * Manages line metrics and layout calculations for the editor.
 */
public class LineMetricsManager {
    private val metricsCache = mutableMapOf<Int, LineMetrics>()
    private var cachedLineHeight = 0f
    private var cachedBaseline = 0f

    /**
     * Updates the cached line measurements from paint metrics.
     *
     * @param fontMetrics Paint font metrics for calculations
     */
    public fun updateMeasurements(fontMetrics: android.graphics.Paint.FontMetrics) {
        cachedLineHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading
        cachedBaseline = -fontMetrics.ascent
    }

    /**
     * Gets or creates metrics for a specific line.
     *
     * @param lineNumber Line number (0-based)
     * @param buffer Text buffer to measure
     * @param textPaint Paint used for text measurement
     * @return Computed line metrics
     */
    public fun getLineMetrics(
        lineNumber: Int,
        buffer: TextBuffer,
        textPaint: android.graphics.Paint
    ): LineMetrics {
        metricsCache[lineNumber]?.let { return it }

        val startOffset = buffer.getLineStart(lineNumber)
        val endOffset = buffer.getLineEnd(lineNumber)
        val lineText = buffer.getLineText(lineNumber)
        val width = textPaint.measureText(lineText)

        val metrics = LineMetrics(
            lineNumber = lineNumber,
            startOffset = startOffset,
            endOffset = endOffset,
            visibleWidth = width,
            height = cachedLineHeight,
            baseline = cachedBaseline
        )
        metricsCache[lineNumber] = metrics
        return metrics
    }

    /**
     * Gets the total height for a range of lines.
     */
    public fun getTotalHeight(lineCount: Int): Float = cachedLineHeight * lineCount

    /**
     * Gets the Y coordinate for a specific line.
     */
    public fun getLineY(lineNumber: Int): Float = cachedLineHeight * lineNumber

    /**
     * Gets the line number at a specific Y coordinate.
     */
    public fun getLineAtY(y: Float): Int = (y / cachedLineHeight).toInt().coerceAtLeast(0)

    /**
     * Gets the column at a specific X coordinate on a line.
     */
    public fun getColumnAtX(
        x: Float,
        lineNumber: Int,
        buffer: TextBuffer,
        textPaint: android.graphics.Paint
    ): Int {
        val lineText = buffer.getLineText(lineNumber)
        if (lineText.isEmpty() || x <= 0) return 0

        var low = 0
        var high = lineText.length
        while (low < high) {
            val mid = (low + high) ushr 1
            val width = textPaint.measureText(lineText, 0, mid)
            if (width < x) low = mid + 1 else high = mid
        }
        return low.coerceIn(0, lineText.length)
    }

    /**
     * Clears the metrics cache. Call this when text changes significantly.
     */
    public fun clearCache() {
        metricsCache.clear()
    }
}
