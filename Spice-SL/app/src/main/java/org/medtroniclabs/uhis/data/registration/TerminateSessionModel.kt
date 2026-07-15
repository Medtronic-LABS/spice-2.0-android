package org.medtroniclabs.uhis.data.registration

data class TerminateSessionModel(
    val patientTrackId: Long? = null,
    val isSessionDropOut: Boolean? = null,
    val reason: String? = null,
    val otherReason: String? = null,
)
