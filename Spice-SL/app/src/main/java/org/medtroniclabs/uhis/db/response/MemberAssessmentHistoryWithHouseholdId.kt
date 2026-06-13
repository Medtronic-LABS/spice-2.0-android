package org.medtroniclabs.uhis.db.response

import androidx.room.ColumnInfo
import androidx.room.Embedded
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity

/**
 * Result of assessment history mapped to particular household
 */
data class MemberAssessmentHistoryWithHouseholdId(
    @ColumnInfo("household_id")
    val householdId: Long,
    @Embedded
    val history: MemberAssessmentHistoryEntity,
)
