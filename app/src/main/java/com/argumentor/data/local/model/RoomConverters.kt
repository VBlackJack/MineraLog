package com.argumentor.data.local.model

import androidx.room.TypeConverter
import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.ClaimPosition
import com.argumentor.domain.model.EvidenceQuality
import com.argumentor.domain.model.EvidenceType
import com.argumentor.domain.model.RebuttalStyle
import com.argumentor.domain.model.TopicStance

class RoomConverters {
    @TypeConverter
    fun fromTopicStance(value: TopicStance?): String? = value?.name

    @TypeConverter
    fun toTopicStance(value: String?): TopicStance? = value?.let { TopicStance.valueOf(it) }

    @TypeConverter
    fun fromClaimPosition(value: ClaimPosition?): String? = value?.name

    @TypeConverter
    fun toClaimPosition(value: String?): ClaimPosition? = value?.let { ClaimPosition.valueOf(it) }

    @TypeConverter
    fun fromStrength(value: ArgumentStrength?): String? = value?.name

    @TypeConverter
    fun toStrength(value: String?): ArgumentStrength? = value?.let { ArgumentStrength.valueOf(it) }

    @TypeConverter
    fun fromEvidenceType(value: EvidenceType?): String? = value?.name

    @TypeConverter
    fun toEvidenceType(value: String?): EvidenceType? = value?.let { EvidenceType.valueOf(it) }

    @TypeConverter
    fun fromEvidenceQuality(value: EvidenceQuality?): String? = value?.name

    @TypeConverter
    fun toEvidenceQuality(value: String?): EvidenceQuality? = value?.let { EvidenceQuality.valueOf(it) }

    @TypeConverter
    fun fromRebuttalStyle(value: RebuttalStyle?): String? = value?.name

    @TypeConverter
    fun toRebuttalStyle(value: String?): RebuttalStyle? = value?.let { RebuttalStyle.valueOf(it) }
}
