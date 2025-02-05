package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.model.SortModel


data class CustomDate(var startDate: String? = null, var endDate: String? = null)

data class FollowUpRequest(
    val searchText: String? = null,
    val type: String? = null,
    val siteId: String? = null,
    val skip: Long? = null,
    val limit: Int? = null,
    val sort: SortModel? = null,
    val remainingAttempts: List<Long>? = null,
    val dateRange: String? = null,
    val customDate: CustomDate? = null
)

data class RegisterCallResponse(
    val patientId: String? = null,
    val memberId: String? = null,
    val patientStatus: String? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val encounterType: String? = null,
    val type: String? = null,
    val chwId: String? = null,
    val attempts: Long? = null,
    val visits: Long? = null,
    val villageId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val phoneNumber: String? = null,
    val callType: String? = null,
    val completed: Boolean? = null
)

data class CallDetails(
    val callDate: String? = null,
    val duration: Double? = null,
    val status: String? = null,
    val reason: String? = null,
    val otherReason: String? = null,
    val patientStatus: String? = null,
    val attempts: Long? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val visitedFacilityId: Long? = null,
    val otherVisitedFacilityName: String? = null
)

data class FollowUpUpdateRequest(
    val requestFrom: String? = CommonUtils.requestFrom(),
    val id: String? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val attempts: Long? = null,
    val phoneNumber: String? = null,
    val type: String? = null,
    val referredSiteId: String? = null,
    val villageId: String? = null,
    val isInitiated: Boolean = false,
    val followUpDetails: List<CallDetails>? = null,
    val provenance: ProvanceDto? = null
)

data class PatientFollowUpEntity(
    val id: String? = null,
    val patientId: String? = null,
    val memberId: String? = null,
    val attempts: Int? = null,
    val isCompleted: Boolean? = null,
    val type: String? = null,
    val referredSiteId: String? = null,
    val villageId: String? = null,
    val calledAt: String? = null,
    val isInitiated: Boolean? = null,
    val name: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val diagnosis: String? = null,
    val phoneNumber: String? = null,
    val countyName: String? = null,
    val subCountyName: String? = null,
    val referredReasons: List<String>? = null,
    val overDueCategories: List<String>? = null,
    val communityHealthUnitName: String? = null,
    val villageName: String? = null,
    val landmark: String? = null,
    val referAssessment: Boolean? = null,
    val referredDateSince: Long? = null,
    val createdAt: String? = null,
    val retryAttempts: Int? = null,
    val screeningDateTime: String? = null,
    val assessmentDate: String? = null,
    val nextMedicalReviewDate: String? = null,
    val nextBpAssessmentDate: String? = null,
    val nextBgAssessmentDate: String? = null,
    val noOfDueDays: Int? = null,
    val dueDate: String? = null,
    val identityValue :String? =null
)

data class SortModelForFollowUp(
    var isScreeningDueDate: Boolean? = null,
    var isAssessmentDueDate: Boolean? = null,
    var isMedicalReviewDueDate: Boolean? = null
)