package com.argumentor.domain.repository

import java.io.File

interface BackupRepository {
    suspend fun exportDatabaseToJson(destinationDir: File): File
    suspend fun importDatabaseFromJson(json: String)
    suspend fun exportTopicToMarkdown(topicId: Long): String
    suspend fun exportTopicToPdf(topicId: Long, destination: File): File
}
