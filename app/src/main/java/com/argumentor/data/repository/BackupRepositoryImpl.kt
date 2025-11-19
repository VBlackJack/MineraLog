package com.argumentor.data.repository

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.argumentor.R
import com.argumentor.core.i18n.StringProvider
import com.argumentor.core.i18n.labelRes
import com.argumentor.data.local.ArguMentorDatabase
import com.argumentor.data.local.dao.ClaimDao
import com.argumentor.data.local.dao.EvidenceDao
import com.argumentor.data.local.dao.QuestionDao
import com.argumentor.data.local.dao.RebuttalDao
import com.argumentor.data.local.dao.SourceDao
import com.argumentor.data.local.dao.TopicDao
import com.argumentor.data.local.entity.ClaimEntity
import com.argumentor.data.local.entity.EvidenceEntity
import com.argumentor.data.local.entity.QuestionEntity
import com.argumentor.data.local.entity.RebuttalEntity
import com.argumentor.data.local.entity.SourceEntity
import com.argumentor.data.local.entity.TopicEntity
import com.argumentor.data.local.model.ClaimPayload
import com.argumentor.data.local.model.DatabaseDump
import com.argumentor.data.local.model.EvidencePayload
import com.argumentor.data.local.model.QuestionPayload
import com.argumentor.data.local.model.RebuttalPayload
import com.argumentor.data.local.model.SourcePayload
import com.argumentor.data.local.model.TopicPayload
import com.argumentor.domain.model.TopicDetail
import com.argumentor.domain.model.TopicStance
import com.argumentor.domain.repository.BackupRepository
import com.argumentor.domain.repository.TopicRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val database: ArguMentorDatabase,
    private val topicDao: TopicDao,
    private val claimDao: ClaimDao,
    private val evidenceDao: EvidenceDao,
    private val sourceDao: SourceDao,
    private val questionDao: QuestionDao,
    private val rebuttalDao: RebuttalDao,
    private val topicRepository: TopicRepository,
    private val json: Json,
    private val stringProvider: StringProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupRepository {

    override suspend fun exportDatabaseToJson(destinationDir: File): File = withContext(dispatcher) {
        val dump = DatabaseDump(
            topics = topicDao.getAll().map { it.toPayload() },
            claims = claimDao.getAll().map { ClaimPayload(it.id, it.topicId, it.text, it.position.name, it.strength.name) },
            evidences = evidenceDao.getAll().map { EvidencePayload(it.id, it.claimId, it.type.name, it.content, it.sourceId, it.quality.name) },
            sources = sourceDao.getAll().map { SourcePayload(it.id, it.title, it.author, it.url, it.year) },
            questions = questionDao.getAll().map { QuestionPayload(it.id, it.claimId, it.prompt, it.expectedAnswer) },
            rebuttals = rebuttalDao.getAll().map { RebuttalPayload(it.id, it.claimId, it.text, it.style.name) }
        )
        if (!destinationDir.exists()) destinationDir.mkdirs()
        val file = File(destinationDir, "argumentor_backup_${System.currentTimeMillis()}.json")
        file.writeText(json.encodeToString(dump))
        file
    }

    override suspend fun importDatabaseFromJson(json: String) = withContext(dispatcher) {
        val payload = this@BackupRepositoryImpl.json.decodeFromString(DatabaseDump.serializer(), json)
        database.withTransaction {
            val topicIdMap = mutableMapOf<Long, Long>()
            val sourceIdMap = mutableMapOf<Long, Long>()
            val claimIdMap = mutableMapOf<Long, Long>()

            payload.sources.forEach { source ->
                val newId = sourceDao.upsert(
                    SourceEntity(
                        title = source.title,
                        author = source.author,
                        url = source.url,
                        year = source.year
                    )
                )
                sourceIdMap[source.id] = newId
            }

            payload.topics.forEach { topic ->
                val newId = topicDao.upsert(
                    TopicEntity(
                        title = topic.title,
                        summary = topic.summary,
                        stance = TopicStance.valueOf(topic.stance),
                        color = topic.color,
                        createdAt = topic.createdAt
                    )
                )
                topicIdMap[topic.id] = newId
            }

            payload.claims.forEach { claim ->
                val topicId = topicIdMap[claim.topicId] ?: return@forEach
                val newId = claimDao.upsert(
                    ClaimEntity(
                        topicId = topicId,
                        text = claim.text,
                        position = com.argumentor.domain.model.ClaimPosition.valueOf(claim.position),
                        strength = com.argumentor.domain.model.ArgumentStrength.valueOf(claim.strength)
                    )
                )
                claimIdMap[claim.id] = newId
            }

            payload.evidences.forEach { evidence ->
                evidenceDao.upsert(
                    EvidenceEntity(
                        claimId = claimIdMap[evidence.claimId] ?: return@forEach,
                        type = com.argumentor.domain.model.EvidenceType.valueOf(evidence.type),
                        content = evidence.content,
                        sourceId = evidence.sourceId?.let { sourceIdMap[it] },
                        quality = com.argumentor.domain.model.EvidenceQuality.valueOf(evidence.quality)
                    )
                )
            }

            payload.questions.forEach { question ->
                questionDao.upsert(
                    QuestionEntity(
                        claimId = claimIdMap[question.claimId] ?: return@forEach,
                        prompt = question.prompt,
                        expectedAnswer = question.expectedAnswer
                    )
                )
            }

            payload.rebuttals.forEach { rebuttal ->
                rebuttalDao.upsert(
                    RebuttalEntity(
                        claimId = claimIdMap[rebuttal.claimId] ?: return@forEach,
                        text = rebuttal.text,
                        style = com.argumentor.domain.model.RebuttalStyle.valueOf(rebuttal.style)
                    )
                )
            }
        }
    }

    override suspend fun exportTopicToMarkdown(topicId: Long): String = withContext(dispatcher) {
        val detail = topicRepository.observeTopicDetail(topicId).first()
            ?: throw IllegalStateException("Topic not found")
        buildMarkdown(detail)
    }

    override suspend fun exportTopicToPdf(topicId: Long, destination: File): File = withContext(dispatcher) {
        val detail = topicRepository.observeTopicDetail(topicId).first()
            ?: throw IllegalStateException("Topic not found")
        destination.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = 18f
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }
        var y = 40f

        fun drawLine(text: String, paint: Paint) {
            var current = ""
            text.split(" ").forEach { word ->
                val attempt = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(attempt) > pageInfo.pageWidth - 80) {
                    canvas.drawText(current, 40f, y, paint)
                    y += 18f
                    current = word
                } else {
                    current = attempt
                }
            }
            if (current.isNotEmpty()) {
                canvas.drawText(current, 40f, y, paint)
                y += 18f
            }
        }

        drawLine(detail.topic.title, titlePaint)
        drawLine("${detail.topic.summary}", bodyPaint)
        detail.claims.forEach { claimDetail ->
            y += 12f
            val positionLabel = stringProvider.getString(claimDetail.claim.position.labelRes())
            drawLine(
                stringProvider.getString(
                    R.string.pdf_claim_line,
                    positionLabel,
                    claimDetail.claim.text
                ),
                bodyPaint
            )
            claimDetail.evidences.forEach { evidence ->
                val typeLabel = stringProvider.getString(evidence.type.labelRes())
                val content = stringProvider.getString(R.string.evidence_item, typeLabel, evidence.content)
                drawLine(stringProvider.getString(R.string.pdf_evidence_line, content), bodyPaint)
            }
            claimDetail.rebuttals.forEach { rebuttal ->
                val styleLabel = stringProvider.getString(rebuttal.style.labelRes())
                val content = stringProvider.getString(R.string.rebuttal_item, styleLabel, rebuttal.text)
                drawLine(stringProvider.getString(R.string.pdf_rebuttal_line, content), bodyPaint)
            }
            claimDetail.questions.forEach { question ->
                val questionLine = stringProvider.getString(
                    R.string.question_item,
                    question.prompt,
                    question.expectedAnswer
                )
                drawLine(stringProvider.getString(R.string.pdf_question_line, questionLine), bodyPaint)
            }
        }
        document.finishPage(page)
        destination.outputStream().use { document.writeTo(it) }
        document.close()
        destination
    }

    private fun buildMarkdown(detail: TopicDetail): String = buildString {
        appendLine(stringProvider.getString(R.string.markdown_topic_title, detail.topic.title))
        appendLine()
        appendLine(detail.topic.summary)
        appendLine()
        detail.claims.forEach { claimDetail ->
            val positionLabel = stringProvider.getString(claimDetail.claim.position.labelRes())
            appendLine(
                stringProvider.getString(
                    R.string.markdown_claim_heading,
                    positionLabel,
                    claimDetail.claim.text
                )
            )
            appendLine()
            claimDetail.evidences.forEach { evidence ->
                val typeLabel = stringProvider.getString(evidence.type.labelRes())
                appendLine(
                    "- " + stringProvider.getString(
                        R.string.evidence_item,
                        typeLabel,
                        evidence.content
                    )
                )
            }
            claimDetail.questions.forEach { question ->
                appendLine(
                    "- " + stringProvider.getString(
                        R.string.question_item,
                        question.prompt,
                        question.expectedAnswer
                    )
                )
            }
            claimDetail.rebuttals.forEach { rebuttal ->
                val styleLabel = stringProvider.getString(rebuttal.style.labelRes())
                appendLine(
                    "- " + stringProvider.getString(
                        R.string.rebuttal_item,
                        styleLabel,
                        rebuttal.text
                    )
                )
            }
            appendLine()
        }
    }

    private fun TopicEntity.toPayload() = TopicPayload(
        id = id,
        title = title,
        summary = summary,
        stance = stance.name,
        color = color,
        createdAt = createdAt
    )
}
