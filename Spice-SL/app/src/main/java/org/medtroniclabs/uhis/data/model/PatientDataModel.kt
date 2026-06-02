package org.medtroniclabs.uhis.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class PatientDataModel(
    var skip: Int? = null,
    var limit: Int? = null,
    var tenantId: Long? = null,
    val searchText: String? = null,
    val isSearchUserOrgPatient: Boolean? = null,
    val globally: Boolean? = null,
    val operatingUnitId: Long? = null,
    val accountId: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val isLabtestReferred: Boolean? = null,
    val isMedicationPrescribed: Boolean? = null,
    var patientSort: SortModel? = null,
    var patientFilter: FilterModel? = null,
    val searchQRValue: String? = null,
    val qrCode: String? = null,
    var counsellorId: Long? = null,
    var unionId: List<Long>? = null,
    var filterUnionId: List<Long>? = null,
    val dateRange: String? = null,
    val diagnosis: List<String>? = null,
    var customDate: CustomDate? = null,
    val villageId: Long? = null,
    val referredSite: String? = null,
    var remainingAttempts: List<Int>? = null,
    var userId: Long? = null,
    var prescribedSiteId: Long? = null,
    val isPsychologist: Boolean? = null,
    val followUpType: String? = null,
    val callStatus: String? = null,
    val patientType: String? = null,
    val isParaCounsellingDisabled: Boolean? = null,
    var status: String? = null,
)

@Parcelize
data class SortModel(
    var origin: String? = null,
    var followUpType: String? = null,
    // Sort
    var isRedRisk: Boolean? = null,
    var isLatestAssessment: Boolean? = null,
    var isLastReviewDate: Boolean? = null,
    var isHighLowBp: Boolean? = null,
    var isHighLowBg: Boolean? = null,
    var isAssessmentDueDate: Boolean? = null,
    var isUpdated: Boolean? = null,
    var isCVDRisk: Boolean? = null,
    var isDateRange: Boolean? = null,
    var isScreeningDueDate: Boolean? = null,
    var isNextReviewDate: Boolean? = null,
) : Parcelable
