package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus

@Entity(tableName = "FollowUp")
data class FollowUp(
    @PrimaryKey(autoGenerate = true)
    val referenceId: Long = 0,
    val id: Long?,
    val householdId: String?,
    val memberId: String,
    val patientId: String,
    val encounterId: String?,
    val patientStatus: String,
    var currentPatientStatus: String? = null,
    val reason: String ? =null,
    var attempts: Int = 0,
    var successfulAttempts: Int = 0,
    var unsuccessfulAttempts: Int = 0,
    val type: String,
    val encounterType: String? = null, // It should not null
    val encounterDate: String? = null,
    val nextVisitDate: String? = null,
    val referredSiteId: String? = null,
    val villageId: String,
    var isCompleted: Boolean = false,
    var isWrongNumber: Boolean = false,
    var calledAt: Long? = null,
    var syncStatus: OfflineSyncStatus = OfflineSyncStatus.Success,
    var updatedAt: Long = System.currentTimeMillis()
) {
    @Ignore
    var followUpDetails : List<FollowUpCall> = listOf()

    @Ignore
    var provenance: ProvanceDto = ProvanceDto(modifiedDate = System.currentTimeMillis().convertToUtcDateTime())
}
