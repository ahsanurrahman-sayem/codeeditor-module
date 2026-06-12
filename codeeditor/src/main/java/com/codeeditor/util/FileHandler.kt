package com.codeeditor.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.codeeditor.model.TextBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*nimport java.nio.charset.Charset

/**
 * Handles file operations including large file support,
 * stream-based loading/saving, SAF integration, and encoding preservation.
 *
 * @param context Android context for content resolver access
 */
public class FileHandler(private val context: Context) {
    private val encodingManager = EncodingManager()
    private val bufferSize = 8192

    /**
     * Loads file content from a URI using streaming.
     *
     * @param uri Content URI or file URI
     * @param charset Optional charset override
     * @return Loaded text content
     */
    public suspend fun loadFromUri(uri: Uri, charset: Charset? = null): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val detected = charset ?: encodingManager.detect(bytes)
            encodingManager.setEncoding(detected)
            encodingManager.decode(bytes)
        } ?: throw FileNotFoundException("Cannot open input stream for $uri")
    }

    /**
     * Saves content to a URI using streaming.
     *
     * @param uri Content URI or file URI
     * @param content Text content to save
     * @param charset Encoding to use (preserves current if null)
     */
    public suspend fun saveToUri(
        uri: Uri,
        content: String,
        charset: Charset? = null
    ) = withContext(Dispatchers.IO) {
        charset?.let { encodingManager.setEncoding(it) }
        val bytes = encodingManager.encode(content)

        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(bytes)
        } ?: throw FileNotFoundException("Cannot open output stream for $uri")
    }

    /**
     * Loads a file with progress callback for large files.
     *
     * @param uri File URI
     * @param onProgress Called with (bytesRead, totalBytes) during loading
     * @return Loaded text content
     */
    public suspend fun loadLargeFile(
        uri: Uri,
        onProgress: ((Long, Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileSize = getFileSize(uri)

        resolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            var totalRead = 0L

            while (stream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                onProgress?.invoke(totalRead, fileSize)
            }

            val bytes = output.toByteArray()
            val detected = encodingManager.detect(bytes)
            encodingManager.setEncoding(detected)
            encodingManager.decode(bytes)
        } ?: throw FileNotFoundException("Cannot open input stream for $uri")
    }

    /**
     * Saves large content with streaming to avoid memory issues.
     *
     * @param uri Target URI
     * @param reader Source reader
     * @param charset Output encoding
     */
    public suspend fun saveStreaming(
        uri: Uri,
        reader: Reader,
        charset: Charset? = null
    ) = withContext(Dispatchers.IO) {
        charset?.let { encodingManager.setEncoding(it) }

        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            val writer = stream.writer(encodingManager.encoding)
            val buffer = CharArray(bufferSize)
            var charsRead: Int

            while (reader.read(buffer).also { charsRead = it } != -1) {
                writer.write(buffer, 0, charsRead)
            }
            writer.flush()
        } ?: throw FileNotFoundException("Cannot open output stream for $uri")
    }

    /**
     * Detects encoding from a file URI.
     *
     * @param uri File URI
     * @return Detected charset
     */
    public suspend fun detectEncoding(uri: Uri): Charset = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            // Read up to 4KB for detection
            val sample = stream.read(4096)
            encodingManager.detect(sample)
        } ?: StandardCharsets.UTF_8
    }

    /**
     * Gets file size from URI.
     *
     * @param uri File URI
     * @return File size in bytes, or -1 if unknown
     */
    public fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Gets file name from URI.
     *
     * @param uri File URI
     * @return File name or null
     */
    public fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }

    /**
     * Creates a new file via SAF.
     *
     * @param parentUri Parent directory URI
     * @param fileName Desired file name
     * @param mimeType MIME type
     * @return URI of created file
     */
    public fun createFile(parentUri: Uri, fileName: String, mimeType: String = "text/plain"): Uri? {
        return try {
            android.provider.DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                mimeType,
                fileName
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a file via SAF.
     *
     * @param uri File URI to delete
     * @return true if deleted successfully
     */
    public fun deleteFile(uri: Uri): Boolean {
        return try {
            android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a URI can be written to.
     *
     * @param uri URI to check
     * @return true if writable
     */
    public fun canWrite(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "wt")?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
