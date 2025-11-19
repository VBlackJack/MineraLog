package net.meshcore.mineralog.domain.service

import android.net.Uri

interface PhotoStorageService {
    suspend fun saveImageToInternalStorage(sourceUri: Uri, desiredFileName: String? = null): String
}
