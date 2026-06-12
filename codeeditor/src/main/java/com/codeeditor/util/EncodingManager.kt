package com.codeeditor.util

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Manages text encoding detection, conversion, and preservation.
 *
 * Supports UTF-8, UTF-16, UTF-32, ASCII, ISO-8859 variants,
 * and automatic encoding detection from byte arrays.
 */
public class EncodingManager {
    private var currentEncoding: Charset = StandardCharsets.UTF_8

    /** Currently active encoding */
    public val encoding: Charset get() = currentEncoding

    /** List of supported encodings */
    public val supportedEncodings: List<Charset> = listOf(
        StandardCharsets.UTF_8,
        StandardCharsets.UTF_16,
        StandardCharsets.UTF_16BE,
        StandardCharsets.UTF_16LE,
        StandardCharsets.US_ASCII,
        Charset.forName("ISO-8859-1"),
        Charset.forName("ISO-8859-15"),
        Charset.forName("Windows-1252"),
        Charset.forName("UTF-32"),
        Charset.forName("UTF-32BE"),
        Charset.forName("UTF-32LE")
    )

    /**
     * Sets the current encoding.
     *
     * @param charset Encoding to use
     */
    public fun setEncoding(charset: Charset) {
        currentEncoding = charset
    }

    /**
     * Sets encoding by name.
     *
     * @param name Encoding name (e.g., "UTF-8", "ISO-8859-1")
     * @return true if encoding was set successfully
     */
    public fun setEncodingByName(name: String): Boolean {
        return try {
            currentEncoding = Charset.forName(name)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects encoding from a byte array using BOM detection
     * and heuristic analysis.
     *
     * @param bytes Byte array to analyze
     * @return Detected charset (defaults to UTF-8)
     */
    public fun detect(bytes: ByteArray): Charset {
        if (bytes.isEmpty()) return StandardCharsets.UTF_8

        // Check BOM
        val bomEncoding = detectBOM(bytes)
        if (bomEncoding != null) {
            return bomEncoding
        }

        // Check for UTF-16 without BOM
        if (looksLikeUTF16LE(bytes)) return StandardCharsets.UTF_16LE
        if (looksLikeUTF16BE(bytes)) return StandardCharsets.UTF_16BE

        // Check for valid UTF-8
        if (isValidUTF8(bytes)) return StandardCharsets.UTF_8

        // Check for ASCII
        if (isASCII(bytes)) return StandardCharsets.US_ASCII

        // Default to UTF-8
        return StandardCharsets.UTF_8
    }

    /**
     * Converts bytes to string using current encoding.
     *
     * @param bytes Byte array
     * @return Decoded string
     */
    public fun decode(bytes: ByteArray): String {
        return String(bytes, currentEncoding)
    }

    /**
     * Converts string to bytes using current encoding.
     *
     * @param text String to encode
     * @return Encoded bytes
     */
    public fun encode(text: String): ByteArray {
        return text.toByteArray(currentEncoding)
    }

    /**
     * Converts bytes from one encoding to another.
     *
     * @param bytes Source bytes
     * @param from Source encoding
     * @param to Target encoding
     * @return Converted bytes
     */
    public fun convert(bytes: ByteArray, from: Charset, to: Charset): ByteArray {
        return String(bytes, from).toByteArray(to)
    }

    /**
     * Gets encoding display name.
     */
    public fun getDisplayName(charset: Charset = currentEncoding): String {
        return charset.displayName()
    }

    private fun detectBOM(bytes: ByteArray): Charset? {
        return when {
            bytes.size >= 3 &&
                    bytes[0] == 0xEF.toByte() &&
                    bytes[1] == 0xBB.toByte() &&
                    bytes[2] == 0xBF.toByte() -> StandardCharsets.UTF_8
            bytes.size >= 4 &&
                    bytes[0] == 0x00.toByte() &&
                    bytes[1] == 0x00.toByte() &&
                    bytes[2] == 0xFE.toByte() &&
                    bytes[3] == 0xFF.toByte() -> Charset.forName("UTF-32BE")
            bytes.size >= 4 &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xFE.toByte() &&
                    bytes[2] == 0x00.toByte() &&
                    bytes[3] == 0x00.toByte() -> Charset.forName("UTF-32LE")
            bytes.size >= 2 &&
                    bytes[0] == 0xFE.toByte() &&
                    bytes[1] == 0xFF.toByte() -> StandardCharsets.UTF_16BE
            bytes.size >= 2 &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xFE.toByte() -> StandardCharsets.UTF_16LE
            else -> null
        }
    }

    private fun looksLikeUTF16LE(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        var nullCount = 0
        for (i in 1 until bytes.size step 2) {
            if (bytes[i] == 0x00.toByte()) nullCount++
        }
        return nullCount > bytes.size / 4
    }

    private fun looksLikeUTF16BE(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        var nullCount = 0
        for (i in 0 until bytes.size step 2) {
            if (bytes[i] == 0x00.toByte()) nullCount++
        }
        return nullCount > bytes.size / 4
    }

    private fun isValidUTF8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b < 0x80 -> i++
                b and 0xE0 == 0xC0 -> {
                    if (i + 1 >= bytes.size || bytes[i + 1].toInt() and 0xC0 != 0x80) return false
                    i += 2
                }
                b and 0xF0 == 0xE0 -> {
                    if (i + 2 >= bytes.size ||
                        bytes[i + 1].toInt() and 0xC0 != 0x80 ||
                        bytes[i + 2].toInt() and 0xC0 != 0x80
                    ) return false
                    i += 3
                }
                b and 0xF8 == 0xF0 -> {
                    if (i + 3 >= bytes.size ||
                        bytes[i + 1].toInt() and 0xC0 != 0x80 ||
                        bytes[i + 2].toInt() and 0xC0 != 0x80 ||
                        bytes[i + 3].toInt() and 0xC0 != 0x80
                    ) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun isASCII(bytes: ByteArray): Boolean {
        return bytes.all { it in 0x00..0x7F }
    }

    public companion object {
        /**
         * Checks if a charset name is supported.
         */
        @JvmStatic
        public fun isSupported(name: String): Boolean {
            return try {
                Charset.forName(name)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
