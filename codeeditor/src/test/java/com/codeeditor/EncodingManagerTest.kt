package com.codeeditor

import com.codeeditor.util.EncodingManager
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Unit tests for the encoding manager.
 */
class EncodingManagerTest {

    @Test
    fun `default encoding is UTF-8`() {
        val manager = EncodingManager()
        assertEquals(StandardCharsets.UTF_8, manager.encoding)
    }

    @Test
    fun `set encoding by charset`() {
        val manager = EncodingManager()
        manager.setEncoding(StandardCharsets.ISO_8859_1)
        assertEquals(StandardCharsets.ISO_8859_1, manager.encoding)
    }

    @Test
    fun `set encoding by valid name`() {
        val manager = EncodingManager()
        val result = manager.setEncodingByName("UTF-16")
        assertTrue(result)
        assertEquals(StandardCharsets.UTF_16, manager.encoding)
    }

    @Test
    fun `set encoding by invalid name returns false`() {
        val manager = EncodingManager()
        val result = manager.setEncodingByName("INVALID_ENCODING")
        assertFalse(result)
    }

    @Test
    fun `detect UTF-8 from plain ASCII bytes`() {
        val manager = EncodingManager()
        val bytes = "Hello World".toByteArray(StandardCharsets.UTF_8)
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    fun `detect UTF-8 BOM`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(
            0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(),
            'H'.toByte(), 'i'.toByte()
        )
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    fun `detect UTF-16 BE BOM`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.UTF_16BE, detected)
    }

    @Test
    fun `detect UTF-16 LE BOM`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.UTF_16LE, detected)
    }

    @Test
    fun `detect ASCII`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(0x41, 0x42, 0x43) // ABC
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.US_ASCII, detected)
    }

    @Test
    fun `empty bytes defaults to UTF-8`() {
        val manager = EncodingManager()
        val detected = manager.detect(byteArrayOf())
        assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    fun `encode and decode roundtrip`() {
        val manager = EncodingManager()
        val text = "Hello World"
        val bytes = manager.encode(text)
        val decoded = manager.decode(bytes)
        assertEquals(text, decoded)
    }

    @Test
    fun `encode uses current encoding`() {
        val manager = EncodingManager()
        manager.setEncoding(StandardCharsets.UTF_16)
        val text = "Test"
        val bytes = manager.encode(text)
        val expected = text.toByteArray(StandardCharsets.UTF_16)
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun `decode uses current encoding`() {
        val manager = EncodingManager()
        val text = "Test"
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val decoded = manager.decode(bytes)
        assertEquals(text, decoded)
    }

    @Test
    fun `convert between encodings`() {
        val manager = EncodingManager()
        val text = "Hello"
        val utf8Bytes = text.toByteArray(StandardCharsets.UTF_8)
        val utf16Bytes = manager.convert(utf8Bytes, StandardCharsets.UTF_8, StandardCharsets.UTF_16)
        val expected = text.toByteArray(StandardCharsets.UTF_16)
        assertArrayEquals(expected, utf16Bytes)
    }

    @Test
    fun `get display name`() {
        val manager = EncodingManager()
        val name = manager.getDisplayName()
        assertNotNull(name)
        assertTrue(name.isNotEmpty())
    }

    @Test
    fun `supported encodings contains common encodings`() {
        val manager = EncodingManager()
        val supported = manager.supportedEncodings

        assertTrue(supported.contains(StandardCharsets.UTF_8))
        assertTrue(supported.contains(StandardCharsets.UTF_16))
        assertTrue(supported.contains(StandardCharsets.US_ASCII))
    }

    @Test
    fun `isSupported for valid encoding`() {
        assertTrue(EncodingManager.isSupported("UTF-8"))
        assertTrue(EncodingManager.isSupported("ISO-8859-1"))
        assertTrue(EncodingManager.isSupported("US-ASCII"))
    }

    @Test
    fun `isSupported for invalid encoding`() {
        assertFalse(EncodingManager.isSupported("NOT_A_REAL_ENCODING"))
    }

    @Test
    fun `detect valid UTF-8 with multi-byte chars`() {
        val manager = EncodingManager()
        val text = "Hello \u00E9\u00E0\u00FC" // accented chars
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val detected = manager.detect(bytes)
        assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    fun `detect invalid UTF-8 falls back gracefully`() {
        val manager = EncodingManager()
        // Invalid UTF-8 sequence
        val bytes = byteArrayOf(0x80.toByte(), 0x81.toByte(), 0x82.toByte())
        val detected = manager.detect(bytes)
        // Should not crash and should return some charset
        assertNotNull(detected)
    }

    @Test
    fun `UTF-32 BE BOM detection`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte())
        val detected = manager.detect(bytes)
        assertEquals(Charset.forName("UTF-32BE"), detected)
    }

    @Test
    fun `UTF-32 LE BOM detection`() {
        val manager = EncodingManager()
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00)
        val detected = manager.detect(bytes)
        assertEquals(Charset.forName("UTF-32LE"), detected)
    }
}
