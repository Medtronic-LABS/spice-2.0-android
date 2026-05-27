package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP

@Entity(tableName = FOLLOW_UP)
data class FollowUp(
    @PrimaryKey(autoGenerate = true)
    val referenceId: Long = 0,
    val id: Long?,
    val householdId: String?,
    val memberId: String,
    val patientId: String,
    val encounterId: String?,
    var patientStatus: String?,
    var currentPatientStatus: String? = null,
    val reason: String? = null,
    var attempts: Int = 0,
    var successfulAttempts: Int = 0,
    var unsuccessfulAttempts: Int = 0,
    val type: String,
    val encounterType: String? = null, // It should not be null
    val encounterName: String? = null, // It should not be null
    val encounterDate: String? = null,
    val nextVisitDate: String? = null,
    val referredSiteId: String? = null,
    val villageId: String,
    var isCompleted: Boolean = false,
    var isWrongNumber: Boolean = false,
    var calledAt: Long? = null,
    var syncStatus: OfflineSyncStatus = OfflineSyncStatus.Success,
    var updatedAt: Long = System.currentTimeMillis(),
) {
    @Ignore
    var followUpDetails: List<FollowUpCall> = listOf()

    @Ignore
    var provenance: ProvanceDto = ProvanceDto(modifiedDate = System.currentTimeMillis().convertToUtcDateTime())
}
