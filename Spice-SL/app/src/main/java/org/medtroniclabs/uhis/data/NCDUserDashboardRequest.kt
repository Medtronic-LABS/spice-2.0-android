package org.medtroniclabs.uhis.data

data class NCDUserDashboardRequest(
    val sortField: String? = null,
    val customDate: CustomDateModel? = null,
    val userId: String? = null,
    val filterBySs: List<Long>? = null,
    val filterBySubVillages: List<Long>? = null,
)

data class CustomDateModel(val startDate: String? = null, val endDate: String? = null)

data class NCDUserDashboardResponse(
    val screened: Int,
    val assessed: Int,
    val registered: Int,
    val referred: Int,
    val dispensed: Int,
    val investigated: Int,
    val nutritionistLifestyleCount: Int,
    val psychologicalNotesCount: Int,
    val familyPlanningCount: Int,
    val pregnantWomenRegistrationCount: Int,
    val pregnancyOutcomeCount: Int,
    val ancCount: Int,
    val pncCount: Int,
    val childVisitCount: Int,
    val tbAssessmentCount: Int,
    val tbContactTracingCount: Int,
    val eyeCareCount: Int,
    val cataractCount: Int,
    val householdRegisteredCount: Int,
    val pwIdentifiedFirst4MonthsWithAncCount: Int,
    val anc3PlusCount: Int,
    val highRiskPregnantWomenCount: Int,
    val totalNcdServicesCount: Int = 0,
    val ncdScreeningFirstServiceCount: Int = 0,
    val ncdFollowUpAssessmentCount: Int = 0,
    val ncdFollowUpReferralCount: Int = 0,
    val linkedToCareCount: Int = 0,
    val glassesSoldCustomStatusCount: Int = 0,
    val ncdServicesInCataractCampCount: Int = 0,
    val patientsReferredForOperationCount: Int = 0,
)

data class ActivityModel(
    val key: String,
    val title: String,
    val fieldName: String,
)
