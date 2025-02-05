package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.model.PatientListRespModel

@Entity(tableName = "NCDFollowUp")
data class NCDFollowUp(
    @PrimaryKey
    val id: Long,
    val deleted: Boolean? = null,
    val isCompleted: Boolean? = null,
    val isWrongNumber: Boolean? = null,
    val isInitiated: Boolean? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val type: String? = null,
    val referredSiteId: String? = null,
    val identityType: String? = null,
    val identityValue: String? = null,
    val name: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val countyName: String? = null,
    val subCountyName: String? = null,
    val communityHealthUnitName: String? = null,
    val villageId: String? = null,
    val villageName: String? = null,
    val landmark: String? = null,
    val retryAttempts: Long? = null,
    val overDueCategories: ArrayList<String?>? = null,
    val dueDate: Long? = null,
    val referredReasons: ArrayList<String?>? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

@Entity(tableName = "NCDCallDetails")
data class NCDCallDetails(
    @PrimaryKey(autoGenerate = true)
    val localId:Long = 0,
    val id: Long,
    val villageId: String? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val appType: String = CommonUtils.isCommunityOrNot(),
    val referredSiteId: String? = null,
    val callDate: String? = null,
    val duration: Double? = null,
    val status: String? = null,
    val reason: String? = null,
    val otherReason: String? = null,
    val patientStatus: String? = null,
    val type: String? = null,
    val attempts: Long? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val visitedFacilityId: Long? = null,
    val otherVisitedFacilityName: String? = null,
    val isSynced: Boolean = false,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class ResponseNCDFollowUp(
    val id: Long,
    val deleted: Boolean? = null,
    val isCompleted: Boolean? = null,
    val isWrongNumber: Boolean? = null,
    val isInitiated: Boolean? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val type: String? = null,
    val referredSiteId: String? = null,
    val identityType: String? = null,
    val identityValue: String? = null,
    val name: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val countyName: String? = null,
    val subCountyName: String? = null,
    val communityHealthUnitName: String? = null,
    val villageId: String? = null,
    val villageName: String? = null,
    val landmark: String? = null,
    val retryAttempts: Long? = null,
    val overDueCategories: ArrayList<String?>? = null,
    val dueDate: String? = null,
    val referredReasons: ArrayList<String?>? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)


data class NCDFollowUpDownload(
    val followUps: List<ResponseNCDFollowUp>? = null,
    val followUpCriteria: NCDFollowUpCriteria? = null,
    val patientDetails: List<PatientListRespModel>? = null,
    val lastSyncTime: String,
)


data class NCDFollowUpCriteria(
    val followupAttempts: Int? = null,
    val screeningFollowupRemainingDays: Int? = null,
    val assessmentFollowupRemainingDays: Int? = null,
    val medicalReviewFollowupRemainingDays: Int? = null,
    val lostToFollowupRemainingDays: Int? = null
)

data class NCDFollowUpRequestCreate(
    val appType: String = CommonUtils.isCommunityOrNot(),
    val id: Long? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val type: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val isInitiated: Boolean = false,
    val followUpDetails: List<NCDCallDetails>? = null,
    val provenance: ProvanceDto = ProvanceDto()
)

@Entity(tableName = "NCDPatientDetailsEntity")
data class NCDPatientDetailsEntity(
    @PrimaryKey
    val id: String,
    val patientDetails: String? = null
)