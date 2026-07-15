package org.medtroniclabs.uhis.data.registration

data class ScreeningDetail(
    val patientTrackId: Long,
    val screeningId: Long,
    val isAssessmentDataRequired: Boolean,
)
