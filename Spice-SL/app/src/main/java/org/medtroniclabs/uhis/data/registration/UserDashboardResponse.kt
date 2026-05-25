package org.medtroniclabs.uhis.data.registration

data class UserDashboardResponse(
    val screened: Int? = null,
    val assessed: Int? = null,
    val registered: Int? = null,
    val referred: Int? = null,
    val dispensed: Int? = null,
    val investigated: Int? = null,
    val nutritionistLifestyleCount: Int? = null,
    val psychologicalNotesCount: Int? = null,
)
