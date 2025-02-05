package com.medtroniclabs.opensource.model

data class PatientsDataModel(
    var skip: Long? = null,
    var limit: Int? = null,
    val villageIds: List<Long>? = null,
    val searchText: String? = null,
    val siteId: String? = null,
    val districtId: Long? = null,
    val referencePatientId: String? = null,
    val filter: MedicalReviewFilterModel? = null,
    val sort: SortModel? = null,
    val type: String? = null,
    var countryId: Long? = null,
    var tenantId: Long? = null
)

data class MedicalReviewFilterModel(
    val patientStatus: List<String>? = null,
    val visitDate: List<String>? = null,
    val labTestReferredOn: String? = null,
    val prescriptionReferredOn: String? = null,
    val medicalReviewDate: String? = null,
    val enrollmentStatus: String? = null,
    val isRedRiskPatient: Boolean? = null,
    val cvdRiskLevel: String? = null,
    val assessmentDate: String? = null,
)

data class SortModel(
    val isRedRisk: Boolean? = null,
    val isLatestAssessment: Boolean? = null,
    val isMedicalReviewDueDate: Boolean? = null,
    val isHighLowBp: Boolean? = null,
    val isHighLowBg: Boolean? = null,
    val isAssessmentDueDate: Boolean? = null
)
