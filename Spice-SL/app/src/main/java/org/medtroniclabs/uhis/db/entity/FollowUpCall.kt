package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL

@Entity(tableName = FOLLOW_UP_CALL)
data class FollowUpCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val followUpId: Long,
    val callDate: String,
    val duration: Long,
    val attempts: Int = 0,
    val status: FollowUpCallStatus = FollowUpCallStatus.UNSUCCESSFUL,
    val callType: String,
    val isWillingToVisitUHC: Boolean,
    val visitRejectReason: String,
    val otherVisitRejectReason: String,
    val unSuccessfulCallReason: String,
    val isSynced: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)
