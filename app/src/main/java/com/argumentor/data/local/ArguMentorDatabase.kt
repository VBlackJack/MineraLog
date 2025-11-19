package com.argumentor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.argumentor.data.local.dao.ClaimDao
import com.argumentor.data.local.dao.EvidenceDao
import com.argumentor.data.local.dao.FallacyDao
import com.argumentor.data.local.dao.QuestionDao
import com.argumentor.data.local.dao.RebuttalDao
import com.argumentor.data.local.dao.SourceDao
import com.argumentor.data.local.dao.StatisticsDao
import com.argumentor.data.local.dao.TopicDao
import com.argumentor.data.local.entity.ClaimEntity
import com.argumentor.data.local.entity.ClaimFtsEntity
import com.argumentor.data.local.entity.EvidenceEntity
import com.argumentor.data.local.entity.FallacyEntity
import com.argumentor.data.local.entity.QuestionEntity
import com.argumentor.data.local.entity.RebuttalEntity
import com.argumentor.data.local.entity.SourceEntity
import com.argumentor.data.local.entity.TopicEntity
import com.argumentor.data.local.entity.TopicFtsEntity
import com.argumentor.data.local.model.RoomConverters

@Database(
    entities = [
        TopicEntity::class,
        TopicFtsEntity::class,
        ClaimEntity::class,
        ClaimFtsEntity::class,
        EvidenceEntity::class,
        SourceEntity::class,
        FallacyEntity::class,
        QuestionEntity::class,
        RebuttalEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class ArguMentorDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun claimDao(): ClaimDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun questionDao(): QuestionDao
    abstract fun rebuttalDao(): RebuttalDao
    abstract fun sourceDao(): SourceDao
    abstract fun fallacyDao(): FallacyDao
    abstract fun statisticsDao(): StatisticsDao
}
