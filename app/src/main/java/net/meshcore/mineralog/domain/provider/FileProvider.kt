package net.meshcore.mineralog.domain.provider

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction layer for file system operations.
 *
 * Decouples ViewModels from Android Context for file operations.
 * Enables unit testing of file-related logic without Android framework.
 * Follows Dependency Inversion Principle (DIP).
 *
 * Sprint 2: Architecture Refactoring - Dependency Inversion Principle (DIP)
 * Target: Make file operations testable in ViewModels
 *
 * @see net.meshcore.mineralog.ui.screens.home.HomeViewModel
 */
interface FileProvider {

    /**
     * Get the application's cache directory.
     *
     * @return Cache directory file
     */
    fun getCacheDir(): File

    /**
     * Get the application's files directory.
     *
     * @return Files directory
     */
    fun getFilesDir(): File

    /**
     * Get the application's external files directory.
     *
     * @param type Optional subdirectory type (e.g., Environment.DIRECTORY_PICTURES)
     * @return External files directory, or null if not available
     */
    fun getExternalFilesDir(type: String? = null): File?

    /**
     * Create a temporary file in the cache directory.
     *
     * @param prefix File name prefix
     * @param suffix File name suffix (extension)
     * @return Temporary file
     */
    fun createTempFile(prefix: String, suffix: String): File

    /**
     * Open an input stream for reading from a content URI.
     *
     * @param uri Content URI to read from
     * @return Input stream, or null if URI cannot be opened
     */
    fun openInputStream(uri: Uri): InputStream?

    /**
     * Open an output stream for writing to a content URI.
     *
     * @param uri Content URI to write to
     * @param mode File mode (e.g., "w" for write, "wa" for append)
     * @return Output stream, or null if URI cannot be opened
     */
    fun openOutputStream(uri: Uri, mode: String = "w"): OutputStream?

    /**
     * Delete a file.
     *
     * @param file File to delete
     * @return true if deleted successfully, false otherwise
     */
    fun deleteFile(file: File): Boolean

    /**
     * Check if a file exists.
     *
     * @param file File to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(file: File): Boolean

    /**
     * Get file name from a content URI.
     *
     * @param uri Content URI
     * @return File name, or null if not available
     */
    fun getFileName(uri: Uri): String?

    /**
     * Get MIME type from a content URI.
     *
     * @param uri Content URI
     * @return MIME type, or null if not available
     */
    fun getMimeType(uri: Uri): String?
}

/**
 * Android implementation of FileProvider.
 *
 * Uses Android Context for file system operations.
 */
class AndroidFileProvider(
    private val context: Context
) : FileProvider {

    override fun getCacheDir(): File {
        return context.cacheDir
    }

    override fun getFilesDir(): File {
        return context.filesDir
    }

    override fun getExternalFilesDir(type: String?): File? {
        return context.getExternalFilesDir(type)
    }

    override fun createTempFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }

    override fun openInputStream(uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }
    }

    override fun openOutputStream(uri: Uri, mode: String): OutputStream? {
        return try {
            context.contentResolver.openOutputStream(uri, mode)
        } catch (e: Exception) {
            null
        }
    }

    override fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    override fun fileExists(file: File): Boolean {
        return file.exists()
    }

    override fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getMimeType(uri: Uri): String? {
        return try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
    }
}
