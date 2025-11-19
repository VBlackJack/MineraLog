package net.meshcore.mineralog.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.domain.service.PhotoStorageService

@Singleton
class PhotoStorageServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PhotoStorageService {

    override suspend fun saveImageToInternalStorage(
        sourceUri: Uri,
        desiredFileName: String?
    ): String = withContext(ioDispatcher) {
        val photosDir = File(context.filesDir, PHOTO_DIRECTORY).apply {
            if (!exists() && !mkdirs()) {
                throw IOException("Unable to create photos directory: $absolutePath")
            }
        }

        val extension = resolveExtension(sourceUri)
        val targetName = desiredFileName ?: buildFileName(extension)
        val destination = File(photosDir, targetName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Unable to open input stream for URI: $sourceUri")

        destination.absolutePath
    }

    private fun buildFileName(extension: String): String {
        val sanitizedExtension = if (extension.startsWith('.')) extension else ".${extension}"
        return "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}${sanitizedExtension}"
    }

    private fun resolveExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return when {
            !extension.isNullOrBlank() -> ".${extension.lowercase(Locale.US)}"
            uri.toString().contains('.') -> uri.toString().substringAfterLast('.')
            else -> DEFAULT_EXTENSION
        }
    }

    companion object {
        private const val PHOTO_DIRECTORY = "mineral_photos"
        private const val DEFAULT_EXTENSION = ".jpg"
    }
}
