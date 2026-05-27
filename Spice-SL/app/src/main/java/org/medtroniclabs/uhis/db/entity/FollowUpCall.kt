package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.appextensions.textOrEmpty
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL

@Entity(tableName = FOLLOW_UP_CALL)
data class FollowUpCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val followUpId: Long,
    val callDate: String,
    val duration: Double?,
    val attempts: Int = 0,
    val status: FollowUpCallStatus = FollowUpCallStatus.UN_SUCCESSFUL,
    val callType: String?,
    val isWillingToVisitUHC: Boolean?,
    val visitRejectReason: String?,
    val otherVisitRejectReason: String?,
    val unSuccessfulCallReason: String?,
    val wrongNumber: Boolean = false,
    val isSynced: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val calledByUserId: String = SecuredPreference.getUserFhirId(),
    val calledByUserName: String = SecuredPreference.getUserDetails()?.firstName.textOrEmpty() + " " +
        SecuredPreference.getUserDetails()?.lastName.textOrEmpty(),
    val calledByUserRole: String = SecuredPreference.getRole(),
)
