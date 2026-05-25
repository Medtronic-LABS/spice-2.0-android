package org.medtroniclabs.uhis.data.model

data class FilterModel(
    var origin: String? = null,
    var selectedTab: String? = null,
    var screeningReferral: Boolean? = null,
    var isFromPcFilter: Boolean? = null,
    var villagePreFilled: Boolean? = null,
    // filterTypeA
    var medicalReviewDate: String? = null,
    var isRedRiskPatient: Boolean? = null,
    var patientStatus: String? = null,
    var cvdRiskLevel: String? = null,
    var riskStatus: String? = null,
    var assessmentDate: String? = null,
    var upazilaTenantId: Long? = null,
    var villageId: Long? = null,
    var cvdRisk: String? = null,
    var diagnosisType: String? = null,
    var filterUnionId: Long? = null,
    var selectedParaCounselor: SiteRoleResponse? = null,
    var sessionDate: String? = null,
    var paraCounsellingStatus: ArrayList<String>? = null,
    var isDefaultPcFilter: Boolean = true,
    var registrationDate: String? = null,
    var healthCondition: String? = null,
    var customRegistrationOption: Boolean? = null,
    var customRegistrationDate: CustomDate? = null,
    // filterTypeB
    var labTestReferredDate: String? = null,
    var medicationPrescribedDate: String? = null,
    // filterTypeC
    var diagnosis: ArrayList<String>? = null,
    var dateRange: String? = null,
    var customOption: Boolean? = null,
    var customDate: CustomDate? = null,
    var subVillageId: Long? = null,
    var referredSite: String? = null,
    var remainingAttempts: List<Int>? = null,
    var shasthyaShebikaId: Long? = null,
    var callStatus: String? = null,
    var patientType: String? = null,
    var countyId: Long? = null,
    var subCountyId: Long? = null,
    var accountIds: List<Long>? = null,
    var allAccountIds: List<Long>? = null,
)

data class PatientFilterByDivisionDistrictUpazila(
    val countyId: Long? = null,
    val subCountyId: Long? = null,
    val accountIds: List<Long>? = null,
)

class CustomDate(var startDate: String? = null, var endDate: String? = null)

data class LabelValue(
    val label: String,
    var value: String,
)
