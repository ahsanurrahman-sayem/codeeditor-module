package com.codeeditor

import com.codeeditor.model.TextBuffer
import com.codeeditor.model.TextChangeListener
import com.codeeditor.model.TextChangeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the TextBuffer gap buffer implementation.
 */
class TextBufferTest {

    private lateinit var buffer: TextBuffer

    @Before
    fun setup() {
        buffer = TextBuffer.create()
    }

    @Test
    fun `create empty buffer has zero length and line count`() {
        assertEquals(0, buffer.length)
        assertEquals(1, buffer.lineCount)
        assertFalse(buffer.isDirty)
    }

    @Test
    fun `insert text updates length and line count`() = runBlocking {
        buffer.insert(0, "Hello\nWorld")

        assertEquals(11, buffer.length)
        assertEquals(2, buffer.lineCount)
        assertTrue(buffer.isDirty)
    }

    @Test
    fun `insert multiple lines`() = runBlocking {
        buffer.insert(0, "Line1\nLine2\nLine3")

        assertEquals(3, buffer.lineCount)
        assertEquals("Line1", buffer.getLineText(0))
        assertEquals("Line2", buffer.getLineText(1))
        assertEquals("Line3", buffer.getLineText(2))
    }

    @Test
    fun `delete text reduces length`() = runBlocking {
        buffer.insert(0, "Hello World")
        buffer.delete(6, 11)

        assertEquals(6, buffer.length)
        assertEquals("Hello ", buffer.getText())
    }

    @Test
    fun `replace text`() = runBlocking {
        buffer.insert(0, "Hello World")
        buffer.replace(6, 11, "Kotlin")

        assertEquals("Hello Kotlin", buffer.getText())
    }

    @Test
    fun `charAt returns correct character`() = runBlocking {
        buffer.insert(0, "Hello")

        assertEquals('H', buffer.charAt(0))
        assertEquals('e', buffer.charAt(1))
        assertEquals('o', buffer.charAt(4))
    }

    @Test
    fun `substring returns correct text`() = runBlocking {
        buffer.insert(0, "Hello World")

        assertEquals("Hello", buffer.substring(0, 5))
        assertEquals("World", buffer.substring(6, 11))
    }

    @Test
    fun `getLineForPosition returns correct line`() = runBlocking {
        buffer.insert(0, "Line1\nLine2\nLine3")

        assertEquals(0, buffer.getLineForPosition(0))
        assertEquals(0, buffer.getLineForPosition(4))
        assertEquals(1, buffer.getLineForPosition(6))
        assertEquals(2, buffer.getLineForPosition(12))
    }

    @Test
    fun `getLineStart returns correct offset`() = runBlocking {
        buffer.insert(0, "Line1\nLine2\nLine3")

        assertEquals(0, buffer.getLineStart(0))
        assertEquals(6, buffer.getLineStart(1))
        assertEquals(12, buffer.getLineStart(2))
    }

    @Test
    fun `markClean resets dirty flag`() = runBlocking {
        buffer.insert(0, "text")
        assertTrue(buffer.isDirty)

        buffer.markClean()
        assertFalse(buffer.isDirty)
    }

    @Test
    fun `fromText creates buffer with content`() {
        val buf = TextBuffer.fromText("Hello\nWorld")

        assertEquals(11, buf.length)
        assertEquals(2, buf.lineCount)
        assertEquals("Hello", buf.getLineText(0))
    }

    @Test
    fun `text change listener is called`() = runBlocking {
        var callCount = 0
        val listener = object : TextChangeListener {
            override fun onTextChanged(start: Int, end: Int, type: TextChangeType) {
                callCount++
            }
        }

        buffer.addChangeListener(listener)
        buffer.insert(0, "test")

        assertEquals(1, callCount)

        buffer.removeChangeListener(listener)
        buffer.insert(4, "more")
        assertEquals(1, callCount) // Should not increase
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `insert at invalid position throws`(): Unit = runBlocking {
        buffer.insert(1, "text")
    }

    @Test
    fun `large text handling`() = runBlocking {
        val lines = List(1000) { "This is line number $it with some content" }
        val text = lines.joinToString("\n")
        buffer.insert(0, text)

        assertEquals(1000, buffer.lineCount)
        assertTrue(buffer.length > 40000)
    }

    @Test
    fun `concurrent inserts maintain consistency`() = runBlocking {
        buffer.insert(0, "base")

        // Simulate concurrent operations
        buffer.insert(0, "pre-")
        buffer.insert(9, "-post")

        assertEquals("pre-base-post", buffer.getText())
    }
}
