package com.argumentor.di

import android.content.Context
import androidx.room.Room
import com.argumentor.core.i18n.AndroidStringProvider
import com.argumentor.core.i18n.StringProvider
import com.argumentor.data.local.ArguMentorDatabase
import com.argumentor.data.local.FallacySeeds
import com.argumentor.data.local.dao.ClaimDao
import com.argumentor.data.local.dao.EvidenceDao
import com.argumentor.data.local.dao.FallacyDao
import com.argumentor.data.local.dao.QuestionDao
import com.argumentor.data.local.dao.RebuttalDao
import com.argumentor.data.local.dao.SourceDao
import com.argumentor.data.local.dao.StatisticsDao
import com.argumentor.data.local.dao.TopicDao
import com.argumentor.data.repository.BackupRepositoryImpl
import com.argumentor.data.repository.FallacyRepositoryImpl
import com.argumentor.data.repository.StatisticsRepositoryImpl
import com.argumentor.data.repository.TopicRepositoryImpl
import com.argumentor.domain.repository.BackupRepository
import com.argumentor.domain.repository.FallacyRepository
import com.argumentor.domain.repository.StatisticsRepository
import com.argumentor.domain.repository.TopicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArguMentorDatabase {
        val database = Room.databaseBuilder(
            context,
            ArguMentorDatabase::class.java,
            "argumentor.db"
        ).fallbackToDestructiveMigration()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.fallacyDao()
            if (dao.count() == 0) {
                dao.insertAll(FallacySeeds.build(context))
            }
        }
        return database
    }

    @Provides
    fun provideTopicDao(database: ArguMentorDatabase): TopicDao = database.topicDao()

    @Provides
    fun provideClaimDao(database: ArguMentorDatabase): ClaimDao = database.claimDao()

    @Provides
    fun provideEvidenceDao(database: ArguMentorDatabase): EvidenceDao = database.evidenceDao()

    @Provides
    fun provideQuestionDao(database: ArguMentorDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideRebuttalDao(database: ArguMentorDatabase): RebuttalDao = database.rebuttalDao()

    @Provides
    fun provideSourceDao(database: ArguMentorDatabase): SourceDao = database.sourceDao()

    @Provides
    fun provideFallacyDao(database: ArguMentorDatabase): FallacyDao = database.fallacyDao()

    @Provides
    fun provideStatisticsDao(database: ArguMentorDatabase): StatisticsDao = database.statisticsDao()

    @Provides
    @Singleton
    fun provideStringProvider(@ApplicationContext context: Context): StringProvider =
        AndroidStringProvider(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideTopicRepository(
        topicDao: TopicDao,
        claimDao: ClaimDao,
        evidenceDao: EvidenceDao,
        questionDao: QuestionDao,
        rebuttalDao: RebuttalDao
    ): TopicRepository = TopicRepositoryImpl(topicDao, claimDao, evidenceDao, questionDao, rebuttalDao)

    @Provides
    @Singleton
    fun provideStatisticsRepository(statisticsDao: StatisticsDao): StatisticsRepository =
        StatisticsRepositoryImpl(statisticsDao)

    @Provides
    @Singleton
    fun provideFallacyRepository(fallacyDao: FallacyDao): FallacyRepository =
        FallacyRepositoryImpl(fallacyDao)

    @Provides
    @Singleton
    fun provideBackupRepository(
        database: ArguMentorDatabase,
        topicDao: TopicDao,
        claimDao: ClaimDao,
        evidenceDao: EvidenceDao,
        sourceDao: SourceDao,
        questionDao: QuestionDao,
        rebuttalDao: RebuttalDao,
        topicRepository: TopicRepository,
        json: Json,
        stringProvider: StringProvider
    ): BackupRepository = BackupRepositoryImpl(
        database = database,
        topicDao = topicDao,
        claimDao = claimDao,
        evidenceDao = evidenceDao,
        sourceDao = sourceDao,
        questionDao = questionDao,
        rebuttalDao = rebuttalDao,
        topicRepository = topicRepository,
        json = json,
        stringProvider = stringProvider
    )
}
