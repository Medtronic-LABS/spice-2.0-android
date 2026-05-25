package org.medtroniclabs.uhis.data.registration

data class UserDashboardRequest(
    val sortField: String? = null,
    val customDate: CustomDateModel? = null,
    val tenantId: Long,
    val userId: Long,
    val shasthyaShebikaId: Long? = null,
)

data class CustomDateModel(val startDate: String? = null, val endDate: String? = null)
