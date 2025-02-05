package com.medtroniclabs.opensource.data

data class NCDUserDashboardRequest(
    val sortField: String? = null,
    val customDate: CustomDateModel? = null,
    val userId: String? = null
)

data class CustomDateModel(val startDate: String? = null, val endDate: String? = null)

data class NCDUserDashboardResponse(
    val screened: Int? = null,
    val assessed: Int? = null,
    val registered: Int? = null,
    val referred: Int? = null,
    val dispensed: Int? = null,
    val investigated: Int? = null,
    val nutritionistLifestyleCount: Int? = null,
    val psychologicalNotesCount: Int? = null
)

data class ActivityModel(
    val key: String,
    val title: String,
    val fieldName: String
)